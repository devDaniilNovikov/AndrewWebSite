import type { LeadAttempt } from './domain-types';
import { isAllowedLeadEndpoint } from './endpoint-policy';

export const LEAD_TIMEOUT_MILLISECONDS = 15_000;

export type LeadTransportResult =
  | {
      readonly kind: 'response';
      readonly status: number;
      readonly retryAfter: string | null;
    }
  | { readonly kind: 'timeout' }
  | { readonly kind: 'network' };

export async function submitLeadAttempt(
  endpoint: string,
  attempt: LeadAttempt,
  timeoutMilliseconds = LEAD_TIMEOUT_MILLISECONDS,
): Promise<LeadTransportResult> {
  if (!isAllowedLeadEndpoint(endpoint)) {
    throw new Error('Lead endpoint is not allowed');
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMilliseconds);

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: attempt.body,
      signal: controller.signal,
      credentials: 'omit',
      cache: 'no-store',
      redirect: 'error',
      referrerPolicy: 'no-referrer',
    });

    if (controller.signal.aborted) {
      return { kind: 'timeout' };
    }

    return {
      kind: 'response',
      status: response.status,
      retryAfter: response.headers.get('Retry-After'),
    };
  } catch {
    return controller.signal.aborted
      ? { kind: 'timeout' }
      : { kind: 'network' };
  } finally {
    clearTimeout(timeout);
  }
}
