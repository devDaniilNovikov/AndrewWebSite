import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDirectory = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '..',
);
const nextBinary = resolve(
  frontendDirectory,
  'node_modules/next/dist/bin/next',
);
const developmentServer = spawnSync(
  process.execPath,
  [nextBinary, 'dev', ...process.argv.slice(2)],
  {
    cwd: frontendDirectory,
    stdio: 'inherit',
    env: {
      ...process.env,
      NEXT_PUBLIC_BUILD_MODE: 'preview',
      NEXT_TELEMETRY_DISABLED: '1',
    },
  },
);

process.exit(developmentServer.status ?? 1);
