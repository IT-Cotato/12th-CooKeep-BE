import { execFile, spawnSync } from "node:child_process";
import { promisify } from "node:util";
import {
  appendFile,
  mkdir,
  readFile,
  rm,
  writeFile,
} from "node:fs/promises";
import path from "node:path";

const execFileAsync = promisify(execFile);
const composeFile = "docker-compose.benchmark.yml";
const resultRoot = path.resolve("performance/refresh-session/results");

const stage = required("STAGE").toLowerCase();
const image = required("BENCHMARK_APP_IMAGE");
const commit = required("BENCHMARK_COMMIT");
const round = Number(required("ROUND"));
const order = required("ROUND_ORDER");
required("BENCHMARK_DB_PASSWORD");
required("BENCHMARK_USER_PASSWORD");
const warmupSeconds = Number(process.env.WARMUP_SECONDS ?? 180);
const measurementSeconds = Number(process.env.MEASUREMENT_SECONDS ?? 180);
const attempt = Number(process.env.ATTEMPT ?? 1);
const runName = `${stage}-run${round}`;
const attemptName = `${runName}-attempt${attempt}`;
const runEnv = { ...process.env, BENCHMARK_APP_IMAGE: image };

if (!["a", "b"].includes(stage)) {
  throw new Error(`STAGE must be a or b, received ${stage}`);
}
if (!Number.isFinite(round) || round < 1 || round > 5) {
  throw new Error(`ROUND must be 1..5, received ${round}`);
}

await mkdir(path.join(resultRoot, "raw"), { recursive: true });
await mkdir(path.join(resultRoot, "metrics"), { recursive: true });
await mkdir(path.join(resultRoot, "logs"), { recursive: true });

const executionStartedAt = new Date();
await log(`START ${attemptName} image=${image} order=${order}`);

compose(["rm", "-sf", "benchmark-app"], { allowFailure: true });
compose(["up", "-d", "--no-build", "benchmark-app"]);
await waitForHealth();
const restartCountBefore = Number(
  docker(["inspect", "-f", "{{.RestartCount}}", "cookeep-benchmark-app"]).trim(),
);

compose(["run", "--rm", "benchmark-seed"]);
resetSessionState();

compose([
  "--profile",
  "load",
  "run",
  "--rm",
  "--no-deps",
  "-e",
  "VUS=10",
  "-e",
  `DURATION=${warmupSeconds}s`,
  "-e",
  "USER_START=1",
  "k6",
  "run",
  "--quiet",
  "--summary-export",
  `/results/raw/${attemptName}-warmup.json`,
  "/scripts/refresh-warmup.js",
]);

resetSessionState();
compose(["--profile", "load", "run", "--rm", "benchmark-prepare"]);
resetStatistics();
await delay(15000);

const samples = [];
let sampling = false;
const sample = async () => {
  if (sampling) return;
  sampling = true;
  try {
    const { stdout } = await execFileAsync(
      "docker",
      [
        "stats",
        "--no-stream",
        "--format",
        "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}",
        "cookeep-benchmark-app",
        "cookeep-benchmark-mysql",
        "cookeep-benchmark-redis",
      ],
      { encoding: "utf8" },
    );
    const at = new Date().toISOString();
    for (const line of stdout.trim().split(/\r?\n/)) {
      if (!line) continue;
      const [name, cpu, memory] = line.split("|");
      samples.push({ at, name, cpu, memory });
    }
  } finally {
    sampling = false;
  }
};

await sample();
const sampleTimer = setInterval(sample, 5000);
const measurementCommandStartedAt = new Date();
let k6Error;
try {
  await composeAsync([
    "--profile",
    "load",
    "run",
    "--rm",
    "--no-deps",
    "-e",
    "VUS=50",
    "-e",
    `DURATION=${measurementSeconds}s`,
    "-e",
    "SESSIONS_PATH=/runtime/measurement-sessions.json",
    "k6",
    "run",
    "--quiet",
    "--summary-export",
    `/results/raw/${attemptName}-k6.json`,
    "/scripts/refresh-load.js",
  ]);
} catch (error) {
  k6Error = error;
} finally {
  clearInterval(sampleTimer);
  while (sampling) {
    await delay(50);
  }
  await sample();
}
const measurementCommandEndedAt = new Date();

const restartCountAfter = Number(
  docker(["inspect", "-f", "{{.RestartCount}}", "cookeep-benchmark-app"]).trim(),
);
const mysqlStatements = mysql(`
SELECT COUNT_STAR,
       ROUND(SUM_TIMER_WAIT / 1000000000000, 6),
       LEFT(DIGEST_TEXT, 300)
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'cookeep'
ORDER BY COUNT_STAR DESC;
`);
const mysqlStatus = mysql(`
SHOW GLOBAL STATUS
WHERE Variable_name IN (
  'Connections',
  'Max_used_connections',
  'Threads_connected',
  'Threads_running',
  'Aborted_connects'
);
`);
const redisCommandstats = docker([
  "exec",
  "cookeep-benchmark-redis",
  "redis-cli",
  "INFO",
  "commandstats",
]);
const redisCpu = docker([
  "exec",
  "cookeep-benchmark-redis",
  "redis-cli",
  "INFO",
  "cpu",
]);
const redisMemory = docker([
  "exec",
  "cookeep-benchmark-redis",
  "redis-cli",
  "INFO",
  "memory",
]);
const appLogs = docker([
  "logs",
  "--since",
  executionStartedAt.toISOString(),
  "cookeep-benchmark-app",
]);

await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-docker-stats.json`),
  JSON.stringify(samples, null, 2),
);
await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-mysql-statements.tsv`),
  mysqlStatements,
);
await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-mysql-status.tsv`),
  mysqlStatus,
);
await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-redis-commandstats.txt`),
  redisCommandstats,
);
await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-redis-cpu.txt`),
  redisCpu,
);
await writeFile(
  path.join(resultRoot, "metrics", `${attemptName}-redis-memory.txt`),
  redisMemory,
);
await writeFile(
  path.join(resultRoot, "logs", `${attemptName}-app.log`),
  appLogs,
);

const rawPath = path.join(resultRoot, "raw", `${attemptName}-k6.json`);
let rawSummary;
try {
  rawSummary = JSON.parse(await readFile(rawPath, "utf8"));
} catch (error) {
  k6Error ??= error;
}

const completed = metricNumber(rawSummary, "refresh_completed", "count");
const k6CounterRate = metricNumber(rawSummary, "refresh_completed", "rate");
const observedMeasurementSeconds =
  completed !== null && k6CounterRate
    ? completed / k6CounterRate
    : null;
const validation = validate(
  rawSummary,
  restartCountBefore,
  restartCountAfter,
  measurementSeconds,
  observedMeasurementSeconds,
);
if (k6Error) {
  validation.reasons.push(`k6 execution failed: ${k6Error.message}`);
}
validation.valid = validation.reasons.length === 0;

const output = {
  metadata: {
    stage,
    commit,
    image,
    round,
    roundOrder: order,
    attempt,
    executionStartedAt: executionStartedAt.toISOString(),
    measurementCommandStartedAt: measurementCommandStartedAt.toISOString(),
    measurementCommandEndedAt: measurementCommandEndedAt.toISOString(),
    configuredMeasurementSeconds: measurementSeconds,
    warmupSeconds,
    seedUsers: 100,
    warmupUsers: { start: 1, count: 10 },
    measurementUsers: { start: 11, count: 50 },
    reserveUsers: { start: 61, count: 40 },
    workload: "closed",
  },
  calculated: {
    refreshCompleted: completed,
    directRps: completed === null ? null : completed / measurementSeconds,
    k6CounterRate,
    observedMeasurementSeconds,
    commandWallSeconds:
      (measurementCommandEndedAt - measurementCommandStartedAt) / 1000,
  },
  validation,
  summary: rawSummary,
};

const outputName = validation.valid
  ? `${runName}.json`
  : `excluded-${attemptName}.json`;
await writeFile(
  path.join(resultRoot, outputName),
  JSON.stringify(output, null, 2),
);
await log(
  `${validation.valid ? "VALID" : "EXCLUDED"} ${attemptName} output=${outputName} reasons=${validation.reasons.join("; ")}`,
);

await removeRuntimeSessions();

if (!validation.valid) {
  process.exitCode = 2;
}

function required(name) {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is required`);
  return value;
}

function run(file, args, options = {}) {
  const result = spawnSync(file, args, {
    cwd: process.cwd(),
    env: runEnv,
    encoding: "utf8",
    stdio: options.capture ? "pipe" : "inherit",
  });
  if (result.error) throw result.error;
  if (result.status !== 0 && !options.allowFailure) {
    throw new Error(
      `${file} ${args.join(" ")} failed with ${result.status}: ${result.stderr ?? ""}`,
    );
  }
  return options.capture ? result.stdout ?? "" : "";
}

function docker(args, options = {}) {
  return run("docker", args, { capture: true, ...options });
}

async function runAsync(file, args, options = {}) {
  try {
    const { stdout } = await execFileAsync(file, args, {
      cwd: process.cwd(),
      env: runEnv,
      encoding: "utf8",
      maxBuffer: 10 * 1024 * 1024,
    });
    if (!options.capture && stdout) {
      process.stdout.write(stdout);
    }
    return options.capture ? stdout ?? "" : "";
  } catch (error) {
    if (options.allowFailure) {
      return options.capture ? error.stdout ?? "" : "";
    }
    throw new Error(
      `${file} ${args.join(" ")} failed with ${error.code ?? "unknown"}: ${error.stderr ?? ""}`,
      { cause: error },
    );
  }
}

function compose(args, options = {}) {
  return run(
    "docker",
    ["compose", "-f", composeFile, ...args],
    options,
  );
}

function composeAsync(args, options = {}) {
  return runAsync(
    "docker",
    ["compose", "-f", composeFile, ...args],
    options,
  );
}

function mysql(sql) {
  return docker([
    "exec",
    "cookeep-benchmark-mysql",
    "mysql",

    "-uroot",
    "-N",
    "-e",
    sql,
    "cookeep",
  ]);
}

function resetSessionState() {
  mysql("DELETE FROM user_sessions;");
  docker(["exec", "cookeep-benchmark-redis", "redis-cli", "FLUSHDB"]);
}

function resetStatistics() {
  mysql(
    "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest; FLUSH STATUS;",
  );
  docker([
    "exec",
    "cookeep-benchmark-redis",
    "redis-cli",
    "CONFIG",
    "RESETSTAT",
  ]);
}

async function waitForHealth() {
  const deadline = Date.now() + 180000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch("http://localhost:8080/swagger-ui/index.html");
      if (response.status === 200) return;
    } catch {
      // Application is still starting.
    }
    await delay(3000);
  }
  throw new Error("benchmark application did not become healthy within 180 seconds");
}

function metricNumber(summary, metric, property) {
  const value = summary?.metrics?.[metric];
  if (!value) return null;
  return value[property] ?? value.values?.[property] ?? null;
}

function validate(summary, before, after, configuredSeconds, observedSeconds) {
  const reasons = [];
  const started = metricNumber(summary, "refresh_started", "count");
  const completed = metricNumber(summary, "refresh_completed", "count");
  const prepared = metricNumber(summary, "prepared_vus", "count");
  const httpRequests = metricNumber(summary, "http_reqs", "count");
  if (!summary) reasons.push("k6 summary is missing");
  if (prepared !== 50) reasons.push(`prepared_vus=${prepared}, expected 50`);
  if (started !== completed) {
    reasons.push(`refresh_started=${started} differs from completed=${completed}`);
  }
  if (httpRequests !== completed) {
    reasons.push(`http_reqs=${httpRequests} differs from completed=${completed}`);
  }
  if (before !== after) {
    reasons.push(`application restart count changed ${before} -> ${after}`);
  }
  if (observedSeconds === null) {
    reasons.push("observed measurement duration is unavailable");
  } else if (
    Math.abs(observedSeconds - configuredSeconds) / configuredSeconds > 0.01
  ) {
    reasons.push(
      `observed duration=${observedSeconds.toFixed(3)}s differs from configured=${configuredSeconds}s by more than 1%`,
    );
  }
  return { valid: false, reasons, restartCountBefore: before, restartCountAfter: after };
}

async function removeRuntimeSessions() {
  run(
    "docker",
    [
      "run",
      "--rm",
      "-v",
      "cookeep-benchmark-runtime:/runtime",
      "node:20-alpine",
      "node",
      "-e",
      "require('fs').rmSync('/runtime/measurement-sessions.json',{force:true})",
    ],
    { allowFailure: true },
  );
}

async function log(message) {
  const line = `${new Date().toISOString()} ${message}\n`;
  await appendFile(path.join(resultRoot, "execution-log.txt"), line);
  process.stdout.write(line);
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
