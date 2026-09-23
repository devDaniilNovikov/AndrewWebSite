import { spawnSync } from 'node:child_process';
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { blockerIds } from './lib/production-readiness.mjs';

const outputDirectory = resolve('out');
const manifestDirectory = mkdtempSync(resolve(tmpdir(), 'andrew-readiness-'));
const missingManifest = resolve(manifestDirectory, 'readiness.json');
let verified = false;

try {
  writeFileSync(
    missingManifest,
    JSON.stringify(Object.fromEntries(blockerIds.map((id) => [id, 'missing']))),
  );
  rmSync(outputDirectory, { recursive: true, force: true });
  mkdirSync(outputDirectory, { recursive: true });
  writeFileSync(resolve(outputDirectory, 'stale-preview.txt'), 'preview');

  const blocked = spawnSync(
    process.execPath,
    ['scripts/build.mjs', 'production'],
    {
      encoding: 'utf8',
      env: { ...process.env, PRODUCTION_READINESS_MANIFEST: missingManifest },
    },
  );
  const reportedBlockerIds = blocked.stderr.trim().split('\n');

  const committed = spawnSync(
    process.execPath,
    ['scripts/validate-production-content.mjs'],
    {
      encoding: 'utf8',
      env: { ...process.env, PRODUCTION_READINESS_MANIFEST: '' },
    },
  );

  verified =
    blocked.status !== 0 &&
    !existsSync(outputDirectory) &&
    JSON.stringify(reportedBlockerIds) === JSON.stringify(blockerIds) &&
    committed.status === 0;
} finally {
  rmSync(manifestDirectory, { recursive: true, force: true });
}

if (!verified) {
  console.error('Production gate verification failed.');
  process.exit(1);
}

console.log(
  `Production gate verified: blocks ${blockerIds.join(', ')}; committed content passes`,
);
