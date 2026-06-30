import http from 'k6/http';
import { check, sleep } from 'k6';
import { CHATBOT_URL } from './config.js';

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    test_25: { executor: 'constant-vus', vus: 25, duration: '1m', startTime: '0s', tags: { testcase: '25_vus' } },
    test_50: { executor: 'constant-vus', vus: 50, duration: '1m', startTime: '1m15s', tags: { testcase: '50_vus' } },
    test_75: { executor: 'constant-vus', vus: 75, duration: '1m', startTime: '2m30s', tags: { testcase: '75_vus' } },
    test_100: { executor: 'constant-vus', vus: 100, duration: '1m', startTime: '3m45s', tags: { testcase: '100_vus' } },
  },
  thresholds: {
    checks: ['rate>0.95'],
    'http_req_duration{testcase:25_vus}': ['p(95)<45000'], // Diperlonggar ke 45 detik untuk cold start / server gratis
    'http_req_duration{testcase:50_vus}': ['p(95)<45000'],
    'http_req_duration{testcase:75_vus}': ['p(95)<45000'],
    'http_req_duration{testcase:100_vus}': ['p(95)<45000'],
    http_req_failed: ['rate<0.10'], // Toleransi error rate 10% untuk rate limiting Hugging Face pada beban tinggi
  }
};

export default function () {
  const url = `${CHATBOT_URL}/chat`;
  const payload = JSON.stringify({
    message: 'Bagaimana cara mengobati penyakit layu fusarium pada pisang?',
  });
  const params = { headers: { 'Content-Type': 'application/json' } };

  const res = http.post(url, payload, params);

  const success = check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response has reply': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.reply !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    console.warn(`[WARNING] Chatbot Request failed! VU: ${__VU} | Status: ${res.status} | Error: ${res.error || 'None'}`);
  }

  sleep(2);
}
