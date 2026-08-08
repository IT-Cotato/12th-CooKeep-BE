import http from "k6/http";
import { check, fail } from "k6";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const benchmarkPassword = __ENV.BENCHMARK_USER_PASSWORD;

export const options = {
  scenarios: {
    concurrentRefresh: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      maxDuration: "30s",
    },
  },
  thresholds: {
    checks: ["rate==1"],
  },
};

export function setup() {
  if (!benchmarkPassword) {
    fail("BENCHMARK_USER_PASSWORD is required");
  }
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({
      email: "benchmark1@example.com",
      password: benchmarkPassword,
    }),
    { headers: { "Content-Type": "application/json" } },
  );
  const setCookie = response.headers["Set-Cookie"] || "";
  const refreshCookie = setCookie.split(";")[0];
  if (response.status !== 200 || !refreshCookie.startsWith("refreshToken=")) {
    fail(`login failed: ${response.status} ${response.body}`);
  }
  return { refreshCookie };
}

export default function (data) {
  const requests = [1, 2].map(() => ({
    method: "POST",
    url: `${baseUrl}/api/auth/refresh`,
    body: null,
    params: {
      headers: { Cookie: data.refreshCookie },
      tags: { name: "concurrent POST /api/auth/refresh" },
    },
  }));
  const responses = http.batch(requests);
  const statuses = responses.map((response) => response.status).sort();

  check(statuses, {
    "exactly one rotation succeeds and one reuse is rejected": (values) =>
      values.length === 2 && values[0] === 200 && values[1] === 401,
  });

  const successfulResponse = responses.find((response) => response.status === 200);
  if (!successfulResponse) {
    return;
  }

  const accessToken = successfulResponse.json("data.accessToken");
  const profileResponse = http.get(`${baseUrl}/api/users/me/profile`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  check(profileResponse, {
    "reuse detection revokes the winner session too": (response) =>
      response.status === 401,
  });
}
