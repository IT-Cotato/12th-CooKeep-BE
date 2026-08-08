import http from "k6/http";
import { check, fail } from "k6";
import { Rate, Trend } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const benchmarkPassword = __ENV.BENCHMARK_USER_PASSWORD;
const refreshDuration = new Trend("refresh_duration", true);
const refreshFailures = new Rate("refresh_failures");

export const options = {
  noCookiesReset: true,
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  scenarios: {
    refreshStress: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 20 },
        { duration: "30s", target: 50 },
        { duration: "1m", target: 100 },
        { duration: "30s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    refresh_failures: ["rate<0.01"],
    refresh_duration: ["p(95)<2000"],
  },
};

let loggedIn = false;

function login() {
  if (!benchmarkPassword) {
    fail("BENCHMARK_USER_PASSWORD is required");
  }
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({
      email: `benchmark${__VU}@example.com`,
      password: benchmarkPassword,
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "POST /api/auth/login" },
    },
  );

  const success = check(response, {
    "login status is 200": (res) => res.status === 200,
    "refresh cookie is issued": (res) =>
      (res.headers["Set-Cookie"] || "").includes("refreshToken="),
  });
  if (!success) {
    fail(`login failed for VU ${__VU}: ${response.status} ${response.body}`);
  }
  loggedIn = true;
}

export default function () {
  if (!loggedIn) {
    login();
  }

  const response = http.post(`${baseUrl}/api/auth/refresh`, null, {
    tags: { name: "POST /api/auth/refresh" },
  });
  refreshDuration.add(response.timings.duration);

  const success = check(response, {
    "refresh status is 200": (res) => res.status === 200,
    "access token is returned": (res) => {
      try {
        return Boolean(res.json("data.accessToken"));
      } catch (_) {
        return false;
      }
    },
  });
  refreshFailures.add(!success);
}
