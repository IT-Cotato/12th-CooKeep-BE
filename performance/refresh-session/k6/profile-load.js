import http from "k6/http";
import { check, fail } from "k6";
import { Rate, Trend } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const vus = Number(__ENV.VUS || 50);
const duration = __ENV.DURATION || "3m";
const benchmarkPassword = __ENV.BENCHMARK_USER_PASSWORD;

const profileDuration = new Trend("profile_duration", true);
const profileFailures = new Rate("profile_failures");

export const options = {
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  scenarios: {
    profile: {
      executor: "constant-vus",
      vus,
      duration,
      gracefulStop: "10s",
    },
  },
  thresholds: {
    profile_failures: ["rate<0.01"],
    profile_duration: ["p(95)<2000"],
  },
};

let accessToken;

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
    "access token is issued": (res) => Boolean(res.json("data.accessToken")),
  });
  if (!success) {
    fail(`login failed for VU ${__VU}: ${response.status} ${response.body}`);
  }
  accessToken = response.json("data.accessToken");
}

export default function () {
  if (!accessToken) {
    login();
  }

  const response = http.get(`${baseUrl}/api/users/me/profile`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    tags: { name: "GET /api/users/me/profile" },
  });
  profileDuration.add(response.timings.duration);

  const success = check(response, {
    "profile status is 200": (res) => res.status === 200,
  });
  profileFailures.add(!success);
}
