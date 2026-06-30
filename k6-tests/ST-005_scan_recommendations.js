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
  const headers = getSupabaseHeaders();

  // 1. Endpoint Rekomendasi Tanaman Detail
  const tanamanParams = '*,pivot_tanaman_penyakit(penyakit(*)),pivot_bagian_tanaman(bagian_tanaman(*))';
  const tanamanUrl = `${SUPABASE_URL}/rest/v1/tanaman?select=${encodeURIComponent(tanamanParams)}`;

  // 2. Endpoint Rekomendasi Resep Detail
  const resepParams = '*,cara_pemakaian(*),tutorial(*),bahan_resep(*),pivot_resep_penyakit(penyakit(*)),pivot_resep_tanaman(tanaman(*))';
  const resepUrl = `${SUPABASE_URL}/rest/v1/resep?select=${encodeURIComponent(resepParams)}`;

  // Kirim request secara batch (bersamaan) untuk mensimulasikan pemanggilan paralel di DetailScanActivity
  const responses = http.batch([
    ['GET', tanamanUrl, null, { headers }],
    ['GET', resepUrl, null, { headers }]
  ]);

  const success1 = check(responses[0], {
    'tanaman detail status is 200': (r) => r.status === 200,
    'tanaman detail body is not empty': (r) => r.body && r.body.length > 0,
  });

  const success2 = check(responses[1], {
    'resep detail status is 200': (r) => r.status === 200,
    'resep detail body is not empty': (r) => r.body && r.body.length > 0,
  });

  if (!success1) {
    console.warn(`[WARNING] Tanaman request failed! VU: ${__VU} | Status: ${responses[0].status} | Error: ${responses[0].error || 'None'}`);
  }
  if (!success2) {
    console.warn(`[WARNING] Resep request failed! VU: ${__VU} | Status: ${responses[1].status} | Error: ${responses[1].error || 'None'}`);
  }

  sleep(1);
}
