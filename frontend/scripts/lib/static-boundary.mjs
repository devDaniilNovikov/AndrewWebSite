import { access, readFile, readdir } from 'node:fs/promises';
import { basename, relative, resolve } from 'node:path';

const generatedDirectories = new Set([
  '.git',
  '.next',
  'coverage',
  'node_modules',
  'out',
  'playwright-report',
  'test-results',
]);
const sourceExtensions = new Set([
  '.css',
  '.js',
  '.jsx',
  '.mjs',
  '.ts',
  '.tsx',
]);
const forbiddenRuntimePatterns = [
  ['Server Action directive', /['"]use server['"]/u],
  ['Next server API import', /from\s+['"]next\/(?:headers|server)['"]/u],
  [
    'runtime network client',
    /\b(?:EventSource|WebSocket|XMLHttpRequest)\b|navigator\.sendBeacon\b|(?:from\s+|require\s*\(\s*)['"](?:axios|got|ky|ofetch)['"]/u,
  ],
  [
    'external runtime URL',
    /['"`]\s*(?:https?:)?\/\/(?:[A-Za-z0-9]|\[)|url\(\s*['"]?(?:https?:)?\/\/(?:[A-Za-z0-9]|\[)/u,
  ],
  [
    'dynamic rendering configuration',
    /export\s+const\s+(?:dynamic|revalidate|runtime)\s*=/u,
  ],
];
const nativeFetchPattern = /\b(?:(?:globalThis|window)\.)?fetch\b/u;
const allowedFetchFiles = new Set(['lib/leads/transport.ts']);
const allowedAppFiles = new Set([
  'app/InterVariable-cyrillic.woff2',
  'app/fonts.ts',
  'app/globals.css',
  'app/layout.tsx',
  'app/not-found.tsx',
  'app/page.tsx',
]);

async function pathExists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function walk(directory, rootDirectory) {
  if (!(await pathExists(directory))) {
    return [];
  }

  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const absolutePath = resolve(directory, entry.name);
    const relativePath = relative(rootDirectory, absolutePath).replaceAll(
      '\\',
      '/',
    );

    if (entry.isDirectory() && generatedDirectories.has(relativePath)) {
      continue;
    }

    if (entry.isDirectory()) {
      files.push(...(await walk(absolutePath, rootDirectory)));
    } else if (entry.isFile()) {
      files.push({
        absolutePath,
        relativePath,
      });
    }
  }

  return files;
}

function extension(path) {
  const suffix = path.slice(path.lastIndexOf('.'));
  return sourceExtensions.has(suffix) ? suffix : '';
}

export async function findStaticBoundaryViolations(rootDirectory) {
  const violations = [];
  const allFiles = await walk(rootDirectory, rootDirectory);

  for (const file of allFiles) {
    const normalizedPath = `/${file.relativePath}`;
    const fileName = basename(file.relativePath);

    if (
      (file.relativePath.startsWith('app/') ||
        file.relativePath.startsWith('src/app/')) &&
      !allowedAppFiles.has(file.relativePath)
    ) {
      violations.push(`${file.relativePath}: additional route surface`);
    }

    if (/\/(?:src\/)?(?:app|pages)\/api(?:\/|$)/u.test(normalizedPath)) {
      violations.push(`${file.relativePath}: API route`);
    }

    if (/^(?:middleware|proxy)\.(?:js|jsx|mjs|ts|tsx)$/u.test(fileName)) {
      violations.push(`${file.relativePath}: middleware or proxy`);
    }

    if (/^route\.(?:js|jsx|mjs|ts|tsx)$/u.test(fileName)) {
      violations.push(`${file.relativePath}: route handler`);
    }
  }

  const runtimeDirectories = [
    'app',
    'components',
    'lib',
    'src/app',
    'src/components',
    'src/lib',
  ];
  const runtimeFiles = (
    await Promise.all(
      runtimeDirectories.map((directory) =>
        walk(resolve(rootDirectory, directory), rootDirectory),
      ),
    )
  )
    .flat()
    .filter((file) => extension(file.relativePath));

  for (const file of runtimeFiles) {
    const contents = await readFile(file.absolutePath, 'utf8');

    if (
      nativeFetchPattern.test(contents) &&
      !allowedFetchFiles.has(file.relativePath)
    ) {
      violations.push(`${file.relativePath}: runtime network client`);
    }

    for (const [label, pattern] of forbiddenRuntimePatterns) {
      if (pattern.test(contents)) {
        violations.push(`${file.relativePath}: ${label}`);
      }
    }
  }

  const configPath = resolve(rootDirectory, 'next.config.mjs');
  if (!(await pathExists(configPath))) {
    violations.push('next.config.mjs: missing static export configuration');
    return violations;
  }

  const config = await readFile(configPath, 'utf8');

  if (!/\boutput\s*:\s*['"]export['"]/u.test(config)) {
    violations.push("next.config.mjs: output must be 'export'");
  }

  if (!/\bunoptimized\s*:\s*true\b/u.test(config)) {
    violations.push('next.config.mjs: images.unoptimized must be true');
  }

  for (const serverFeature of ['headers', 'redirects', 'rewrites']) {
    if (new RegExp(`\\b${serverFeature}\\s*:`).test(config)) {
      violations.push(`next.config.mjs: ${serverFeature} is forbidden`);
    }
  }

  return violations;
}
