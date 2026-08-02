import { resolve } from 'node:path';
import { findVisualContractViolations } from './lib/visual-contract.mjs';

const violations = await findVisualContractViolations(resolve('.'));

if (violations.length > 0) {
  console.error(violations.join('\n'));
  process.exit(1);
}

console.log('Behavior-only visual contract verified.');
