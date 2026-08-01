import { spawnSync } from 'node:child_process';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { findStaticBoundaryViolations } from '../scripts/lib/static-boundary.mjs';

const temporaryDirectories: string[] = [];

async function createFixture() {
  const directory = await mkdtemp(resolve(tmpdir(), 'andrew-boundary-'));
  temporaryDirectories.push(directory);
  return directory;
}

async function writeFixtureFile(
  fixtureDirectory: string,
  relativePath: string,
  contents: string,
) {
  const absolutePath = resolve(fixtureDirectory, relativePath);
  await mkdir(resolve(absolutePath, '..'), { recursive: true });
  await writeFile(absolutePath, contents);
}

afterEach(async () => {
  await Promise.all(
    temporaryDirectories
      .splice(0)
      .map((directory) => rm(directory, { recursive: true, force: true })),
  );
});

describe('static frontend boundary', () => {
  it('accepts the foundation tree with no runtime server or network surface', async () => {
    await expect(findStaticBoundaryViolations(resolve('.'))).resolves.toEqual(
      [],
    );
  });

  it('rejects API, proxy, route, server action, and network surfaces', async () => {
    const fixture = await createFixture();
    await writeFixtureFile(
      fixture,
      'next.config.mjs',
      "export default { output: 'export', images: { unoptimized: true } };",
    );
    await writeFixtureFile(
      fixture,
      'app/page.tsx',
      [
        "'use server';",
        "import { headers } from 'next/headers';",
        "export const dynamic = 'force-dynamic';",
        "fetch('https://example.invalid/media');",
      ].join('\n'),
    );
    await writeFixtureFile(fixture, 'app/api/route.ts', 'export {};');
    await writeFixtureFile(fixture, 'src/proxy.ts', 'export {};');
    await writeFixtureFile(fixture, 'node_modules/ignored.ts', 'fetch("x");');

    const violations = await findStaticBoundaryViolations(fixture);

    expect(violations.join('\n')).toContain('API route');
    expect(violations.join('\n')).toContain('route handler');
    expect(violations.join('\n')).toContain('additional route surface');
    expect(violations.join('\n')).toContain('middleware or proxy');
    expect(violations.join('\n')).toContain('Server Action directive');
    expect(violations.join('\n')).toContain('Next server API import');
    expect(violations.join('\n')).toContain('runtime network client');
    expect(violations.join('\n')).toContain('external runtime URL');
    expect(violations.join('\n')).toContain('dynamic rendering configuration');
  });

  it('rejects missing or server-capable Next configuration', async () => {
    const missingConfigFixture = await createFixture();
    await expect(
      findStaticBoundaryViolations(missingConfigFixture),
    ).resolves.toContain(
      'next.config.mjs: missing static export configuration',
    );

    const unsafeConfigFixture = await createFixture();
    await writeFixtureFile(
      unsafeConfigFixture,
      'next.config.mjs',
      [
        'export default {',
        "  output: 'standalone',",
        '  images: { unoptimized: false },',
        '  headers: async () => [],',
        '  redirects: async () => [],',
        '  rewrites: async () => [],',
        '};',
      ].join('\n'),
    );

    const violations = await findStaticBoundaryViolations(unsafeConfigFixture);
    expect(violations).toContain("next.config.mjs: output must be 'export'");
    expect(violations).toContain(
      'next.config.mjs: images.unoptimized must be true',
    );
    expect(violations).toContain('next.config.mjs: headers is forbidden');
    expect(violations).toContain('next.config.mjs: redirects is forbidden');
    expect(violations).toContain('next.config.mjs: rewrites is forbidden');
  });

  it('exposes the command-line success contract', () => {
    const result = spawnSync(
      process.execPath,
      ['scripts/verify-static-boundary.mjs'],
      { encoding: 'utf8' },
    );

    expect(result.status).toBe(0);
    expect(result.stdout).toContain('Static frontend boundary verified.');
    expect(result.stderr).toBe('');
  });
});
