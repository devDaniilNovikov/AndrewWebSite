import { describe, expect, it, vi } from 'vitest';
import {
  createLeadAttemptState,
  invalidateLeadAttempt,
  markLeadAttemptFinished,
  markLeadAttemptStarted,
  markLeadFormEdited,
  prepareLeadAttempt,
} from '../lib/leads/attempt-state';
import type { LegitimateLeadDraft } from '../lib/leads/domain-types';

const draft: LegitimateLeadDraft = {
  name: 'Тест',
  phone: '0000000',
  sourcePath: '/',
  intent: 'repair',
  consent: true,
};

describe('immutable lead attempt state', () => {
  it('creates one UUID and reuses the immutable payload until an edit', () => {
    const createRequestId = vi.fn(() => '11111111-1111-4111-8111-111111111111');
    const stringify = vi.spyOn(JSON, 'stringify');
    const initial = createLeadAttemptState();
    const first = prepareLeadAttempt(initial, draft, createRequestId);
    const retry = prepareLeadAttempt(
      first.state,
      { ...draft },
      createRequestId,
    );
    const stringifyCallCount = stringify.mock.calls.length;
    stringify.mockRestore();

    expect(initial).toEqual({ revision: 0, active: null, inFlight: false });
    expect(first.attempt).toBe(retry.attempt);
    expect(first.attempt.payload).toEqual({
      requestId: '11111111-1111-4111-8111-111111111111',
      ...draft,
    });
    expect(Object.isFrozen(first.attempt)).toBe(true);
    expect(Object.isFrozen(first.attempt.payload)).toBe(true);
    expect(first.attempt.body).toBe(retry.attempt.body);
    expect(JSON.parse(first.attempt.body)).toEqual(first.attempt.payload);
    expect(stringifyCallCount).toBe(1);
    expect(createRequestId).toHaveBeenCalledOnce();
  });

  it('creates a new UUID after any form edit', () => {
    const createRequestId = vi
      .fn()
      .mockReturnValueOnce('11111111-1111-4111-8111-111111111111')
      .mockReturnValueOnce('22222222-2222-4222-8222-222222222222');
    const first = prepareLeadAttempt(
      createLeadAttemptState(),
      draft,
      createRequestId,
    );
    const edited = markLeadFormEdited(first.state);
    const second = prepareLeadAttempt(
      edited,
      { ...draft, comment: 'Изменено' },
      createRequestId,
    );

    expect(edited).toEqual({ revision: 1, active: null, inFlight: false });
    expect(second.attempt.payload).toMatchObject({
      requestId: '22222222-2222-4222-8222-222222222222',
      comment: 'Изменено',
    });
  });

  it('invalidates a conflict so the next attempt receives a new UUID', () => {
    const first = prepareLeadAttempt(
      createLeadAttemptState(),
      draft,
      () => '11111111-1111-4111-8111-111111111111',
    );
    const invalidated = invalidateLeadAttempt(first.state);
    const next = prepareLeadAttempt(
      invalidated,
      draft,
      () => '22222222-2222-4222-8222-222222222222',
    );

    expect(invalidated.revision).toBe(1);
    expect(next.attempt.payload).toMatchObject({
      requestId: '22222222-2222-4222-8222-222222222222',
    });
  });

  it('tracks in-flight state without mutating earlier snapshots', () => {
    const prepared = prepareLeadAttempt(
      createLeadAttemptState(),
      draft,
      () => '11111111-1111-4111-8111-111111111111',
    );
    const started = markLeadAttemptStarted(prepared.state);
    const finished = markLeadAttemptFinished(started);

    expect(prepared.state.inFlight).toBe(false);
    expect(started.inFlight).toBe(true);
    expect(finished.inFlight).toBe(false);
    expect(finished.active).toBe(prepared.attempt);
  });

  it('prepares a minimal honeypot attempt without allocating a UUID', () => {
    const createRequestId = vi.fn(() => 'must-not-be-used');
    const prepared = prepareLeadAttempt(
      createLeadAttemptState(),
      { website: 'bot-marker' },
      createRequestId,
    );

    expect(prepared.attempt.payload).toEqual({ website: 'bot-marker' });
    expect(createRequestId).not.toHaveBeenCalled();
  });

  it('rejects duplicate starts and starts without a prepared attempt', () => {
    expect(() => markLeadAttemptStarted(createLeadAttemptState())).toThrow(
      'No lead attempt is prepared',
    );

    const prepared = prepareLeadAttempt(
      createLeadAttemptState(),
      draft,
      () => '11111111-1111-4111-8111-111111111111',
    );
    const started = markLeadAttemptStarted(prepared.state);
    expect(() => markLeadAttemptStarted(started)).toThrow(
      'Lead attempt is already in flight',
    );
  });
});
