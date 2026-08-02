import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parsePreviewApiOrigin } from './lib/preview-api-origin.mjs';
import { assertPinnedNodeVersion } from './lib/toolchain.mjs';

assertPinnedNodeVersion();

const frontendDirectory = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '..',
);
const nextBinary = resolve(
  frontendDirectory,
  'node_modules/next/dist/bin/next',
);
let previewApiOrigin;

try {
  previewApiOrigin = parsePreviewApiOrigin(
    'preview',
    process.env.NEXT_PUBLIC_PREVIEW_API_ORIGIN,
  );
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(2);
}

const developmentServer = spawnSync(
  process.execPath,
  [nextBinary, 'dev', ...process.argv.slice(2)],
  {
    cwd: frontendDirectory,
    stdio: 'inherit',
    env: {
      ...process.env,
      NEXT_PUBLIC_BUILD_MODE: 'preview',
      NEXT_PUBLIC_PREVIEW_API_ORIGIN: previewApiOrigin ?? '',
      NEXT_TELEMETRY_DISABLED: '1',
    },
  },
);

process.exit(developmentServer.status ?? 1);
