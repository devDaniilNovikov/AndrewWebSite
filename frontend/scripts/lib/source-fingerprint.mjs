import { createHash } from 'node:crypto';
import { readdir, readFile } from 'node:fs/promises';
import { relative, resolve } from 'node:path';

const ignoredDirectories = new Set([
  '.git',
  '.next',
  'coverage',
  'node_modules',
  'out',
  'playwright-report',
  'test-results',
]);

async function collectFiles(directory, rootDirectory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    if (ignoredDirectories.has(entry.name)) {
      continue;
    }

    const absolutePath = resolve(directory, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await collectFiles(absolutePath, rootDirectory)));
    } else if (entry.isFile() && !entry.name.endsWith('.tsbuildinfo')) {
      files.push({
        absolutePath,
        relativePath: relative(rootDirectory, absolutePath).replaceAll(
          '\\',
          '/',
        ),
      });
    }
  }

  return files;
}

export async function createSourceFingerprint(rootDirectory) {
  const hash = createHash('sha256');
  const files = (await collectFiles(rootDirectory, rootDirectory)).sort(
    (a, b) => a.relativePath.localeCompare(b.relativePath),
  );

  for (const file of files) {
    hash.update(file.relativePath);
    hash.update('\0');
    hash.update(await readFile(file.absolutePath));
    hash.update('\0');
  }

  return hash.digest('hex');
}
