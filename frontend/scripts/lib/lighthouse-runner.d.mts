export type LighthouseCategory =
  'performance' | 'accessibility' | 'best-practices';

export type LighthouseScores = Readonly<Record<LighthouseCategory, number>>;

export const LIGHTHOUSE_CATEGORIES: readonly LighthouseCategory[];
export const LIGHTHOUSE_RUN_COUNT: 3;
export const LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN: 3;

export function shouldGzipStaticResponse(
  extension: string,
  acceptEncoding?: string,
): boolean;

export function calculateCategoryMedians(
  runs: readonly LighthouseScores[],
): LighthouseScores;

export function assertLighthouseMedians(
  medians: LighthouseScores,
  threshold?: number,
): void;

export function extractLighthouseScores(result: {
  categories: Readonly<
    Record<string, Readonly<{ score?: number | null }> | undefined>
  >;
}): LighthouseScores;
