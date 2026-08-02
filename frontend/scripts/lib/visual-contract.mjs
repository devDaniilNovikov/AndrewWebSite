import { readFile, readdir } from 'node:fs/promises';
import { extname, relative, resolve } from 'node:path';

const ignoredDirectories = new Set([
  '.git',
  '.next',
  'coverage',
  'node_modules',
  'out',
  'playwright-report',
  'test-results',
]);
const imageExtensions = new Set(['.jpeg', '.jpg', '.png', '.svg', '.webp']);
const sourceExtensions = new Set(['.js', '.jsx', '.mjs', '.ts', '.tsx']);

async function walk(directory, rootDirectory) {
  let entries;

  try {
    entries = await readdir(directory, { withFileTypes: true });
  } catch (error) {
    if (error && typeof error === 'object' && error.code === 'ENOENT') {
      return [];
    }
    throw error;
  }

  const files = [];

  for (const entry of entries) {
    if (entry.isDirectory() && ignoredDirectories.has(entry.name)) {
      continue;
    }

    const absolutePath = resolve(directory, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await walk(absolutePath, rootDirectory)));
    } else if (entry.isFile()) {
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

function isBaselinePath(path) {
  return /(?:^|\/)(?:e2e|test|tests|__snapshots__|[^/]*-snapshots)(?:\/|$)/u.test(
    path,
  );
}

export async function findVisualContractViolations(rootDirectory) {
  const violations = [];
  const files = await walk(rootDirectory, rootDirectory);

  for (const file of files) {
    const extension = extname(file.relativePath).toLowerCase();

    if (imageExtensions.has(extension)) {
      if (isBaselinePath(file.relativePath)) {
        violations.push(`${file.relativePath}: image baseline is forbidden`);
      } else if (
        file.relativePath.startsWith('public/') &&
        !file.relativePath.startsWith('public/media/verified/')
      ) {
        violations.push(
          `${file.relativePath}: published media must use public/media/verified/`,
        );
      } else if (!file.relativePath.startsWith('public/media/verified/')) {
        violations.push(
          `${file.relativePath}: media asset is outside the verified local path`,
        );
      }
    }

    if (!sourceExtensions.has(extension)) {
      continue;
    }

    const contents = await readFile(file.absolutePath, 'utf8');

    if (/\btoHaveScreenshot\s*\(/u.test(contents)) {
      violations.push(
        `${file.relativePath}: screenshot-based visual assertion`,
      );
    }

    if (/\.\s*screenshot\s*\(/u.test(contents)) {
      violations.push(`${file.relativePath}: browser screenshot capture`);
    }
  }

  return violations.sort();
}
