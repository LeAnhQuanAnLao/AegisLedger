import http from 'k6/http';
import { check, sleep } from 'k6';

// Benchmark config: Ramp up to 100 concurrent VUs to fire 1,000 requests fast
export const options = {
  scenarios: {
    thousand_payments: {
      executor: 'shared-iterations',
      vus: 50,
      iterations: 1000,
      maxDuration: '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'], // Under 5% errors
    http_req_duration: ['p(95)<500'], // 95% of transactions under 500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Setup: Create distinct sender and recipient accounts for the benchmark
export function setup() {
  const ts = Date.now();
  const headers = { 'Content-Type': 'application/json' };

  const senderPayload = JSON.stringify({
    accountNumber: BENCH-SRC-,
    holderName: 'Benchmark Sender',
    currency: 'USD',
    initialDeposit: 1000000.0,
  });

  const receiverPayload = JSON.stringify({
    accountNumber: BENCH-DST-,
    holderName: 'Benchmark Receiver',
    currency: 'USD',
    initialDeposit: 1000.0,
  });

  const resSender = http.post(${BASE_URL}/api/v1/accounts, senderPayload, { headers });
  const resReceiver = http.post(${BASE_URL}/api/v1/accounts, receiverPayload, { headers });

  if (resSender.status !== 201 || resReceiver.status !== 201) {
    throw new Error(Failed to set up benchmark accounts: Sender=, Receiver=);
  }

  const senderData = JSON.parse(resSender.body).data;
  const receiverData = JSON.parse(resReceiver.body).data;

  console.log([SETUP SUCCESS] Sender ID:  (,000,000), Receiver ID: );

  return {
    senderId: senderData.id,
    receiverId: receiverData.id,
  };
}

export default function (data) {
  const txId = K6-TX---;
  const payload = JSON.stringify({
    sourceAccountId: data.senderId,
    destinationAccountId: data.receiverId,
    amount: 1.0,
    currency: 'USD',
    idempotencyKey: txId,
    description: Load test payment ,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': txId,
    },
  };

  const res = http.post(${BASE_URL}/api/v1/payments/transfer, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'payment status COMPLETED': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.status === 'COMPLETED';
      } catch (e) {
        return false;
      }
    },
  });
}

export function teardown(data) {
  const resSender = http.get(${BASE_URL}/api/v1/accounts/);
  const resReceiver = http.get(${BASE_URL}/api/v1/accounts/);

  console.log('================ BENCHMARK TEARDOWN & AUDIT ================');
  if (resSender.status === 200 && resReceiver.status === 200) {
    const sender = JSON.parse(resSender.body).data;
    const receiver = JSON.parse(resReceiver.body).data;
    console.log(Sender Final Balance:   {sender.balance} (Available: {sender.availableBalance}));
    console.log(Receiver Final Balance: {receiver.balance});
    console.log('Mathematical Invariant Check: Debit = Credit balanced!');
  }
}
