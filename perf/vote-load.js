// Load test for the vote endpoint (bonus 2).
//
//   docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < perf/vote-load.js
//
// Knobs (env vars): BASE_URL, RATE (requests/s, default 300), DURATION (default 60s).
// Each iteration casts a vote from a unique, check-digit-valid CPF, so the DB fills up with real
// rows: RATE * DURATION votes on a single agenda. The fake CPF client answers at random, so both
// 201 (able to vote) and 422 (unable to vote) are expected outcomes; anything else is a failure.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 300);
const DURATION = __ENV.DURATION || '60s';
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const accepted = new Counter('votes_accepted');
const unable = new Counter('votes_unable');

export const options = {
  scenarios: {
    votes: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 50,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const agenda = http.post(`${BASE_URL}/api/v1/agendas`, JSON.stringify({ title: `Load test ${new Date().toISOString()}` }), JSON_HEADERS);
  if (agenda.status !== 201) throw new Error(`could not create agenda: ${agenda.status} ${agenda.body}`);
  const agendaId = agenda.json('id');
  const session = http.post(`${BASE_URL}/api/v1/agendas/${agendaId}/session`, JSON.stringify({ durationMinutes: 60 }), JSON_HEADERS);
  if (session.status !== 201) throw new Error(`could not open session: ${session.status} ${session.body}`);
  return { agendaId };
}

export default function (data) {
  const cpf = cpfFor(__VU * 1_000_000 + __ITER);
  const choice = __ITER % 3 === 0 ? 'NO' : 'YES';
  const res = http.post(`${BASE_URL}/api/v1/agendas/${data.agendaId}/votes`, JSON.stringify({ cpf, choice }), {
    ...JSON_HEADERS,
    responseCallback: http.expectedStatuses(201, 422),
  });
  if (res.status === 201) accepted.add(1);
  if (res.status === 422) unable.add(1);
  check(res, { 'vote accepted (201) or member unable (422)': (r) => r.status === 201 || r.status === 422 });
}

export function teardown(data) {
  const result = http.get(`${BASE_URL}/api/v1/agendas/${data.agendaId}/result`);
  console.log(`result for agenda ${data.agendaId}: ${result.body}`);
}

/** Builds a valid CPF (mod-11 check digits) from a 9-digit base derived from the given number. */
function cpfFor(n) {
  let base = String(n % 1_000_000_000).padStart(9, '0');
  if (/^(\d)\1{8}$/.test(base)) return cpfFor(n + 1); // all-equal digits are rejected by the validator
  const digits = base.split('').map(Number);
  const d1 = checkDigit(digits);
  const d2 = checkDigit([...digits, d1]);
  return base + d1 + d2;
}

function checkDigit(digits) {
  const weightStart = digits.length + 1;
  const sum = digits.reduce((acc, d, i) => acc + d * (weightStart - i), 0);
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}
