import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { SharedArray } from "k6/data";
import { Counter, Gauge, Rate, Trend } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const vus = Number(__ENV.VUS || 50);
const duration = __ENV.DURATION || "3m";
const sessionsPath = __ENV.SESSIONS_PATH || "/runtime/measurement-sessions.json";
const sessionsDocument = JSON.parse(open(sessionsPath));
const sessions = new SharedArray("measurement sessions", () => sessionsDocument.sessions);

const refreshDuration = new Trend("refresh_duration", true);
const refreshFailures = new Rate("refresh_failures");
const refreshStarted = new Counter("refresh_started");
const refreshCompleted = new Counter("refresh_completed");
const refreshSuccesses = new Counter("refresh_successes");
const refreshFailureCount = new Counter("refresh_failure_count");
const preparedVus = new Counter("prepared_vus");
const measurementStartMs = new Gauge("measurement_start_ms");

export const options = {
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  scenarios: {
    refresh: {
      executor: "constant-vus",
      vus,
      duration,
      gracefulStop: "30s",
    },
  },
};

if (sessions.length !== vus) {
  throw new Error(`expected ${vus} prepared sessions but found ${sessions.length}`);
}

let refreshCookie;
let prepared = false;

function rotatedCookie(response) {
  const header = response.headers["Set-Cookie"] || "";
  const cookie = header.split(";")[0];
  return cookie.startsWith("refreshToken=") ? cookie : null;
}

export default function () {
  if (!prepared) {
    const index = exec.vu.idInTest - 1;
    const session = sessions[index];
    if (!session || !session.cookie) {
      exec.test.abort(`missing prepared session for VU ${exec.vu.idInTest}`);
      return;
    }
    refreshCookie = session.cookie;
    preparedVus.add(1);
    measurementStartMs.add(exec.scenario.startTime);
    prepared = true;
  }

  refreshStarted.add(1);
  const response = http.post(`${baseUrl}/api/auth/refresh`, null, {
    headers: { Cookie: refreshCookie },
    tags: { name: "POST /api/auth/refresh" },
  });

  refreshCompleted.add(1);
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
  if (success) {
    refreshSuccesses.add(1);
    const nextCookie = rotatedCookie(response);
    if (nextCookie) {
      refreshCookie = nextCookie;
    }
  } else {
    refreshFailureCount.add(1);
  }
  refreshFailures.add(!success);
}
