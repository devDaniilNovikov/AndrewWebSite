import { createHash } from 'node:crypto';
import { readdir, readFile } from 'node:fs/promises';
import { relative, resolve } from 'node:path';

async function collectFiles(directory, rootDirectory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const absolutePath = resolve(directory, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await collectFiles(absolutePath, rootDirectory)));
    } else if (entry.isFile()) {
      files.push({
        absolutePath,
        relativePath: relative(rootDirectory, absolutePath).replaceAll(
          '\\',
          '/',
        ),
      });
    } else {
      throw new Error(`Unexpected export entry: ${entry.name}`);
    }
  }

  return files;
}

export async function createFileManifest(rootDirectory) {
  const files = (await collectFiles(rootDirectory, rootDirectory)).sort(
    (a, b) => a.relativePath.localeCompare(b.relativePath),
  );

  return Promise.all(
    files.map(async ({ absolutePath, relativePath }) => ({
      path: relativePath,
      sha256: createHash('sha256')
        .update(await readFile(absolutePath))
        .digest('hex'),
    })),
  );
}

export function hashFileManifest(manifest) {
  return createHash('sha256')
    .update(manifest.map(({ path, sha256 }) => `${sha256}  ${path}`).join('\n'))
    .digest('hex');
}
