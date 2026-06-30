import http from 'k6/http';
import { check, sleep } from 'k6';
import { SUPABASE_URL, getSupabaseHeaders } from './config.js';

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
    'http_req_duration{testcase:25_vus}': ['p(95)<4000'],
    'http_req_duration{testcase:50_vus}': ['p(95)<10000'],
    'http_req_duration{testcase:75_vus}': ['p(95)<25000'],
    'http_req_duration{testcase:100_vus}': ['p(95)<45000'],
    http_req_failed: ['rate<0.02'],
  }
};

export default function () {
  const idTanaman = Math.floor(Math.random() * 5) + 1;
  const selectParams = '*,pivot_tanaman_penyakit(penyakit(*)),pivot_bagian_tanaman(bagian_tanaman(*))';
  const url = `${SUPABASE_URL}/rest/v1/tanaman?select=${encodeURIComponent(selectParams)}&id_tanaman=eq.${idTanaman}`;
  const headers = getSupabaseHeaders();
  
  const res = http.get(url, { headers });

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response body is not empty': (r) => r.body && r.body.length > 0,
  });

  if (!success) {
    console.warn(`[WARNING] Request failed! VU: ${__VU} | Status: ${res.status} | Error: ${res.error || 'None'}`);
  }

  sleep(1);
}
