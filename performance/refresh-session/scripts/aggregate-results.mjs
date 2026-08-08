import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..", "..", "..");
const resultsDirectory = path.join(
  projectRoot,
  "performance",
  "refresh-session",
  "results",
);
const metricsDirectory = path.join(resultsDirectory, "metrics");

function round(value, digits = 2) {
  return Number(value.toFixed(digits));
}

function quantile(values, probability) {
  const sorted = [...values].sort((left, right) => left - right);
  const index = (sorted.length - 1) * probability;
  const lower = Math.floor(index);
  const upper = Math.ceil(index);
  if (lower === upper) {
    return sorted[lower];
  }
  return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
}

function describe(values) {
  return {
    median: round(quantile(values, 0.5)),
    q1: round(quantile(values, 0.25)),
    q3: round(quantile(values, 0.75)),
    iqr: round(quantile(values, 0.75) - quantile(values, 0.25)),
    min: round(Math.min(...values)),
    max: round(Math.max(...values)),
  };
}

function parseTsv(content) {
  return content
    .trim()
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("\t"));
}

function parseRedisCommandStats(content) {
  const result = {};
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(/^cmdstat_([^:]+):calls=(\d+)/);
    if (match) {
      result[match[1]] = Number(match[2]);
    }
  }
  return result;
}

function parseMemoryMiB(value) {
  const match = value.match(/^([\d.]+)(KiB|MiB|GiB)/);
  if (!match) {
    return null;
  }
  const amount = Number(match[1]);
  return match[2] === "KiB"
    ? amount / 1024
    : match[2] === "GiB"
      ? amount * 1024
      : amount;
}

async function loadRun(stage, roundNumber) {
  const result = JSON.parse(
    await readFile(
      path.join(resultsDirectory, `${stage}-run${roundNumber}.json`),
      "utf8",
    ),
  );
  if (!result.validation.valid) {
    throw new Error(`${stage}-run${roundNumber} is not valid`);
  }

  const prefix = `${stage}-run${roundNumber}-attempt${result.metadata.attempt}`;
  const [mysqlStatusText, mysqlStatementsText, redisText, dockerText] =
    await Promise.all([
      readFile(path.join(metricsDirectory, `${prefix}-mysql-status.tsv`), "utf8"),
      readFile(
        path.join(metricsDirectory, `${prefix}-mysql-statements.tsv`),
        "utf8",
      ),
      readFile(
        path.join(metricsDirectory, `${prefix}-redis-commandstats.txt`),
        "utf8",
      ),
      readFile(
        path.join(metricsDirectory, `${prefix}-docker-stats.json`),
        "utf8",
      ).catch(() => null),
    ]);

  const mysqlStatus = Object.fromEntries(
    parseTsv(mysqlStatusText).map(([name, value]) => [name, Number(value)]),
  );
  const mysqlStatements = parseTsv(mysqlStatementsText).map(
    ([count, totalLatencyMs, sql]) => ({
      count: Number(count),
      totalLatencyMs: Number(totalLatencyMs),
      sql,
    }),
  );
  const userSessionSelects = mysqlStatements
    .filter(
      ({ sql }) => sql.includes("SELECT") && sql.includes("user_sessions"),
    )
    .reduce((sum, statement) => sum + statement.count, 0);
  const userSelects = mysqlStatements
    .filter(
      ({ sql }) =>
        sql.includes("SELECT") &&
        sql.includes("FROM `users`") &&
        !sql.includes("user_sessions"),
    )
    .reduce((sum, statement) => sum + statement.count, 0);
  const userUpdates = mysqlStatements
    .filter(({ sql }) => sql.includes("UPDATE `users`"))
    .reduce((sum, statement) => sum + statement.count, 0);
  const statementExecutions = mysqlStatements.reduce(
    (sum, statement) => sum + statement.count,
    0,
  );

  const docker = {};
  if (dockerText) {
    const dockerSamples = JSON.parse(dockerText);
    for (const containerName of [
      "cookeep-benchmark-app",
      "cookeep-benchmark-mysql",
      "cookeep-benchmark-redis",
    ]) {
      const samples = dockerSamples.filter(({ name }) => name === containerName);
      docker[containerName] = {
        sampleCount: samples.length,
        cpuPercent: samples.map(({ cpu }) => Number(cpu.replace("%", ""))),
        memoryMiB: samples.map(({ memory }) =>
          parseMemoryMiB(memory.split(" / ")[0]),
        ),
      };
    }
  }

  const duration = result.summary.metrics.refresh_duration;
  return {
    stage: stage.toUpperCase(),
    round: roundNumber,
    attempt: result.metadata.attempt,
    order: result.metadata.roundOrder,
    commit: result.metadata.commit,
    completed: result.calculated.refreshCompleted,
    rps: result.calculated.directRps,
    k6Rate: result.calculated.k6CounterRate,
    errorRate: result.summary.metrics.refresh_failures.value,
    latencyMs: {
      avg: duration.avg,
      p50: duration.med,
      p90: duration["p(90)"],
      p95: duration["p(95)"],
      p99: duration["p(99)"],
      max: duration.max,
    },
    mysql: {
      statementExecutions,
      userSessionSelects,
      userSelects,
      userUpdates,
      connections: mysqlStatus.Connections,
      maxUsedConnections: mysqlStatus.Max_used_connections,
    },
    redis: parseRedisCommandStats(redisText),
    docker,
  };
}

const runs = [];
for (const stage of ["a", "b"]) {
  for (let roundNumber = 1; roundNumber <= 5; roundNumber += 1) {
    runs.push(await loadRun(stage, roundNumber));
  }
}

const metrics = {
  rps: (run) => run.rps,
  avgMs: (run) => run.latencyMs.avg,
  p50Ms: (run) => run.latencyMs.p50,
  p90Ms: (run) => run.latencyMs.p90,
  p95Ms: (run) => run.latencyMs.p95,
  p99Ms: (run) => run.latencyMs.p99,
  maxMs: (run) => run.latencyMs.max,
  mysqlStatements: (run) => run.mysql.statementExecutions,
  userSessionSelects: (run) => run.mysql.userSessionSelects,
  mysqlConnections: (run) => run.mysql.connections,
  maxUsedConnections: (run) => run.mysql.maxUsedConnections,
  redisEvalSha: (run) => run.redis.evalsha ?? 0,
  redisGet: (run) => run.redis.get ?? 0,
  redisSet: (run) => run.redis.set ?? 0,
};

const stageSummary = {};
for (const stage of ["A", "B"]) {
  const stageRuns = runs.filter((run) => run.stage === stage);
  stageSummary[stage] = Object.fromEntries(
    Object.entries(metrics).map(([name, selector]) => [
      name,
      describe(stageRuns.map(selector)),
    ]),
  );
}

const paired = [];
for (let roundNumber = 1; roundNumber <= 5; roundNumber += 1) {
  const a = runs.find(
    (run) => run.stage === "A" && run.round === roundNumber,
  );
  const b = runs.find(
    (run) => run.stage === "B" && run.round === roundNumber,
  );
  paired.push({
    round: roundNumber,
    order: a.order,
    rpsChangePercent: round(((b.rps - a.rps) / a.rps) * 100),
    avgLatencyReductionPercent: round(
      ((a.latencyMs.avg - b.latencyMs.avg) / a.latencyMs.avg) * 100,
    ),
    p95ReductionPercent: round(
      ((a.latencyMs.p95 - b.latencyMs.p95) / a.latencyMs.p95) * 100,
    ),
    p99ReductionPercent: round(
      ((a.latencyMs.p99 - b.latencyMs.p99) / a.latencyMs.p99) * 100,
    ),
  });
}

const overall = {
  rpsChangePercent: round(
    ((stageSummary.B.rps.median - stageSummary.A.rps.median) /
      stageSummary.A.rps.median) *
      100,
  ),
  avgLatencyReductionPercent: round(
    ((stageSummary.A.avgMs.median - stageSummary.B.avgMs.median) /
      stageSummary.A.avgMs.median) *
      100,
  ),
  p95ReductionPercent: round(
    ((stageSummary.A.p95Ms.median - stageSummary.B.p95Ms.median) /
      stageSummary.A.p95Ms.median) *
      100,
  ),
  p99ReductionPercent: round(
    ((stageSummary.A.p99Ms.median - stageSummary.B.p99Ms.median) /
      stageSummary.A.p99Ms.median) *
      100,
  ),
};

const aggregate = {
  generatedAt: new Date().toISOString(),
  methodology: {
    repeats: 5,
    warmupSeconds: 180,
    measurementSeconds: 180,
    vus: 50,
    workload: "closed",
    order: ["A1-B1", "B2-A2", "A3-B3", "B4-A4", "A5-B5"],
    representativeStatistic: "median",
    quartileMethod: "linear interpolation (R type 7)",
  },
  runs,
  stageSummary,
  paired,
  pairedSummary: {
    rpsChangePercent: describe(paired.map((item) => item.rpsChangePercent)),
    avgLatencyReductionPercent: describe(
      paired.map((item) => item.avgLatencyReductionPercent),
    ),
    p95ReductionPercent: describe(
      paired.map((item) => item.p95ReductionPercent),
    ),
    p99ReductionPercent: describe(
      paired.map((item) => item.p99ReductionPercent),
    ),
  },
  overall,
  limitations: [
    "Docker CPU/memory sampler produced only two boundary samples per run because the synchronous k6 child process blocked the Node.js sampling timer.",
    "Long sequential local runs showed host-performance drift; alternating order, medians, IQRs, and paired changes are reported.",
    "The closed workload couples throughput to response latency.",
  ],
};

await writeFile(
  path.join(resultsDirectory, "aggregate.json"),
  `${JSON.stringify(aggregate, null, 2)}\n`,
);

const csvHeader = [
  "stage",
  "round",
  "attempt",
  "order",
  "completed",
  "rps",
  "avg_ms",
  "p50_ms",
  "p90_ms",
  "p95_ms",
  "p99_ms",
  "max_ms",
  "error_rate",
  "mysql_statements",
  "user_session_selects",
  "mysql_connections",
  "max_used_connections",
  "redis_evalsha",
  "redis_get",
  "redis_set",
];
const csvRows = runs.map((run) =>
  [
    run.stage,
    run.round,
    run.attempt,
    run.order,
    run.completed,
    round(run.rps, 4),
    round(run.latencyMs.avg, 4),
    round(run.latencyMs.p50, 4),
    round(run.latencyMs.p90, 4),
    round(run.latencyMs.p95, 4),
    round(run.latencyMs.p99, 4),
    round(run.latencyMs.max, 4),
    run.errorRate,
    run.mysql.statementExecutions,
    run.mysql.userSessionSelects,
    run.mysql.connections,
    run.mysql.maxUsedConnections,
    run.redis.evalsha ?? 0,
    run.redis.get ?? 0,
    run.redis.set ?? 0,
  ].join(","),
);
await writeFile(
  path.join(resultsDirectory, "runs.csv"),
  `${csvHeader.join(",")}\n${csvRows.join("\n")}\n`,
);

console.log(JSON.stringify({ stageSummary, paired, overall }, null, 2));
