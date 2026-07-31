import { execFileSync } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..", "..", "..");
const outputPath = path.join(
  projectRoot,
  "performance",
  "refresh-session",
  "results",
  "security-verification.json",
);
const baseUrl = process.env.BASE_URL ?? "http://localhost:8080";
const redisContainer =
  process.env.REDIS_CONTAINER ?? "cookeep-benchmark-redis";
const benchmarkPassword = process.env.BENCHMARK_USER_PASSWORD;

if (!benchmarkPassword) {
  throw new Error("BENCHMARK_USER_PASSWORD is required");
}
const checks = [];

function docker(...args) {
  return execFileSync("docker", args, {
    cwd: projectRoot,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();
}

function record(name, passed, details) {
  checks.push({ name, passed, details });
  console.log(`${passed ? "PASS" : "FAIL"} ${name}: ${details}`);
}

function assertCheck(name, condition, details) {
  record(name, Boolean(condition), details);
}

function extractRefreshCookie(response) {
  const setCookie = response.headers.get("set-cookie") ?? "";
  const match = setCookie.match(/(?:^|,\s*)(refreshToken=[^;,\s]+)/);
  return match?.[1] ?? null;
}

function hasExpiredRefreshCookie(response) {
  const setCookie = response.headers.get("set-cookie") ?? "";
  return (
    setCookie.includes("refreshToken=") &&
    /(?:Max-Age=0|Expires=Thu, 01 Jan 1970)/i.test(setCookie)
  );
}

async function responseBody(response) {
  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function login(userNumber) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: `benchmark${userNumber}@example.com`,
      password: benchmarkPassword,
    }),
  });
  return {
    response,
    body: await responseBody(response),
    cookie: extractRefreshCookie(response),
  };
}

async function refresh(cookie) {
  const response = await fetch(`${baseUrl}/api/auth/refresh`, {
    method: "POST",
    headers: cookie ? { Cookie: cookie } : {},
  });
  return {
    response,
    body: await responseBody(response),
    cookie: extractRefreshCookie(response),
  };
}

function flushRedis() {
  docker("exec", redisContainer, "redis-cli", "FLUSHDB");
}

function findSessionKey() {
  const keys = docker(
    "exec",
    redisContainer,
    "redis-cli",
    "--raw",
    "--scan",
    "--pattern",
    "auth:session:*",
  )
    .split(/\r?\n/)
    .filter(Boolean);
  if (keys.length !== 1) {
    throw new Error(`expected one auth session key, found ${keys.length}`);
  }
  return keys[0];
}

function redis(...args) {
  return docker("exec", redisContainer, "redis-cli", "--raw", ...args);
}

async function waitForRedis() {
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    try {
      if (redis("PING") === "PONG") {
        return;
      }
    } catch {
      // Redis container can still be starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error("Redis did not become ready within 30 seconds");
}

async function verifyStorageAndAbsoluteTtl() {
  flushRedis();
  const issued = await login(90);
  assertCheck(
    "login creates Redis session",
    issued.response.status === 200 && issued.cookie,
    `status=${issued.response.status}, cookie=${Boolean(issued.cookie)}`,
  );
  const key = findSessionKey();
  const rawToken = issued.cookie.split("=")[1];
  const storedValue = redis("GET", key);
  assertCheck(
    "Redis stores sessionId and SHA-256 digest, not raw token",
    /^[A-Za-z0-9_-]{22,}:[a-f0-9]{64}$/.test(storedValue) &&
      !storedValue.includes(rawToken),
    `key=${key}, valuePattern=${/^[A-Za-z0-9_-]{22,}:[a-f0-9]{64}$/.test(storedValue)}, rawStored=${storedValue.includes(rawToken)}`,
  );

  const ttlBefore = Number(redis("PTTL", key));
  await new Promise((resolve) => setTimeout(resolve, 1_200));
  const rotated = await refresh(issued.cookie);
  const ttlAfter = Number(redis("PTTL", key));
  assertCheck(
    "rotation keeps the original absolute expiration",
    rotated.response.status === 200 &&
      ttlAfter > 0 &&
      ttlAfter < ttlBefore - 700,
    `status=${rotated.response.status}, ttlBeforeMs=${ttlBefore}, ttlAfterMs=${ttlAfter}`,
  );
}

async function verifyReuseRevocation() {
  flushRedis();
  const issued = await login(91);
  const first = await refresh(issued.cookie);
  const reused = await refresh(issued.cookie);
  const winnerAfterReuse = await refresh(first.cookie);
  assertCheck(
    "rotated token reuse is detected and cookie is expired",
    first.response.status === 200 &&
      reused.response.status === 401 &&
      reused.body?.code === "AUTH-015" &&
      hasExpiredRefreshCookie(reused.response),
    `first=${first.response.status}, reuse=${reused.response.status}/${reused.body?.code}, expiredCookie=${hasExpiredRefreshCookie(reused.response)}`,
  );
  assertCheck(
    "reuse detection revokes the winner refresh token too",
    winnerAfterReuse.response.status === 401,
    `winnerAfterReuse=${winnerAfterReuse.response.status}/${winnerAfterReuse.body?.code}`,
  );
}

async function verifyConcurrentReuse() {
  flushRedis();
  const issued = await login(92);
  const responses = await Promise.all([
    refresh(issued.cookie),
    refresh(issued.cookie),
  ]);
  const statuses = responses.map(({ response }) => response.status).sort();
  const winner = responses.find(({ response }) => response.status === 200);
  const rejected = responses.find(({ response }) => response.status === 401);
  const winnerAfterDetection = await refresh(winner?.cookie);
  assertCheck(
    "concurrent refresh allows one rotation and rejects one reuse",
    statuses.join(",") === "200,401" &&
      rejected?.body?.code === "AUTH-015",
    `statuses=${statuses.join(",")}, rejectedCode=${rejected?.body?.code}`,
  );
  assertCheck(
    "concurrent reuse revokes the rotated refresh token",
    winnerAfterDetection.response.status === 401,
    `winnerAfterDetection=${winnerAfterDetection.response.status}/${winnerAfterDetection.body?.code}`,
  );
}

async function verifyDifferentSessionIsolation() {
  flushRedis();
  const oldLogin = await login(93);
  const currentLogin = await login(93);
  const oldRefresh = await refresh(oldLogin.cookie);
  const currentRefresh = await refresh(currentLogin.cookie);
  assertCheck(
    "an older login session cannot revoke the current login session",
    oldRefresh.response.status === 401 &&
      oldRefresh.body?.code !== "AUTH-015" &&
      currentRefresh.response.status === 200,
    `old=${oldRefresh.response.status}/${oldRefresh.body?.code}, current=${currentRefresh.response.status}`,
  );
}

async function verifyExpiry() {
  flushRedis();
  const issued = await login(94);
  const key = findSessionKey();
  redis("PEXPIRE", key, "500");
  await new Promise((resolve) => setTimeout(resolve, 800));
  const expired = await refresh(issued.cookie);
  assertCheck(
    "expired Redis session rejects refresh and expires the cookie",
    expired.response.status === 401 &&
      hasExpiredRefreshCookie(expired.response),
    `status=${expired.response.status}/${expired.body?.code}, expiredCookie=${hasExpiredRefreshCookie(expired.response)}`,
  );
}

async function verifyRedisOutage() {
  flushRedis();
  const issued = await login(95);
  docker("stop", redisContainer);
  try {
    const refreshDuringOutage = await refresh(issued.cookie);
    const loginDuringOutage = await login(96);
    assertCheck(
      "Redis outage fails refresh closed",
      refreshDuringOutage.response.status === 503 &&
        refreshDuringOutage.body?.code === "AUTH-016",
      `status=${refreshDuringOutage.response.status}/${refreshDuringOutage.body?.code}`,
    );
    assertCheck(
      "Redis outage fails session issuance closed",
      loginDuringOutage.response.status === 503 &&
        loginDuringOutage.body?.code === "AUTH-016" &&
        !loginDuringOutage.cookie,
      `status=${loginDuringOutage.response.status}/${loginDuringOutage.body?.code}, cookie=${Boolean(loginDuringOutage.cookie)}`,
    );
  } finally {
    docker("start", redisContainer);
    await waitForRedis();
  }
}

let fatalError = null;
try {
  await verifyStorageAndAbsoluteTtl();
  await verifyReuseRevocation();
  await verifyConcurrentReuse();
  await verifyDifferentSessionIsolation();
  await verifyExpiry();
  await verifyRedisOutage();
} catch (error) {
  fatalError = error instanceof Error ? error.stack : String(error);
  console.error(fatalError);
  try {
    docker("start", redisContainer);
    await waitForRedis();
  } catch {
    // Preserve the original failure.
  }
}

const result = {
  generatedAt: new Date().toISOString(),
  target: {
    stage: "B",
    commit: "363fa0a",
    baseUrl,
    redisContainer,
  },
  checks,
  fatalError,
  passed:
    fatalError === null &&
    checks.length === 11 &&
    checks.every(({ passed }) => passed),
};

await mkdir(path.dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${JSON.stringify(result, null, 2)}\n`);
console.log(`result=${outputPath}`);
process.exitCode = result.passed ? 0 : 1;
