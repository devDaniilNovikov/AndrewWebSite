import type {
  LeadFormValues,
  LeadValidationErrorCode,
  LeadValidationErrors,
  LeadValidationField,
  LeadValidationResult,
  LegitimateLeadDraft,
} from './domain-types';

const PHONE_FORMAT = /^[+0-9(). -]+$/u;

function unicodeLength(value: string): number {
  return Array.from(value).length;
}

function isSafeSourcePath(sourcePath: string): boolean {
  return (
    unicodeLength(sourcePath) >= 1 &&
    unicodeLength(sourcePath) <= 2048 &&
    sourcePath.startsWith('/') &&
    !sourcePath.startsWith('//') &&
    !/[?#\\\u0000-\u001F]/u.test(sourcePath) &&
    !sourcePath.split('/').includes('..')
  );
}

export function validateLeadForm(
  values: LeadFormValues,
  sourcePath: string,
): LeadValidationResult {
  if (values.website.length > 0) {
    return { ok: true, draft: { website: values.website } };
  }

  const name = values.name.trim().normalize('NFC');
  const phone = values.phone.trim().normalize('NFC');
  const comment = values.comment.trim().normalize('NFC');
  const errors: Partial<Record<LeadValidationField, LeadValidationErrorCode>> =
    {};

  if (name.length === 0) {
    errors.name = 'name_required';
  } else if (unicodeLength(name) < 2 || unicodeLength(name) > 100) {
    errors.name = 'name_length';
  }

  if (phone.length === 0) {
    errors.phone = 'phone_required';
  } else if (unicodeLength(phone) < 7 || unicodeLength(phone) > 32) {
    errors.phone = 'phone_length';
  } else if (!PHONE_FORMAT.test(phone)) {
    errors.phone = 'phone_format';
  } else {
    const digitCount = phone.replace(/\D/gu, '').length;
    if (digitCount < 7 || digitCount > 15) {
      errors.phone = 'phone_length';
    }
  }

  if (unicodeLength(comment) > 1000) {
    errors.comment = 'comment_length';
  }

  if (values.intent !== 'repair' && values.intent !== 'maintenance') {
    errors.intent = 'intent_invalid';
  }

  if (values.consent !== true) {
    errors.consent = 'consent_required';
  }

  if (!isSafeSourcePath(sourcePath)) {
    errors.sourcePath = 'source_path_invalid';
  }

  if (Object.keys(errors).length > 0) {
    return { ok: false, errors: errors as LeadValidationErrors };
  }

  const draft: LegitimateLeadDraft = {
    name,
    phone,
    sourcePath,
    intent: values.intent,
    consent: true,
    ...(comment.length > 0 ? { comment } : {}),
  };

  return { ok: true, draft };
}
