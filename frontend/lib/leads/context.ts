import type { LeadAnalyticsEventName, LeadContext } from './domain-types';

const SOURCE_SECTION_PATTERN = /^[a-z0-9][a-z0-9_-]{0,63}$/u;

export const DEFAULT_LEAD_CONTEXT: LeadContext = Object.freeze({
  intent: 'repair',
  sourceSection: 'request',
});

export function parseLeadContext(detail: unknown): LeadContext | null {
  if (typeof detail !== 'object' || detail === null) {
    return null;
  }

  const candidate = detail as Record<string, unknown>;
  if (
    Object.keys(candidate).some(
      (key) => key !== 'intent' && key !== 'sourceSection',
    ) ||
    (candidate.intent !== 'repair' && candidate.intent !== 'maintenance') ||
    typeof candidate.sourceSection !== 'string' ||
    !SOURCE_SECTION_PATTERN.test(candidate.sourceSection)
  ) {
    return null;
  }

  return Object.freeze({
    intent: candidate.intent,
    sourceSection: candidate.sourceSection,
  });
}

export function dispatchLeadAnalytics(
  name: LeadAnalyticsEventName,
  sourceSection: string,
): void {
  const safeContext = parseLeadContext({ intent: 'repair', sourceSection });
  const safeSourceSection =
    safeContext?.sourceSection ?? DEFAULT_LEAD_CONTEXT.sourceSection;

  window.dispatchEvent(
    new CustomEvent('andrew:analytics-request', {
      detail: Object.freeze({ name, sourceSection: safeSourceSection }),
    }),
  );
}
