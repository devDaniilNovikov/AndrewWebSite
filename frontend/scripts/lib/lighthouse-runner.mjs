export const LIGHTHOUSE_CATEGORIES = Object.freeze([
  'performance',
  'accessibility',
  'best-practices',
]);
export const LIGHTHOUSE_RUN_COUNT = 3;
export const LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN = 3;
const gzipExtensions = new Set(['.css', '.html', '.js', '.json', '.map']);

export function shouldGzipStaticResponse(extension, acceptEncoding = '') {
  if (!gzipExtensions.has(extension)) {
    return false;
  }

  return acceptEncoding.split(',').some((entry) => {
    const [encoding, ...parameters] = entry.trim().toLowerCase().split(';');
    const quality = parameters
      .map((parameter) => parameter.trim())
      .find((parameter) => parameter.startsWith('q='));

    return encoding === 'gzip' && quality !== 'q=0' && quality !== 'q=0.0';
  });
}

function median(values) {
  const sorted = [...values].sort((left, right) => left - right);
  return sorted[Math.floor(sorted.length / 2)];
}

export function calculateCategoryMedians(runs) {
  if (runs.length !== LIGHTHOUSE_RUN_COUNT) {
    throw new Error(
      `Expected ${LIGHTHOUSE_RUN_COUNT} Lighthouse runs; received ${runs.length}.`,
    );
  }

  return Object.fromEntries(
    LIGHTHOUSE_CATEGORIES.map((category) => [
      category,
      median(runs.map((run) => run[category])),
    ]),
  );
}

export function assertLighthouseMedians(medians, threshold = 90) {
  const failures = LIGHTHOUSE_CATEGORIES.filter(
    (category) => medians[category] < threshold,
  );

  if (failures.length > 0) {
    const scores = LIGHTHOUSE_CATEGORIES.map(
      (category) => `${category}=${medians[category]}`,
    ).join(', ');
    throw new Error(`Lighthouse median below ${threshold}: ${scores}`);
  }
}

export function extractLighthouseScores(result) {
  return Object.fromEntries(
    LIGHTHOUSE_CATEGORIES.map((category) => {
      const score = result.categories[category]?.score;

      if (typeof score !== 'number') {
        throw new Error(`Lighthouse did not return a ${category} score.`);
      }

      return [category, Math.round(score * 100)];
    }),
  );
}
