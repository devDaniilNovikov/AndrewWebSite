import type {
  LeadFormValues,
  LeadValidationErrorCode,
  LeadValidationErrors,
  LeadValidationField,
  LeadValidationResult,
  LegitimateLeadDraft,
} from './domain-types';

const PHONE_CHARACTERS = /^[+0-9(). -]+$/u;

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

function formatCanonicalRussianPhone(digits: string): string {
  return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(
    7,
    9,
  )}-${digits.slice(9, 11)}`;
}

function normalizeRussianPhone(value: string): string | null {
  if (!PHONE_CHARACTERS.test(value)) {
    return null;
  }

  const inputDigits = value.replace(/\D/gu, '');
  if (inputDigits.length !== 11) {
    return null;
  }

  const digits = inputDigits.startsWith('8')
    ? `7${inputDigits.slice(1)}`
    : inputDigits;
  return digits.startsWith('7') ? formatCanonicalRussianPhone(digits) : null;
}

export function formatRussianPhoneInput(value: string): string {
  const inputDigits = value.replace(/\D/gu, '');
  if (inputDigits.length === 0) {
    return '';
  }
  if (inputDigits.length > 11) {
    return inputDigits;
  }

  const digits = inputDigits.startsWith('8')
    ? `7${inputDigits.slice(1)}`
    : inputDigits;
  if (!digits.startsWith('7')) {
    return digits;
  }

  const national = digits.slice(1);
  let formatted = '+7';
  if (national.length > 0) {
    formatted += ` (${national.slice(0, 3)}`;
  }
  if (national.length >= 3) {
    formatted += ')';
  }
  if (national.length > 3) {
    formatted += ` ${national.slice(3, 6)}`;
  }
  if (national.length > 6) {
    formatted += `-${national.slice(6, 8)}`;
  }
  if (national.length > 8) {
    formatted += `-${national.slice(8, 10)}`;
  }

  return formatted;
}

export function validateLeadForm(
  values: LeadFormValues,
  sourcePath: string,
): LeadValidationResult {
  if (values.website.length > 0) {
    return { ok: true, draft: { website: values.website } };
  }

  const name = values.name.trim().normalize('NFC');
  const phoneInput = values.phone.trim().normalize('NFC');
  const phone = normalizeRussianPhone(phoneInput);
  const comment = values.comment.trim().normalize('NFC');
  const errors: Partial<Record<LeadValidationField, LeadValidationErrorCode>> =
    {};

  if (name.length === 0) {
    errors.name = 'name_required';
  } else if (unicodeLength(name) < 2 || unicodeLength(name) > 50) {
    errors.name = 'name_length';
  }

  if (phoneInput.length === 0) {
    errors.phone = 'phone_required';
  } else if (phone === null) {
    errors.phone = 'phone_format';
  }

  if (
    comment.length > 0 &&
    (unicodeLength(comment) < 10 || unicodeLength(comment) > 1000)
  ) {
    errors.comment = 'comment_length';
  }

  if (values.intent !== 'repair' && values.intent !== 'maintenance') {
    errors.intent = 'intent_invalid';
  }

  if (!isSafeSourcePath(sourcePath)) {
    errors.sourcePath = 'source_path_invalid';
  }

  if (Object.keys(errors).length > 0) {
    return { ok: false, errors: errors as LeadValidationErrors };
  }

  const draft: LegitimateLeadDraft = {
    name,
    phone: phone as string,
    sourcePath,
    intent: values.intent,
    consent: true,
    ...(comment.length > 0 ? { comment } : {}),
  };

  return { ok: true, draft };
}
