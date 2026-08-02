import { spawnSync } from 'node:child_process';
import { rm } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parsePreviewApiOrigin } from './lib/preview-api-origin.mjs';
import { createSourceFingerprint } from './lib/source-fingerprint.mjs';
import { assertPinnedNodeVersion } from './lib/toolchain.mjs';

assertPinnedNodeVersion();

const frontendDirectory = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '..',
);
const mode = process.argv[2];

if (mode !== 'preview' && mode !== 'production') {
  console.error('Usage: node scripts/build.mjs <preview|production>');
  process.exit(2);
}

await Promise.all(
  ['.next', 'out'].map((directory) =>
    rm(resolve(frontendDirectory, directory), { recursive: true, force: true }),
  ),
);

if (mode === 'production') {
  const validation = spawnSync(
    process.execPath,
    [resolve(frontendDirectory, 'scripts/validate-production-content.mjs')],
    {
      cwd: frontendDirectory,
      stdio: 'inherit',
    },
  );

  if (validation.status !== 0) {
    process.exit(validation.status ?? 1);
  }
}

let previewApiOrigin;

try {
  previewApiOrigin = parsePreviewApiOrigin(
    mode,
    process.env.NEXT_PUBLIC_PREVIEW_API_ORIGIN,
  );
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(2);
}

const sourceFingerprint = await createSourceFingerprint(frontendDirectory);
const nextBinary = resolve(
  frontendDirectory,
  'node_modules/next/dist/bin/next',
);
const build = spawnSync(process.execPath, [nextBinary, 'build'], {
  cwd: frontendDirectory,
  stdio: 'inherit',
  env: {
    ...process.env,
    ANDREW_BUILD_ID: `andrew-${sourceFingerprint.slice(0, 24)}`,
    NEXT_PUBLIC_BUILD_MODE: mode,
    NEXT_PUBLIC_PREVIEW_API_ORIGIN: previewApiOrigin ?? '',
    NEXT_TELEMETRY_DISABLED: '1',
    SOURCE_DATE_EPOCH: '0',
    TZ: 'UTC',
  },
});

process.exit(build.status ?? 1);
