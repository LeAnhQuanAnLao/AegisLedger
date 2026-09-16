import http from 'k6/http';
import { check } from 'k6';

// 20 concurrent users firing at the exact same second
export const options = {
  scenarios: {
    race_drain: {
      executor: 'per-vu-iterations',
      vus: 20,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const ts = Date.now();
  const headers = { 'Content-Type': 'application/json' };

  // Victim account has EXACTLY .00
  const victimPayload = JSON.stringify({
    accountNumber: RACE-VICTIM-,
    holderName: 'Alice Limited',
    currency: 'USD',
    initialDeposit: 100.0,
  });

  const receiverPayload = JSON.stringify({
    accountNumber: RACE-RECEIVER-,
    holderName: 'Bob Merchant',
    currency: 'USD',
    initialDeposit: 0.0,
  });

  const resVictim = http.post(${BASE_URL}/api/v1/accounts, victimPayload, { headers });
  const resReceiver = http.post(${BASE_URL}/api/v1/accounts, receiverPayload, { headers });

  const victim = JSON.parse(resVictim.body).data;
  const receiver = JSON.parse(resReceiver.body).data;

  console.log([SETUP] Alice created with {victim.balance}. 20 users will try to withdraw  simultaneously!);

  return {
    victimId: victim.id,
    receiverId: receiver.id,
  };
}

export default function (data) {
  const key = RACE-KEY--;
  const payload = JSON.stringify({
    sourceAccountId: data.victimId,
    destinationAccountId: data.receiverId,
    amount: 60.0, //  * 2 =  > , so only 1 can succeed!
    currency: 'USD',
    idempotencyKey: key,
    description: Race condition test from VU ,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': key,
    },
  };

  const res = http.post(${BASE_URL}/api/v1/payments/transfer, payload, params);

  check(res, {
    'response received (200 success or 400 insufficient funds)': (r) =>
      r.status === 200 || r.status === 400,
  });
}

export function teardown(data) {
  const resVictim = http.get(${BASE_URL}/api/v1/accounts/);
  const resReceiver = http.get(${BASE_URL}/api/v1/accounts/);

  console.log('================ RACE CONDITION AUDIT ================');
  if (resVictim.status === 200 && resReceiver.status === 200) {
    const victim = JSON.parse(resVictim.body).data;
    const receiver = JSON.parse(resReceiver.body).data;
    console.log(Alice Final Balance:    {victim.balance} (Must be >= .00, expected .00));
    console.log(Bob Final Balance:      {receiver.balance} (Expected .00));

    const aliceBal = parseFloat(victim.balance);
    if (aliceBal < 0) {
      console.error('[CRITICAL FAILURE] Race Condition detected! Balance is negative!');
    } else if (aliceBal === 40.0) {
      console.log('[PERFECT AUDIT] Zero Race Condition! Exactly 1 withdrawal succeeded, 19 rejected safely!');
    }
  }
}
