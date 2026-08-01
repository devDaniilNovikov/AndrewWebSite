import { resolve } from 'node:path';
import { findStaticBoundaryViolations } from './lib/static-boundary.mjs';

const violations = await findStaticBoundaryViolations(resolve('.'));

if (violations.length > 0) {
  console.error(violations.join('\n'));
  process.exit(1);
}

console.log('Static frontend boundary verified.');
