import { describe, expect, it } from 'vitest';
import {
  assertLighthouseMedians,
  calculateCategoryMedians,
  extractLighthouseScores,
  LIGHTHOUSE_CATEGORIES,
  LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN,
  LIGHTHOUSE_RUN_COUNT,
  shouldGzipStaticResponse,
} from '../scripts/lib/lighthouse-runner.mjs';

describe('Lighthouse runner policy', () => {
  it('pins three non-SEO mobile quality categories', () => {
    expect(LIGHTHOUSE_RUN_COUNT).toBe(3);
    expect(LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN).toBe(3);
    expect(LIGHTHOUSE_CATEGORIES).toEqual([
      'performance',
      'accessibility',
      'best-practices',
    ]);
    expect(LIGHTHOUSE_CATEGORIES).not.toContain('seo');
  });

  it('calculates a category-by-category median across three cold runs', () => {
    expect(
      calculateCategoryMedians([
        { accessibility: 96, 'best-practices': 91, performance: 87 },
        { accessibility: 94, 'best-practices': 95, performance: 93 },
        { accessibility: 98, 'best-practices': 93, performance: 91 },
      ]),
    ).toEqual({
      accessibility: 96,
      'best-practices': 93,
      performance: 91,
    });
  });

  it('requires exactly three score sets', () => {
    expect(() =>
      calculateCategoryMedians([
        { accessibility: 100, 'best-practices': 100, performance: 100 },
      ]),
    ).toThrow('Expected 3 Lighthouse runs; received 1.');
  });

  it('extracts integer percentage scores and rejects missing categories', () => {
    expect(
      extractLighthouseScores({
        categories: {
          accessibility: { score: 0.98 },
          'best-practices': { score: 0.94 },
          performance: { score: 0.915 },
        },
      }),
    ).toEqual({
      accessibility: 98,
      'best-practices': 94,
      performance: 92,
    });
    expect(() =>
      extractLighthouseScores({
        categories: {
          accessibility: { score: 1 },
          'best-practices': { score: 1 },
        },
      }),
    ).toThrow('Lighthouse did not return a performance score.');
  });

  it('fails closed when any median is below 90', () => {
    expect(() =>
      assertLighthouseMedians(
        { accessibility: 100, 'best-practices': 92, performance: 89 },
        90,
      ),
    ).toThrow(
      'Lighthouse median below 90: performance=89, accessibility=100, best-practices=92',
    );
  });

  it('accepts the inclusive 90-point boundary', () => {
    expect(() =>
      assertLighthouseMedians(
        { accessibility: 90, 'best-practices': 90, performance: 90 },
        90,
      ),
    ).not.toThrow();
  });

  it('compresses text assets when the browser accepts gzip', () => {
    expect(shouldGzipStaticResponse('.html', 'gzip, deflate, br')).toBe(true);
    expect(shouldGzipStaticResponse('.css', 'gzip')).toBe(true);
    expect(shouldGzipStaticResponse('.js', 'br')).toBe(false);
    expect(shouldGzipStaticResponse('.woff2', 'gzip')).toBe(false);
  });
});
