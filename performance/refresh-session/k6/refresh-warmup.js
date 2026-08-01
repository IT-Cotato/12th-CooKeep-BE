import http from "k6/http";
import { check, fail } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const vus = Number(__ENV.VUS || 10);
const duration = __ENV.DURATION || "3m";
const userStart = Number(__ENV.USER_START || 1);
const benchmarkPassword = __ENV.BENCHMARK_USER_PASSWORD;

const windows = [
  new Counter("warmup_window_1"),
  new Counter("warmup_window_2"),
  new Counter("warmup_window_3"),
  new Counter("warmup_window_4"),
  new Counter("warmup_window_5"),
  new Counter("warmup_window_6"),
];

export const options = {
  scenarios: {
    warmup: {
      executor: "constant-vus",
      vus,
      duration,
      gracefulStop: "30s",
    },
  },
};

let refreshCookie;

function extractCookie(response) {
  const header = response.headers["Set-Cookie"] || "";
  const cookie = header.split(";")[0];
  return cookie.startsWith("refreshToken=") ? cookie : null;
}

function login() {
  if (!benchmarkPassword) {
    fail("BENCHMARK_USER_PASSWORD is required");
  }
  const userNumber = userStart + exec.vu.idInTest - 1;
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({
      email: `benchmark${userNumber}@example.com`,
      password: benchmarkPassword,
    }),
    { headers: { "Content-Type": "application/json" } },
  );
  refreshCookie = extractCookie(response);
  const success = check(response, {
    "warmup login status is 200": (res) => res.status === 200,
    "warmup refresh cookie is issued": () => Boolean(refreshCookie),
  });
  if (!success) {
    fail(`warmup login failed for user ${userNumber}`);
  }
}

export default function () {
  if (!refreshCookie) {
    login();
  }

  const response = http.post(`${baseUrl}/api/auth/refresh`, null, {
    headers: { Cookie: refreshCookie },
    tags: { name: "warmup POST /api/auth/refresh" },
  });
  const success = check(response, {
    "warmup refresh status is 200": (res) => res.status === 200,
  });
  if (!success) {
    fail(`warmup refresh failed: ${response.status}`);
  }

  const nextCookie = extractCookie(response);
  if (nextCookie) {
    refreshCookie = nextCookie;
  }

  const elapsedMs = Date.now() - exec.scenario.startTime;
  const windowIndex = Math.min(5, Math.floor(elapsedMs / 30000));
  windows[windowIndex].add(1);
}
