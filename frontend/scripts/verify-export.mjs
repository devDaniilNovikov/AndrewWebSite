import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { createFileManifest, hashFileManifest } from './lib/file-manifest.mjs';

const outputDirectory = resolve('out');
const requiredProductFiles = [
  'uslugi.html',
  'remont-torgovogo-holodilnogo-oborudovaniya.html',
  'remont-ledogeneratorov.html',
  'o-kompanii.html',
  'raboty.html',
  'tseny.html',
  'kontakty.html',
];

function runPreviewBuild() {
  const result = spawnSync(process.execPath, ['scripts/build.mjs', 'preview'], {
    stdio: 'inherit',
  });

  if (result.status !== 0) {
    throw new Error('Preview build failed.');
  }
}

runPreviewBuild();
const firstManifest = await createFileManifest(outputDirectory);
runPreviewBuild();
const secondManifest = await createFileManifest(outputDirectory);

const requiredFilesExist =
  existsSync(resolve(outputDirectory, 'index.html')) &&
  existsSync(resolve(outputDirectory, '404.html')) &&
  existsSync(resolve(outputDirectory, '_next/static')) &&
  requiredProductFiles.every((file) =>
    existsSync(resolve(outputDirectory, file)),
  );
const outputIsIgnored =
  spawnSync('git', ['check-ignore', '--quiet', 'out/']).status === 0;

if (
  !requiredFilesExist ||
  !outputIsIgnored ||
  JSON.stringify(firstManifest) !== JSON.stringify(secondManifest)
) {
  console.error('Static export verification failed.');
  process.exit(1);
}

console.log(
  `Deterministic static export verified: ${hashFileManifest(secondManifest)}`,
);
