import { createHash } from 'node:crypto';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import {
  buildHostedContentSecurityPolicy,
  buildStandaloneContentSecurityPolicy,
  hashCspSource,
  injectStandaloneContentSecurityPolicy,
  OFFLINE_STANDALONE_ARTIFACT_PATH,
  writeHostedCspConfig,
} from '../scripts/lib/content-security-policy.mjs';

const temporaryDirectories: string[] = [];

afterEach(async () => {
  await Promise.all(
    temporaryDirectories
      .splice(0)
      .map((directory) => rm(directory, { force: true, recursive: true })),
  );
});

describe('content security policy', () => {
  it('hashes the exact UTF-8 inline source and emits a strict hosted policy', () => {
    const script = 'globalThis.fixture = "тест";\n';
    const style = '.fixture { color: navy; }\n';
    const html = `<html><head><style>${style}</style></head><body><script>${script}</script></body></html>`;
    const policy = buildHostedContentSecurityPolicy({
      html,
      previewApiOrigin: 'http://127.0.0.1:4174',
    });

    expect(hashCspSource(script)).toBe(
      `'sha256-${createHash('sha256').update(script, 'utf8').digest('base64')}'`,
    );
    expect(policy).toContain(`script-src 'self' ${hashCspSource(script)}`);
    expect(policy).toContain(`style-src 'self' ${hashCspSource(style)}`);
    expect(policy).toContain("connect-src 'self' http://127.0.0.1:4174");
    expect(policy).toContain("frame-ancestors 'none'");
    expect(policy).toContain("script-src-attr 'none'");
    expect(policy).toContain("style-src-attr 'none'");
    expect(policy).not.toMatch(/'unsafe-(?:eval|inline)'/u);
    expect(
      policy.split(';').find((entry) => entry.trim().startsWith('script-src ')),
    ).not.toContain('data:');
  });

  it('writes deterministic per-document serve headers from the final export', async () => {
    const outputDirectory = await mkdtemp(resolve(tmpdir(), 'andrew-csp-'));
    temporaryDirectories.push(outputDirectory);
    await mkdir(resolve(outputDirectory, 'nested'), { recursive: true });
    await writeFile(
      resolve(outputDirectory, 'index.html'),
      '<html><head></head><body><script>globalThis.page="home"</script></body></html>',
      'utf8',
    );
    await writeFile(
      resolve(outputDirectory, 'nested/page.html'),
      '<html><head></head><body><script>globalThis.page="nested"</script></body></html>',
      'utf8',
    );

    await writeHostedCspConfig({
      outputDirectory,
      previewApiOrigin: 'http://127.0.0.1:4174',
    });
    const first = await readFile(
      resolve(outputDirectory, 'serve.json'),
      'utf8',
    );
    await writeFile(
      resolve(outputDirectory, OFFLINE_STANDALONE_ARTIFACT_PATH),
      '<html><head><meta http-equiv="Content-Security-Policy" content="default-src none"></head></html>',
      'utf8',
    );
    await writeHostedCspConfig({
      outputDirectory,
      previewApiOrigin: 'http://127.0.0.1:4174',
    });
    const second = await readFile(
      resolve(outputDirectory, 'serve.json'),
      'utf8',
    );
    const config = JSON.parse(first) as {
      headers: Array<{
        headers: Array<{ key: string; value: string }>;
        source: string;
      }>;
    };

    expect(second).toBe(first);
    expect(config.headers.map(({ source }) => source)).toEqual([
      OFFLINE_STANDALONE_ARTIFACT_PATH,
      'index.html',
      'nested/page.html',
    ]);
    expect(config.headers[1]?.headers[0]?.value).not.toBe(
      config.headers[2]?.headers[0]?.value,
    );
    expect(config.headers[1]?.headers[0]).toMatchObject({
      key: 'Content-Security-Policy',
      value: expect.stringContaining("frame-ancestors 'none'"),
    });
    expect(config.headers[0]).toEqual({
      source: OFFLINE_STANDALONE_ARTIFACT_PATH,
      headers: [
        {
          key: 'Content-Security-Policy',
          value:
            "default-src 'none'; form-action 'none'; base-uri 'none'; object-src 'none'; frame-ancestors 'none'",
        },
        {
          key: 'Content-Disposition',
          value: `attachment; filename="${OFFLINE_STANDALONE_ARTIFACT_PATH}"`,
        },
        { key: 'X-Content-Type-Options', value: 'nosniff' },
      ],
    });
  });

  it('rejects inline style attributes before publishing a strict policy', () => {
    expect(() =>
      buildHostedContentSecurityPolicy({
        html: '<html><body><p style="color:red">unsafe</p></body></html>',
      }),
    ).toThrow('inline style or event-handler attribute');
  });

  it('allows only explicit bare HTTP loopback origins for preview connections', () => {
    for (const previewApiOrigin of [
      'http://localhost:4174',
      'http://[::1]:4174',
      'http://127.12.34.56:4174',
    ]) {
      expect(
        buildHostedContentSecurityPolicy({
          html: '<html><head></head><body></body></html>',
          previewApiOrigin,
        }),
      ).toContain(`connect-src 'self' ${previewApiOrigin}`);
    }

    for (const previewApiOrigin of [
      'not a URL',
      'https://127.0.0.1:4174',
      'http://example.com:4174',
      'http://preview-user@127.0.0.1:4174',
      'http://127.0.0.1:4174/path',
      'http://127.0.0.1:4174/?query=true',
      'http://127.0.0.1:4174/#fragment',
      'http://127.0.0.1:80',
    ]) {
      expect(() =>
        buildHostedContentSecurityPolicy({
          html: '<html><head></head><body></body></html>',
          previewApiOrigin,
        }),
      ).toThrow('bare HTTP loopback origin');
    }
  });

  it('builds a strict standalone meta policy and rejects duplicate or missing heads', () => {
    const source = `const template = '<button style="color:red">safe text</button>';`;
    const html = `<html lang="ru"><head data-fixture="true"></head><body><script>${source}</script></body></html>`;
    const standalonePolicy = buildStandaloneContentSecurityPolicy(html);

    expect(standalonePolicy).toContain(`script-src ${hashCspSource(source)}`);
    expect(standalonePolicy).toContain("style-src 'none'");
    expect(standalonePolicy).toContain("connect-src 'none'");
    expect(standalonePolicy).not.toContain('frame-ancestors');
    expect(injectStandaloneContentSecurityPolicy(html)).toMatch(
      /<head data-fixture="true"><meta data-csp-mode="standalone"/u,
    );
    expect(() =>
      injectStandaloneContentSecurityPolicy('<html><body></body></html>'),
    ).toThrow('does not contain a head element');
    expect(() =>
      buildHostedContentSecurityPolicy({
        html: '<html><head><meta http-equiv="Content-Security-Policy" content="default-src none"></head></html>',
      }),
    ).toThrow('already contains a Content Security Policy');
  });

  it('rejects event-handler attributes without treating script text as markup', () => {
    expect(() =>
      buildHostedContentSecurityPolicy({
        html: '<html><body><button onclick="submitFixture()">Send</button></body></html>',
      }),
    ).toThrow('inline style or event-handler attribute');
    expect(() =>
      buildHostedContentSecurityPolicy({
        html: '<html><body><script onload="submitFixture()"></script></body></html>',
      }),
    ).toThrow('inline style or event-handler attribute');
  });

  it('refuses to publish a header config without HTML documents', async () => {
    const outputDirectory = await mkdtemp(resolve(tmpdir(), 'andrew-csp-'));
    temporaryDirectories.push(outputDirectory);
    await writeFile(resolve(outputDirectory, 'asset.txt'), 'fixture', 'utf8');

    await expect(writeHostedCspConfig({ outputDirectory })).rejects.toThrow(
      'contains no HTML documents',
    );
  });

  it('enforces serve header rule and value limits', async () => {
    const tooManyDocuments = await mkdtemp(resolve(tmpdir(), 'andrew-csp-'));
    temporaryDirectories.push(tooManyDocuments);
    await Promise.all(
      Array.from({ length: 50 }, (_, index) =>
        writeFile(
          resolve(tooManyDocuments, `page-${index}.html`),
          '<html><head></head><body></body></html>',
          'utf8',
        ),
      ),
    );
    await expect(
      writeHostedCspConfig({ outputDirectory: tooManyDocuments }),
    ).rejects.toThrow('exceeds the serve header rule limit');

    const oversizedPolicy = await mkdtemp(resolve(tmpdir(), 'andrew-csp-'));
    temporaryDirectories.push(oversizedPolicy);
    const scripts = Array.from(
      { length: 40 },
      (_, index) => `<script>globalThis.fixture${index}=${index}</script>`,
    ).join('');
    await writeFile(
      resolve(oversizedPolicy, 'index.html'),
      `<html><head></head><body>${scripts}</body></html>`,
      'utf8',
    );
    await expect(
      writeHostedCspConfig({ outputDirectory: oversizedPolicy }),
    ).rejects.toThrow('Content Security Policy exceeds 2048 characters');
  });
});
