import { createHash } from 'node:crypto';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { buildStandaloneHtml } from '../scripts/build-standalone-html.mjs';

const temporaryDirectories: string[] = [];

function directive(policy: string, name: string) {
  return policy
    .split(';')
    .map((entry) => entry.trim())
    .find((entry) => entry.startsWith(`${name} `));
}

function inlineSources(html: string, tagName: 'script' | 'style') {
  const expression = new RegExp(
    `<${tagName}\\b(?<attributes>[^>]*)>(?<source>[\\s\\S]*?)<\\/${tagName}>`,
    'giu',
  );

  return [...html.matchAll(expression)]
    .filter((match) => !/\bsrc\s*=/iu.test(match.groups?.attributes ?? ''))
    .map((match) => match.groups?.source ?? '');
}

function sha256Source(source: string) {
  return `'sha256-${createHash('sha256').update(source, 'utf8').digest('base64')}'`;
}

afterEach(async () => {
  await Promise.all(
    temporaryDirectories
      .splice(0)
      .map((directory) => rm(directory, { recursive: true, force: true })),
  );
});

describe('standalone HTML export', () => {
  it('inlines every runtime, stylesheet, and font needed by file URLs', async () => {
    const directory = await mkdtemp(resolve(tmpdir(), 'andrew-standalone-'));
    temporaryDirectories.push(directory);
    const exportDirectory = resolve(directory, 'out');
    const chunkDirectory = resolve(exportDirectory, '_next/static/chunks');
    const mediaDirectory = resolve(exportDirectory, '_next/static/media');
    const outputPath = resolve(directory, 'andrew-website-updated.html');
    await mkdir(chunkDirectory, { recursive: true });
    await mkdir(mediaDirectory, { recursive: true });
    await writeFile(
      resolve(exportDirectory, 'index.html'),
      '<!DOCTYPE html><html lang="ru"><head>' +
        '<link rel="stylesheet" href="/_next/static/chunks/app.css"/>' +
        '<script src="/_next/static/chunks/asset-prefix.js"></script>' +
        '<script src="/_next/static/chunks/turbopack-fixture.js"></script>' +
        '</head><body><main id="main-content"></main>' +
        '<section id="request"></section>' +
        '<script>self.__next_f=self.__next_f||[];self.__next_f.push([1,' +
        '"2:I[1,[\\"/_next/static/chunks/turbopack-fixture.js\\"],\\"default\\"]\\n' +
        ':HL[\\"/_next/static/chunks/app.css\\",\\"style\\"]\\n' +
        '3:[\\"$\\",\\"link\\",\\"0\\",{\\"rel\\":\\"stylesheet\\",\\"href\\":\\"/_next/static/chunks/app.css\\",\\"precedence\\":\\"next\\"}]\\n' +
        '4:[\\"$\\",\\"script\\",\\"script-0\\",{\\"src\\":\\"/_next/static/chunks/turbopack-fixture.js\\",\\"async\\":true}]\\n' +
        ':HL[\\"/_next/static/media/font.woff2\\",\\"font\\"]\\n"]);</script>' +
        '</body></html>',
      'utf8',
    );
    await writeFile(
      resolve(chunkDirectory, 'app.css'),
      '@font-face{font-family:Fixture;src:url(../media/font.woff2)}/* </StYle> */',
      'utf8',
    );
    await writeFile(
      resolve(chunkDirectory, 'asset-prefix.js'),
      'function l(){let e=document.currentScript;if(!e)throw new Error("Expected document.currentScript");let t=e.src,n=t.indexOf("/_next/");return t.slice(0,n)}',
      'utf8',
    );
    await writeFile(
      resolve(chunkDirectory, 'turbopack-fixture.js'),
      '(globalThis.TURBOPACK||(globalThis.TURBOPACK=[])).push(["object"==typeof document?document.currentScript:void 0,{}]);globalThis.fixture="</ScRiPt>";',
      'utf8',
    );
    await writeFile(resolve(mediaDirectory, 'font.woff2'), new Uint8Array([0]));

    await buildStandaloneHtml({
      inputPath: resolve(exportDirectory, 'index.html'),
      outputPath,
    });

    const html = await readFile(outputPath, 'utf8');
    const policy = html.match(
      /<meta\b(?=[^>]*\bhttp-equiv="Content-Security-Policy")(?=[^>]*\bcontent="([^"]+)")[^>]*>/iu,
    )?.[1];
    expect(html).toMatch(/^<!DOCTYPE html>/u);
    expect(html).toContain('data-standalone-export="true"');
    expect(html).toContain('data-standalone-bootstrap="true"');
    expect(policy).toBeDefined();
    expect(policy).not.toMatch(/'unsafe-(?:eval|inline)'/u);
    expect(directive(policy!, 'script-src')).not.toContain('data:');
    expect(directive(policy!, 'script-src-attr')).toBe(
      "script-src-attr 'none'",
    );
    expect(directive(policy!, 'style-src-attr')).toBe("style-src-attr 'none'");
    expect(policy).not.toContain('frame-ancestors');
    for (const source of inlineSources(html, 'script')) {
      expect(directive(policy!, 'script-src')).toContain(sha256Source(source));
    }
    for (const source of inlineSources(html, 'style')) {
      expect(directive(policy!, 'style-src')).toContain(sha256Source(source));
    }
    expect(html.indexOf('http-equiv="Content-Security-Policy"')).toBeLessThan(
      html.indexOf('<script'),
    );
    expect(html).toContain("connect-src 'none'");
    expect(html).toContain("form-action 'none'");
    expect(html).toContain('data:font/woff2;base64,');
    expect(html).toContain('data-inline-chunk=');
    expect(html).not.toMatch(/(?:href|src)="\/_next\//u);
    expect(html).not.toContain('/_next/static/');
    expect(html).not.toContain('data:text/css,');
    expect(html).not.toContain('data:text/javascript,');
    expect(html).not.toContain('url(../media/');
    expect(html).not.toContain('Expected document.currentScript');
    expect(html).toContain('<\\/script>');
    expect(html).toContain('<\\/style>');
  });

  it('rejects generated asset paths that escape the export directory', async () => {
    const directory = await mkdtemp(resolve(tmpdir(), 'andrew-standalone-'));
    temporaryDirectories.push(directory);
    const exportDirectory = resolve(directory, 'out');
    await mkdir(resolve(exportDirectory, '_next/static/chunks'), {
      recursive: true,
    });
    await writeFile(resolve(directory, 'private.css'), 'body{}', 'utf8');
    await writeFile(
      resolve(exportDirectory, 'index.html'),
      '<!DOCTYPE html><html><head>' +
        '<link rel="stylesheet" href="/_next/../../private.css"/>' +
        '</head><body></body></html>',
      'utf8',
    );

    await expect(
      buildStandaloneHtml({
        inputPath: resolve(exportDirectory, 'index.html'),
        outputPath: resolve(directory, 'standalone.html'),
      }),
    ).rejects.toThrow('outside the generated export');
  });

  it('rejects inline presentation attributes that a strict style policy would block', async () => {
    const directory = await mkdtemp(resolve(tmpdir(), 'andrew-standalone-'));
    temporaryDirectories.push(directory);
    const exportDirectory = resolve(directory, 'out');
    await mkdir(resolve(exportDirectory, '_next/static/chunks'), {
      recursive: true,
    });
    await writeFile(
      resolve(exportDirectory, 'index.html'),
      '<!DOCTYPE html><html><head></head><body><main style="color:red"></main></body></html>',
      'utf8',
    );

    await expect(
      buildStandaloneHtml({
        inputPath: resolve(exportDirectory, 'index.html'),
        outputPath: resolve(directory, 'standalone.html'),
      }),
    ).rejects.toThrow('inline style or event-handler attribute');
  });
});
