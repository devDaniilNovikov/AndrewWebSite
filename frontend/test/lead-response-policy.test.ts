import { describe, expect, it } from 'vitest';
import {
  classifyLeadFailure,
  classifyLeadResponse,
  parseRetryAfterSeconds,
} from '../lib/leads/response-policy';

describe('lead response policy', () => {
  it.each([
    [202, 'accepted', false, false],
    [400, 'invalid_request', false, false],
    [409, 'conflict', true, true],
    [413, 'invalid_request', false, false],
    [415, 'unsupported_media_type', false, false],
    [503, 'unavailable', true, false],
    [418, 'unexpected', false, false],
  ] as const)(
    'maps HTTP %i to a fixed PII-free outcome',
    (status, kind, retryable, invalidateAttempt) => {
      expect(classifyLeadResponse(status, null)).toMatchObject({
        kind,
        retryable,
        invalidateAttempt,
      });
    },
  );

  it('maps 429 to a retryable cooldown and preserves the attempt', () => {
    expect(classifyLeadResponse(429, '120')).toMatchObject({
      kind: 'rate_limited',
      retryable: true,
      invalidateAttempt: false,
      retryAfterSeconds: 120,
    });
  });

  it.each(['network_error', 'timeout'] as const)(
    'keeps an unchanged attempt retryable after %s',
    (kind) => {
      expect(classifyLeadFailure(kind)).toMatchObject({
        kind,
        retryable: true,
        invalidateAttempt: false,
      });
    },
  );

  it.each([
    ['1', 1],
    ['3600', 3600],
    [null, 60],
    ['', 60],
    ['0', 60],
    ['3601', 60],
    ['1.5', 60],
    [' 60', 60],
    ['60 ', 60],
    ['date-value', 60],
  ] as const)('parses Retry-After %j as %i seconds', (header, expected) => {
    expect(parseRetryAfterSeconds(header)).toBe(expected);
  });

  it('never includes a server response body in its fixed message', () => {
    const outcome = classifyLeadResponse(400, null);

    expect(outcome.message).toBe(
      'Проверьте заполненные поля и отправьте заявку снова.',
    );
    expect(JSON.stringify(outcome)).not.toContain('detail');
  });
});
