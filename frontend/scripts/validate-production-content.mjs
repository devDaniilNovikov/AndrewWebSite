import {
  findMissingBlockerIds,
  loadProductionReadiness,
} from './lib/production-readiness.mjs';

try {
  const readiness = await loadProductionReadiness();
  const missingBlockerIds = findMissingBlockerIds(readiness);

  if (missingBlockerIds.length > 0) {
    console.error(missingBlockerIds.join('\n'));
    process.exit(1);
  }
} catch {
  console.error('Production readiness manifest is invalid.');
  process.exit(1);
}
