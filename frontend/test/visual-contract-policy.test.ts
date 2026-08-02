import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { findVisualContractViolations } from '../scripts/lib/visual-contract.mjs';

const temporaryDirectories: string[] = [];

async function createFixture() {
  const directory = await mkdtemp(resolve(tmpdir(), 'andrew-visual-policy-'));
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

describe('behavior-only visual contract policy', () => {
  it('accepts the current placeholder-only application tree', async () => {
    await expect(findVisualContractViolations(resolve('.'))).resolves.toEqual(
      [],
    );
  });

  it('rejects screenshot assertions and browser screenshot calls', async () => {
    const fixture = await createFixture();
    const assertion = ['toHave', 'Screenshot'].join('');
    const capture = ['page', 'screenshot'].join('.');
    await writeFixtureFile(
      fixture,
      'e2e/visual.spec.ts',
      `expect(page).${assertion}();\nawait ${capture}();`,
    );

    await expect(findVisualContractViolations(fixture)).resolves.toEqual([
      'e2e/visual.spec.ts: browser screenshot capture',
      'e2e/visual.spec.ts: screenshot-based visual assertion',
    ]);
  });

  it('rejects image baselines and unverified published media', async () => {
    const fixture = await createFixture();
    await writeFixtureFile(
      fixture,
      ['e2e', 'visual.spec.ts-snapshots', ['home', 'png'].join('.')].join('/'),
      'baseline',
    );
    await writeFixtureFile(
      fixture,
      ['public', ['reference', 'jpeg'].join('.')].join('/'),
      'reference',
    );
    await writeFixtureFile(
      fixture,
      ['assets', ['stock', 'svg'].join('.')].join('/'),
      '<svg />',
    );

    await expect(findVisualContractViolations(fixture)).resolves.toEqual([
      'assets/stock.svg: media asset is outside the verified local path',
      'e2e/visual.spec.ts-snapshots/home.png: image baseline is forbidden',
      'public/reference.jpeg: published media must use public/media/verified/',
    ]);
  });

  it('reserves public/media/verified for later provenance-approved photos', async () => {
    const fixture = await createFixture();
    await writeFixtureFile(
      fixture,
      ['public', 'media', 'verified', ['hero', 'webp'].join('.')].join('/'),
      'future verified photo',
    );

    await expect(findVisualContractViolations(fixture)).resolves.toEqual([]);
  });
});
