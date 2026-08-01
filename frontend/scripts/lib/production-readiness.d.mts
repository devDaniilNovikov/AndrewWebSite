export const blockerIds: readonly string[];

export function loadProductionReadiness(
  manifestUrl?: URL | string,
): Promise<Readonly<Record<string, 'missing' | 'verified'>>>;

export function findMissingBlockerIds(
  readiness: Readonly<Record<string, 'missing' | 'verified'>>,
): string[];
