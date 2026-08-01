import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

export const blockerIds = Object.freeze([
  'company_name',
  'phone',
  'consent_text',
  'personal_data_policy',
  'prices',
  'warranty_terms',
  'cases',
  'testimonial',
  'licensed_photographs',
]);

const allowedStates = new Set(['missing', 'verified']);
const defaultManifestPath = resolve('content/production-readiness.json');

export async function loadProductionReadiness(
  manifestUrl = defaultManifestPath,
) {
  const parsed = JSON.parse(await readFile(manifestUrl, 'utf8'));

  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('Production readiness manifest must be an object.');
  }

  const receivedIds = Object.keys(parsed);
  const unexpectedIds = receivedIds.filter((id) => !blockerIds.includes(id));
  const absentIds = blockerIds.filter((id) => !(id in parsed));

  if (unexpectedIds.length > 0 || absentIds.length > 0) {
    throw new Error('Production readiness manifest IDs do not match.');
  }

  const readiness = Object.fromEntries(
    blockerIds.map((id) => {
      const state = parsed[id];

      if (typeof state !== 'string' || !allowedStates.has(state)) {
        throw new Error('Production readiness manifest has an invalid state.');
      }

      return [id, state];
    }),
  );

  return Object.freeze(readiness);
}

export function findMissingBlockerIds(readiness) {
  return blockerIds.filter((id) => readiness[id] === 'missing');
}
