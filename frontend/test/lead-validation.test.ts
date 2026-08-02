import { describe, expect, it } from 'vitest';
import { validateLeadForm } from '../lib/leads/validation';
import type { LeadFormValues } from '../lib/leads/domain-types';

const validValues = (
  overrides: Partial<LeadFormValues> = {},
): LeadFormValues => ({
  name: '  Те\u0301ст  ',
  phone: '  +0 (000) 000-00-00  ',
  comment: '  Проверка формы  ',
  intent: 'repair',
  consent: true,
  website: '',
  ...overrides,
});

describe('lead form validation', () => {
  it('normalizes a legitimate lead without retaining an empty website field', () => {
    const result = validateLeadForm(validValues(), '/services/');

    expect(result).toEqual({
      ok: true,
      draft: {
        name: 'Те́ст'.normalize('NFC'),
        phone: '+0 (000) 000-00-00',
        comment: 'Проверка формы',
        sourcePath: '/services/',
        intent: 'repair',
        consent: true,
      },
    });
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
        consent: false,
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
        comment: 'x'.repeat(1001),
        intent: 'invalid' as LeadFormValues['intent'],
        consent: false,
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
        consent: 'consent_required',
        sourcePath: 'source_path_invalid',
      },
    });
  });

  it.each([
    '',
    '//authority',
    '/safe?query',
    '/safe#fragment',
    '/safe\\path',
    '/safe/../escape',
    `/safe/${String.fromCharCode(1)}`,
    `/${'x'.repeat(2048)}`,
  ])('rejects unsafe source path %j', (sourcePath) => {
    const result = validateLeadForm(validValues(), sourcePath);

    expect(result).toMatchObject({
      ok: false,
      errors: { sourcePath: 'source_path_invalid' },
    });
  });

  it.each([
    ['000000', 'phone_length'],
    ['0000000000000000', 'phone_length'],
    ['000-000-A', 'phone_format'],
    ['+'.repeat(33), 'phone_length'],
  ] as const)('rejects invalid phone %j', (phone, code) => {
    const result = validateLeadForm(validValues({ phone }), '/');

    expect(result).toMatchObject({ ok: false, errors: { phone: code } });
  });
});
