import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { rmSync } from 'node:fs';
import { resolve } from 'node:path';
import { blockerIds } from './lib/production-readiness.mjs';

const outputDirectory = resolve('out');
rmSync(outputDirectory, { recursive: true, force: true });
mkdirSync(outputDirectory, { recursive: true });
writeFileSync(resolve(outputDirectory, 'stale-preview.txt'), 'preview');

const result = spawnSync(
  process.execPath,
  ['scripts/build.mjs', 'production'],
  {
    encoding: 'utf8',
  },
);
const reportedBlockerIds = result.stderr.trim().split('\n');

if (
  result.status === 0 ||
  existsSync(outputDirectory) ||
  JSON.stringify(reportedBlockerIds) !== JSON.stringify(blockerIds)
) {
  console.error('Production gate verification failed.');
  process.exit(1);
}

console.log(`Production gate verified: ${blockerIds.join(', ')}`);
