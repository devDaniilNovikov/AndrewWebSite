import type {
  CreateRequestId,
  LeadAttempt,
  LeadAttemptState,
  LeadRequest,
  LeadSubmissionDraft,
} from './domain-types';

export function createLeadAttemptState(): LeadAttemptState {
  return { revision: 0, active: null, inFlight: false };
}

function freezeAttempt(revision: number, payload: LeadRequest): LeadAttempt {
  return Object.freeze({
    revision,
    payload: Object.freeze(payload),
    body: JSON.stringify(payload),
  });
}

export function prepareLeadAttempt(
  state: LeadAttemptState,
  draft: LeadSubmissionDraft,
  createRequestId: CreateRequestId,
): { readonly state: LeadAttemptState; readonly attempt: LeadAttempt } {
  if (state.active !== null) {
    return { state, attempt: state.active };
  }

  const payload: LeadRequest =
    'website' in draft
      ? { website: draft.website }
      : { requestId: createRequestId(), ...draft };
  const attempt = freezeAttempt(state.revision, payload);
  const nextState: LeadAttemptState = {
    ...state,
    active: attempt,
  };

  return { state: nextState, attempt };
}

export function markLeadFormEdited(state: LeadAttemptState): LeadAttemptState {
  return {
    revision: state.revision + 1,
    active: null,
    inFlight: false,
  };
}

export function invalidateLeadAttempt(
  state: LeadAttemptState,
): LeadAttemptState {
  return markLeadFormEdited(state);
}

export function markLeadAttemptStarted(
  state: LeadAttemptState,
): LeadAttemptState {
  if (state.active === null) {
    throw new Error('No lead attempt is prepared');
  }
  if (state.inFlight) {
    throw new Error('Lead attempt is already in flight');
  }

  return { ...state, inFlight: true };
}

export function markLeadAttemptFinished(
  state: LeadAttemptState,
): LeadAttemptState {
  if (!state.inFlight) {
    throw new Error('Lead attempt is not in flight');
  }

  return { ...state, inFlight: false };
}
