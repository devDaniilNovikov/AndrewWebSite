import type { components } from '../../generated/openapi';

export type LegitimateLeadRequest =
  components['schemas']['LegitimateLeadRequest'];
export type HoneypotLeadRequest = components['schemas']['HoneypotLeadRequest'];
export type LeadRequest = components['schemas']['LeadRequest'];
export type LeadProblem = components['schemas']['Problem'];
export type LeadIntent = LegitimateLeadRequest['intent'];

export type LegitimateLeadDraft = Omit<
  LegitimateLeadRequest,
  'requestId' | 'website' | 'comment'
> & {
  readonly comment?: string;
};

export type LeadSubmissionDraft =
  LegitimateLeadDraft | Pick<HoneypotLeadRequest, 'website'>;

export interface LeadFormValues {
  readonly name: string;
  readonly phone: string;
  readonly comment: string;
  readonly intent: LeadIntent;
  readonly consent: boolean;
  readonly website: string;
}

export type LeadValidationField =
  'name' | 'phone' | 'comment' | 'intent' | 'consent' | 'sourcePath';

export type LeadValidationErrorCode =
  | 'name_required'
  | 'name_length'
  | 'phone_required'
  | 'phone_length'
  | 'phone_format'
  | 'comment_length'
  | 'intent_invalid'
  | 'consent_required'
  | 'source_path_invalid';

export type LeadValidationErrors = Partial<
  Readonly<Record<LeadValidationField, LeadValidationErrorCode>>
>;

export type LeadValidationResult =
  | { readonly ok: true; readonly draft: LeadSubmissionDraft }
  | { readonly ok: false; readonly errors: LeadValidationErrors };

export interface LeadAttempt {
  readonly revision: number;
  readonly payload: LeadRequest;
  readonly body: string;
}

export interface LeadAttemptState {
  readonly revision: number;
  readonly active: LeadAttempt | null;
  readonly inFlight: boolean;
}

export type CreateRequestId = () => string;

export type LeadEndpointDisabledReason =
  | 'unknown_build_mode'
  | 'page_not_loopback'
  | 'preview_api_missing'
  | 'preview_api_invalid';

export type LeadEndpointPolicy =
  | {
      readonly enabled: true;
      readonly endpoint: string;
      readonly mode: 'preview' | 'production';
    }
  | {
      readonly enabled: false;
      readonly reason: LeadEndpointDisabledReason;
    };

export type LeadResponseKind =
  | 'accepted'
  | 'invalid_request'
  | 'conflict'
  | 'unsupported_media_type'
  | 'rate_limited'
  | 'unavailable'
  | 'network_error'
  | 'timeout'
  | 'unexpected';

export interface LeadResponseOutcome {
  readonly kind: LeadResponseKind;
  readonly message: string;
  readonly retryable: boolean;
  readonly invalidateAttempt: boolean;
  readonly retryAfterSeconds?: number;
}
