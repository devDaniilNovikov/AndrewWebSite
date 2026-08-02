import { afterEach, describe, expect, it, vi } from 'vitest';
import type { LeadAttempt } from '../lib/leads/domain-types';
import { submitLeadAttempt } from '../lib/leads/transport';

const payload = Object.freeze({
  requestId: '11111111-1111-4111-8111-111111111111',
  name: 'Тест',
  phone: '0000000',
  sourcePath: '/',
  intent: 'repair' as const,
  consent: true as const,
});
const attempt: LeadAttempt = Object.freeze({
  revision: 0,
  payload,
  body: JSON.stringify(payload),
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('lead transport', () => {
  it('posts the immutable body with privacy-preserving request options', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, {
        status: 202,
        headers: { 'Retry-After': '120' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await submitLeadAttempt('/api/leads', attempt);

    expect(result).toEqual({
      kind: 'response',
      status: 202,
      retryAfter: '120',
    });
    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/leads',
      expect.objectContaining({
        method: 'POST',
        body: attempt.body,
        cache: 'no-store',
        credentials: 'omit',
        redirect: 'error',
        referrerPolicy: 'no-referrer',
        headers: { 'Content-Type': 'application/json' },
        signal: expect.any(AbortSignal),
      }),
    );
  });

  it('rejects every endpoint outside the relative or loopback lead path', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await expect(
      submitLeadAttempt('https://example.invalid/api/leads', attempt),
    ).rejects.toThrow('Lead endpoint is not allowed');
    await expect(
      submitLeadAttempt('http://127.0.0.1:8080/other', attempt),
    ).rejects.toThrow('Lead endpoint is not allowed');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('maps a rejected request to a network result without exposing details', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('private')));

    await expect(
      submitLeadAttempt('http://127.0.0.1:8080/api/leads', attempt),
    ).resolves.toEqual({ kind: 'network' });
  });

  it('aborts after fifteen seconds and reports a timeout', async () => {
    vi.useFakeTimers();
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_endpoint: string, init: RequestInit) =>
          new Promise<Response>((_resolve, reject) => {
            init.signal?.addEventListener('abort', () => {
              reject(new DOMException('aborted', 'AbortError'));
            });
          }),
      ),
    );

    const result = submitLeadAttempt('/api/leads', attempt);
    await vi.advanceTimersByTimeAsync(15_000);

    await expect(result).resolves.toEqual({ kind: 'timeout' });
  });
});
