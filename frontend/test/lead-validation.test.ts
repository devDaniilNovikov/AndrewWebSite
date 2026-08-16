import { describe, expect, it } from 'vitest';
import {
  formatRussianPhoneInput,
  validateLeadForm,
} from '../lib/leads/validation';
import type { LeadFormValues } from '../lib/leads/domain-types';

const validValues = (
  overrides: Partial<LeadFormValues> = {},
): LeadFormValues => ({
  name: '  Те\u0301ст  ',
  phone: '  89991234567  ',
  comment: '  Проверка формы  ',
  intent: 'repair',
  website: '',
  ...overrides,
});

describe('lead form validation', () => {
  it('normalizes a legitimate lead to the exact OpenAPI payload', () => {
    const result = validateLeadForm(validValues(), '/services/');

    expect(result).toEqual({
      ok: true,
      draft: {
        name: 'Те́ст'.normalize('NFC'),
        phone: '+7 (999) 123-45-67',
        comment: 'Проверка формы',
        sourcePath: '/services/',
        intent: 'repair',
        consent: true,
      },
    });
  });

  it.each(['89991234567', '79991234567', '+79991234567', '+7 (999) 123-45-67'])(
    'accepts and normalizes supported Russian phone input %j',
    (phone) => {
      const result = validateLeadForm(validValues({ phone }), '/');

      expect(result).toMatchObject({
        ok: true,
        draft: { phone: '+7 (999) 123-45-67' },
      });
    },
  );

  it.each([
    ['8', '+7'],
    ['8999', '+7 (999)'],
    ['7999123', '+7 (999) 123'],
    ['+79991234567', '+7 (999) 123-45-67'],
    ['799912345678', '799912345678'],
  ])('applies the visible phone mask to %j', (input, expected) => {
    expect(formatRussianPhoneInput(input)).toBe(expected);
  });

  it('omits a blank optional comment', () => {
    const result = validateLeadForm(
      validValues({ comment: '  \n  ', intent: 'maintenance' }),
      '/',
    );

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.draft).not.toHaveProperty('comment');
      expect(result.draft).toMatchObject({ intent: 'maintenance' });
    }
  });

  it('returns only the honeypot field when it is non-empty', () => {
    const result = validateLeadForm(
      validValues({
        name: '',
        phone: '',
        comment: 'must not be retained',
        website: 'bot-marker',
      }),
      'not-a-path',
    );

    expect(result).toEqual({ ok: true, draft: { website: 'bot-marker' } });
  });

  it('reports every invalid legitimate field with stable codes', () => {
    const result = validateLeadForm(
      validValues({
        name: 'x',
        phone: '+0 script',
        comment: 'short',
        intent: 'invalid' as LeadFormValues['intent'],
      }),
      '/safe?query=forbidden',
    );

    expect(result).toEqual({
      ok: false,
      errors: {
        name: 'name_length',
        phone: 'phone_format',
        comment: 'comment_length',
        intent: 'intent_invalid',
        sourcePath: 'source_path_invalid',
      },
    });
  });

  it.each([
    ['', 'name_required'],
    ['x', 'name_length'],
    ['x'.repeat(51), 'name_length'],
  ] as const)('enforces the required 2-50 character name %j', (name, code) => {
    const result = validateLeadForm(validValues({ name }), '/');

    expect(result).toMatchObject({ ok: false, errors: { name: code } });
  });

  it.each(['short', 'x'.repeat(1001)])(
    'rejects a non-empty comment outside 10-1000 characters',
    (comment) => {
      const result = validateLeadForm(validValues({ comment }), '/');

      expect(result).toMatchObject({
        ok: false,
        errors: { comment: 'comment_length' },
      });
    },
  );

  it.each([
    '',
    '//authority',
    '/safe?query',
    '/safe#fragment',
    '/safe\\path',
    '/safe/../escape',
    '/safe/' + String.fromCharCode(1),
    '/' + 'x'.repeat(2048),
  ])('rejects unsafe source path %j', (sourcePath) => {
    const result = validateLeadForm(validValues(), sourcePath);

    expect(result).toMatchObject({
      ok: false,
      errors: { sourcePath: 'source_path_invalid' },
    });
  });

  it.each([
    '8999123456',
    '799912345678',
    '+69991234567',
    '9991234567',
    '8 999 123 AB 67',
  ])('rejects unsupported phone %j', (phone) => {
    const result = validateLeadForm(validValues({ phone }), '/');

    expect(result).toMatchObject({
      ok: false,
      errors: { phone: 'phone_format' },
    });
  });
});
