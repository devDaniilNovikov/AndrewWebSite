import { createHash } from 'node:crypto';
import { readdir, readFile, writeFile } from 'node:fs/promises';
import { relative, resolve } from 'node:path';

const CONTENT_SECURITY_POLICY_HEADER = 'Content-Security-Policy';
export const OFFLINE_STANDALONE_ARTIFACT_PATH = 'andrew-website-updated.html';
const SERVE_HEADER_VALUE_MAX_LENGTH = 2_048;
const SERVE_HEADER_RULE_MAX_COUNT = 50;
const offlineStandalonePolicy = policy([
  ['default-src', ["'none'"]],
  ['form-action', ["'none'"]],
  ['base-uri', ["'none'"]],
  ['object-src', ["'none'"]],
  ['frame-ancestors', ["'none'"]],
]);
const contentSecurityPolicyMetaPattern =
  /<meta\b(?=[^>]*\bhttp-equiv\s*=\s*["']Content-Security-Policy["'])[^>]*>/iu;

function compareStrings(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function inlineElementSources(html, expression) {
  return [...html.matchAll(expression)]
    .filter((match) => !/\bsrc\s*=/iu.test(match.groups?.attributes ?? ''))
    .map((match) => match.groups?.source ?? '');
}

function uniqueHashes(sources) {
  return [...new Set(sources.map((source) => hashCspSource(source)))].sort(
    compareStrings,
  );
}

function directive(name, sources) {
  return `${name} ${sources.join(' ')}`;
}

function policy(directives) {
  return directives
    .map(([name, sources]) => directive(name, sources))
    .join('; ');
}

function previewConnectSources(previewApiOrigin) {
  if (previewApiOrigin === undefined) return ["'self'"];

  let parsed;
  try {
    parsed = new URL(previewApiOrigin);
  } catch {
    throw new Error('Preview API origin must be a bare HTTP loopback origin.');
  }

  const loopback =
    parsed.hostname === 'localhost' ||
    parsed.hostname === '[::1]' ||
    parsed.hostname === '::1' ||
    /^127(?:\.\d{1,3}){3}$/u.test(parsed.hostname);
  if (
    parsed.protocol !== 'http:' ||
    !loopback ||
    parsed.username !== '' ||
    parsed.password !== '' ||
    parsed.pathname !== '/' ||
    parsed.search !== '' ||
    parsed.hash !== '' ||
    parsed.origin !== previewApiOrigin
  ) {
    throw new Error('Preview API origin must be a bare HTTP loopback origin.');
  }

  return ["'self'", parsed.origin];
}

function markupWithoutInlineElementBodies(html) {
  return html.replace(
    /<(script|style)\b(?<attributes>[^>]*)>[\s\S]*?<\/\1>/giu,
    '<$1$<attributes>></$1>',
  );
}

export function assertNoInlinePresentationAttributes(html) {
  const markup = markupWithoutInlineElementBodies(html);
  for (const match of markup.matchAll(/<[a-z][^<>]*>/giu)) {
    if (/\s(?:style|on[a-z][\w:-]*)\s*=/iu.test(match[0])) {
      throw new Error(
        'Generated HTML contains an inline style or event-handler attribute.',
      );
    }
  }
}

function assertNoExistingContentSecurityPolicy(html) {
  if (contentSecurityPolicyMetaPattern.test(html)) {
    throw new Error(
      'Generated HTML already contains a Content Security Policy.',
    );
  }
}

export function hashCspSource(source) {
  return `'sha256-${createHash('sha256').update(source, 'utf8').digest('base64')}'`;
}

export function buildHostedContentSecurityPolicy({ html, previewApiOrigin }) {
  assertNoInlinePresentationAttributes(html);
  assertNoExistingContentSecurityPolicy(html);
  const scriptHashes = uniqueHashes(
    inlineElementSources(
      html,
      /<script\b(?<attributes>[^>]*)>(?<source>[\s\S]*?)<\/script>/giu,
    ),
  );
  const styleHashes = uniqueHashes(
    inlineElementSources(
      html,
      /<style\b(?<attributes>[^>]*)>(?<source>[\s\S]*?)<\/style>/giu,
    ),
  );

  return policy([
    ['default-src', ["'none'"]],
    ['script-src', ["'self'", ...scriptHashes]],
    ['script-src-attr', ["'none'"]],
    ['style-src', ["'self'", ...styleHashes]],
    ['style-src-attr', ["'none'"]],
    ['img-src', ["'self'", 'data:']],
    ['font-src', ["'self'"]],
    ['connect-src', previewConnectSources(previewApiOrigin)],
    ['form-action', ["'none'"]],
    ['base-uri', ["'none'"]],
    ['object-src', ["'none'"]],
    ['frame-ancestors', ["'none'"]],
    ['frame-src', ["'none'"]],
    ['worker-src', ["'none'"]],
    ['manifest-src', ["'none'"]],
    ['media-src', ["'none'"]],
  ]);
}

export function buildStandaloneContentSecurityPolicy(html) {
  assertNoInlinePresentationAttributes(html);
  assertNoExistingContentSecurityPolicy(html);
  const scriptHashes = uniqueHashes(
    inlineElementSources(
      html,
      /<script\b(?<attributes>[^>]*)>(?<source>[\s\S]*?)<\/script>/giu,
    ),
  );
  const styleHashes = uniqueHashes(
    inlineElementSources(
      html,
      /<style\b(?<attributes>[^>]*)>(?<source>[\s\S]*?)<\/style>/giu,
    ),
  );

  return policy([
    ['default-src', ["'none'"]],
    ['script-src', scriptHashes.length > 0 ? scriptHashes : ["'none'"]],
    ['script-src-attr', ["'none'"]],
    ['style-src', styleHashes.length > 0 ? styleHashes : ["'none'"]],
    ['style-src-attr', ["'none'"]],
    ['img-src', ['data:']],
    ['font-src', ['data:']],
    ['connect-src', ["'none'"]],
    ['form-action', ["'none'"]],
    ['base-uri', ["'none'"]],
    ['object-src', ["'none'"]],
    ['frame-src', ["'none'"]],
    ['worker-src', ["'none'"]],
    ['manifest-src', ["'none'"]],
    ['media-src', ["'none'"]],
  ]);
}

export function injectStandaloneContentSecurityPolicy(html) {
  const contentSecurityPolicy = buildStandaloneContentSecurityPolicy(html);
  const head = /<head(?:\s[^>]*)?>/iu;
  if (!head.test(html)) {
    throw new Error('Standalone export does not contain a head element.');
  }

  return html.replace(
    head,
    (openingHead) =>
      `${openingHead}<meta data-csp-mode="standalone" http-equiv="Content-Security-Policy" content=${JSON.stringify(contentSecurityPolicy)}>`,
  );
}

async function collectHtmlFiles(directory, rootDirectory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const absolutePath = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectHtmlFiles(absolutePath, rootDirectory)));
    } else if (entry.isFile() && entry.name.endsWith('.html')) {
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

export async function writeHostedCspConfig({
  outputDirectory,
  previewApiOrigin,
}) {
  const htmlFiles = (await collectHtmlFiles(outputDirectory, outputDirectory))
    .filter(
      ({ relativePath }) => relativePath !== OFFLINE_STANDALONE_ARTIFACT_PATH,
    )
    .sort((left, right) =>
      compareStrings(left.relativePath, right.relativePath),
    );
  if (htmlFiles.length === 0) {
    throw new Error(
      'Static export contains no HTML documents for CSP headers.',
    );
  }
  if (htmlFiles.length + 1 > SERVE_HEADER_RULE_MAX_COUNT) {
    throw new Error('Static export exceeds the serve header rule limit.');
  }

  const headers = [];
  for (const file of htmlFiles) {
    const html = await readFile(file.absolutePath, 'utf8');
    const contentSecurityPolicy = buildHostedContentSecurityPolicy({
      html,
      previewApiOrigin,
    });
    if (contentSecurityPolicy.length > SERVE_HEADER_VALUE_MAX_LENGTH) {
      throw new Error(
        `${file.relativePath} Content Security Policy exceeds ${SERVE_HEADER_VALUE_MAX_LENGTH} characters.`,
      );
    }

    headers.push({
      source: file.relativePath,
      headers: [
        { key: CONTENT_SECURITY_POLICY_HEADER, value: contentSecurityPolicy },
      ],
    });
  }

  headers.push({
    source: OFFLINE_STANDALONE_ARTIFACT_PATH,
    headers: [
      {
        key: CONTENT_SECURITY_POLICY_HEADER,
        value: offlineStandalonePolicy,
      },
      {
        key: 'Content-Disposition',
        value: `attachment; filename="${OFFLINE_STANDALONE_ARTIFACT_PATH}"`,
      },
      { key: 'X-Content-Type-Options', value: 'nosniff' },
    ],
  });
  headers.sort((left, right) => compareStrings(left.source, right.source));

  const outputPath = resolve(outputDirectory, 'serve.json');
  await writeFile(
    outputPath,
    `${JSON.stringify({ headers }, null, 2)}\n`,
    'utf8',
  );
  return Object.freeze({ documents: htmlFiles.length, outputPath });
}
