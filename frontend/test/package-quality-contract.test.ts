import { readFile, stat } from 'node:fs/promises';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

type FrontendPackage = Readonly<{
  devDependencies: Readonly<Record<string, string>>;
  scripts: Readonly<Record<string, string>>;
}>;

async function readFrontendPackage(): Promise<FrontendPackage> {
  return JSON.parse(
    await readFile(resolve('package.json'), 'utf8'),
  ) as FrontendPackage;
}

describe('F6 package quality contract', () => {
  it('pins the Lighthouse toolchain exactly', async () => {
    const manifest = await readFrontendPackage();

    expect(manifest.devDependencies.lighthouse).toBe('13.4.1');
    expect(manifest.devDependencies['chrome-launcher']).toBe('1.2.1');
  });

  it('runs the Lighthouse gate as part of the serialized verify contract', async () => {
    const manifest = await readFrontendPackage();

    expect(manifest.scripts['test:lighthouse']).toBe(
      'node scripts/test-lighthouse.mjs',
    );
    expect(manifest.scripts.verify).toContain('pnpm run test:lighthouse');
    expect(manifest.scripts.verify.indexOf('pnpm run test:lighthouse')).toBe(
      manifest.scripts.verify.lastIndexOf('pnpm run test:lighthouse'),
    );
  });

  it('ships one compact upright Inter Variable subset with swap and preload', async () => {
    const fontModule = await readFile(resolve('app/fonts.ts'), 'utf8');
    const fontFile = await stat(resolve('app/InterVariable-cyrillic.woff2'));
    const fontNotice = await readFile(resolve('LICENSES/README.md'), 'utf8');

    expect(fontModule).toContain("src: './InterVariable-cyrillic.woff2'");
    expect(fontModule).toContain("display: 'swap'");
    expect(fontModule).toContain('preload: true');
    expect(fontModule).toContain("style: 'normal'");
    expect(fontModule).not.toMatch(/italic/iu);
    expect(fontFile.size).toBeLessThan(100_000);
    expect(fontNotice).toContain('InterVariable-cyrillic.woff2');
    expect(fontNotice).toContain('inter-ui@4.1.1');
  });
});
