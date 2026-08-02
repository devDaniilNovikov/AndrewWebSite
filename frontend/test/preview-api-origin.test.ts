import { describe, expect, it } from 'vitest';
import { parsePreviewApiOrigin } from '../scripts/lib/preview-api-origin.mjs';

describe('parsePreviewApiOrigin', () => {
  it('keeps preview submission disabled when no origin is configured', () => {
    expect(parsePreviewApiOrigin('preview', undefined)).toBeUndefined();
    expect(parsePreviewApiOrigin('preview', '')).toBeUndefined();
  });

  it.each([
    ['http://localhost:8080', 'http://localhost:8080'],
    ['http://127.1.2.3:8080', 'http://127.1.2.3:8080'],
    ['http://[::1]:8080', 'http://[::1]:8080'],
  ])('accepts the loopback origin %s', (input, expected) => {
    expect(parsePreviewApiOrigin('preview', input)).toBe(expected);
  });

  it.each([
    'https://localhost:8080',
    'http://example.test:8080',
    'http://user@localhost:8080',
    'http://localhost:8080/api',
    'http://localhost:8080?mode=test',
    'http://localhost:8080/#fragment',
  ])('rejects the unsafe preview API origin %s', (input) => {
    expect(() => parsePreviewApiOrigin('preview', input)).toThrow(
      'NEXT_PUBLIC_PREVIEW_API_ORIGIN',
    );
  });

  it('rejects preview API configuration in a production build', () => {
    expect(() =>
      parsePreviewApiOrigin('production', 'http://127.0.0.1:8080'),
    ).toThrow('forbidden for production');
  });
});
