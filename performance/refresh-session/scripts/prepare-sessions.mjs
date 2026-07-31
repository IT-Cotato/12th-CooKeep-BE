import { chmod, mkdir, writeFile } from "node:fs/promises";

const baseUrl = process.env.BASE_URL ?? "http://benchmark-app:8080";
const userStart = Number(process.env.USER_START ?? 11);
const userCount = Number(process.env.USER_COUNT ?? 50);
const outputPath = process.env.OUTPUT_PATH ?? "/runtime/measurement-sessions.json";
const benchmarkPassword = process.env.BENCHMARK_USER_PASSWORD;

if (!benchmarkPassword) {
  throw new Error("BENCHMARK_USER_PASSWORD is required");
}

function extractRefreshCookie(response) {
  const values =
    typeof response.headers.getSetCookie === "function"
      ? response.headers.getSetCookie()
      : [response.headers.get("set-cookie") ?? ""];
  const header = values.find((value) => value.includes("refreshToken=")) ?? "";
  return header.split(";")[0];
}

async function login(userNumber) {
  const email = `benchmark${userNumber}@example.com`;
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: benchmarkPassword }),
  });
  const body = await response.text();
  const cookie = extractRefreshCookie(response);

  if (response.status !== 200 || !cookie.startsWith("refreshToken=")) {
    throw new Error(
      `login failed: user=${email} status=${response.status} cookie=${Boolean(cookie)} body=${body}`,
    );
  }
  return { userNumber, email, cookie };
}

const userNumbers = Array.from({ length: userCount }, (_, index) => userStart + index);
const sessions = await Promise.all(userNumbers.map(login));

if (new Set(sessions.map(({ email }) => email)).size !== userCount) {
  throw new Error("prepared users are not unique");
}
if (new Set(sessions.map(({ cookie }) => cookie)).size !== userCount) {
  throw new Error("prepared refresh cookies are not unique");
}

await mkdir(outputPath.slice(0, outputPath.lastIndexOf("/")), { recursive: true });
await writeFile(
  outputPath,
  JSON.stringify(
    {
      preparedAt: new Date().toISOString(),
      userStart,
      userCount,
      sessions,
    },
    null,
    2,
  ),
  { mode: 0o644 },
);
await chmod(outputPath, 0o644);

console.log(`prepared ${sessions.length} unique refresh sessions`);
