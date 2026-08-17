import {
  mkdir,
  readFile,
  readdir,
  realpath,
  writeFile,
} from 'node:fs/promises';
import { dirname, isAbsolute, relative, resolve, sep } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import {
  injectStandaloneContentSecurityPolicy,
  OFFLINE_STANDALONE_ARTIFACT_PATH,
} from './lib/content-security-policy.mjs';

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, '..');
const defaultInputPath = resolve(frontendDirectory, 'out/index.html');
const defaultOutputPath = resolve(
  frontendDirectory,
  'out',
  OFFLINE_STANDALONE_ARTIFACT_PATH,
);
const nextAssetPrefix = '/_next/';
const turbopackCurrentScriptExpression =
  '"object"==typeof document?document.currentScript:void 0';

const standaloneBootstrap = String.raw`
<script data-standalone-bootstrap="true">
(() => {
  const nativeFetch = window.fetch.bind(window);
  window.fetch = (input, init) => {
    const inputUrl =
      typeof input === 'string' || input instanceof URL ? input : input.url;

    try {
      const url = new URL(inputUrl, window.location.href);
      if (window.location.protocol === 'file:' && url.protocol === 'file:') {
        return Promise.resolve(new Response(null, { status: 404 }));
      }
    } catch {
      return nativeFetch(input, init);
    }

    return nativeFetch(input, init);
  };
})();
</script>`;

const standaloneNavigation = String.raw`
<script data-standalone-navigation="true">
(() => {
  const resolveLocalTarget = (href) => {
    if (href === '/') return 'main-content';
    if (href.startsWith('/#')) return href.slice(2);
    return null;
  };

  document.addEventListener('click', (event) => {
    const origin = event.target;
    if (!(origin instanceof Element)) return;
    const anchor = origin.closest('a[href]');
    if (!(anchor instanceof HTMLAnchorElement)) return;
    const targetId = resolveLocalTarget(anchor.getAttribute('href') ?? '');
    if (targetId === null) return;

    const target = document.getElementById(targetId);
    if (target === null) return;

    event.preventDefault();
    event.stopImmediatePropagation();
    const dialog = anchor.closest('dialog');
    if (dialog instanceof HTMLDialogElement && dialog.open) dialog.close();
    target.scrollIntoView({
      behavior: matchMedia('(prefers-reduced-motion: reduce)').matches
        ? 'auto'
        : 'smooth',
      block: 'start',
    });
    try {
      history.pushState(null, '', targetId === 'main-content' ? '#' : '#' + targetId);
    } catch {
      location.hash = targetId === 'main-content' ? '' : targetId;
    }
  }, true);
})();
</script>`;

async function resolveContainedPath(rootPath, targetPath) {
  const [canonicalRoot, canonicalTarget] = await Promise.all([
    realpath(rootPath),
    realpath(targetPath),
  ]);
  const relativeAssetPath = relative(canonicalRoot, canonicalTarget);
  if (
    relativeAssetPath === '..' ||
    relativeAssetPath.startsWith(`..${sep}`) ||
    isAbsolute(relativeAssetPath)
  ) {
    throw new Error('Standalone asset is outside the generated export.');
  }

  return canonicalTarget;
}

async function resolveAssetPath(inputPath, assetUrl) {
  if (!assetUrl.startsWith(nextAssetPrefix)) {
    throw new Error(`Unsupported standalone asset URL: ${assetUrl}`);
  }
  const exportDirectory = dirname(inputPath);
  const assetPath = resolve(exportDirectory, assetUrl.slice(1));
  return resolveContainedPath(exportDirectory, assetPath);
}

async function replaceAsync(input, expression, replacer) {
  let output = '';
  let previousIndex = 0;

  for (const match of input.matchAll(expression)) {
    if (match.index === undefined) continue;
    output += input.slice(previousIndex, match.index);
    output += await replacer(match);
    previousIndex = match.index + match[0].length;
  }

  return output + input.slice(previousIndex);
}

async function inlineFontUrls(css, cssPath, inputPath) {
  return replaceAsync(
    css,
    /url\((['"]?)\.\.\/media\/([^)'"\s]+)\1\)/gu,
    async (match) => {
      const fontPath = await resolveContainedPath(
        dirname(inputPath),
        resolve(dirname(cssPath), '../media', match[2]),
      );
      const font = await readFile(fontPath);
      return `url("data:font/woff2;base64,${font.toString('base64')}")`;
    },
  );
}

function inlineChunkSource(source, assetUrl) {
  const logicalPath = assetUrl.slice(nextAssetPrefix.length);
  const withoutCurrentScriptAssetPrefix = source.includes(
    'Expected document.currentScript',
  )
    ? source.replace(
        /function l\(\)\{let e=document\.currentScript;.*?return t\.slice\(0,n\)\}/u,
        'function l(){return""}',
      )
    : source;
  if (
    withoutCurrentScriptAssetPrefix.includes('Expected document.currentScript')
  ) {
    throw new Error('Could not patch the Next.js asset-prefix bootstrap.');
  }

  return withoutCurrentScriptAssetPrefix
    .replaceAll(turbopackCurrentScriptExpression, JSON.stringify(logicalPath))
    .replace(/<\/script/giu, '<\\/script');
}

async function createInlineChunk(inputPath, assetUrl) {
  const source = await readFile(
    await resolveAssetPath(inputPath, assetUrl),
    'utf8',
  );
  const logicalPath = assetUrl.slice(nextAssetPrefix.length);
  return `<script data-inline-chunk="${logicalPath}">${inlineChunkSource(source, assetUrl)}\n</script>`;
}

async function inlineStylesheets(html, inputPath) {
  return replaceAsync(
    html,
    /<link\b(?=[^>]*\brel="stylesheet")(?=[^>]*\bhref="(\/_next\/[^"<>]+\.css)")[^>]*\/?>/gu,
    async (match) => {
      const cssPath = await resolveAssetPath(inputPath, match[1]);
      const css = (
        await inlineFontUrls(
          await readFile(cssPath, 'utf8'),
          cssPath,
          inputPath,
        )
      ).replace(/<\/style/giu, '<\\/style');
      return `<style data-inline-stylesheet="true">${css}</style>`;
    },
  );
}

async function inlineScripts(html, inputPath) {
  const referencedChunks = new Set();
  let output = await replaceAsync(
    html,
    /<script(?<before>[^>]*)\bsrc="(?<url>\/_next\/static\/chunks\/[^"<>]+\.js)"(?<after>[^>]*)><\/script>/gu,
    async (match) => {
      const attributes = `${match.groups?.before ?? ''}${match.groups?.after ?? ''}`;
      const assetUrl = match.groups?.url;
      if (assetUrl === undefined) return '';
      if (/\bnoModule\b/iu.test(attributes)) return '';
      referencedChunks.add(assetUrl);
      return createInlineChunk(inputPath, assetUrl);
    },
  );

  const chunkDirectory = resolve(dirname(inputPath), '_next/static/chunks');
  const missingChunks = (await readdir(chunkDirectory))
    .filter((name) => name.endsWith('.js'))
    .map((name) => `${nextAssetPrefix}static/chunks/${name}`)
    .filter(
      (assetUrl) =>
        !referencedChunks.has(assetUrl) && !assetUrl.includes('0cz1d0mv5g_q7'),
    );

  if (missingChunks.length > 0) {
    const inlineMissing = (
      await Promise.all(
        missingChunks.map((assetUrl) => createInlineChunk(inputPath, assetUrl)),
      )
    ).join('');
    const runtimeMarker = '<script data-inline-chunk="static/chunks/turbopack-';
    output = output.replace(runtimeMarker, `${inlineMissing}${runtimeMarker}`);
  }

  return output;
}

async function inlineFlightAssets(html, inputPath) {
  const withoutChunkPreloads = html.replace(
    /(I\[\d+,)\[(?:\\"\/_next\/static\/chunks\/[^"\\]+\.js\\"(?:,\\"\/_next\/static\/chunks\/[^"\\]+\.js\\")*)\]/gu,
    '$1[]',
  );
  const withoutStylesheetHints = withoutChunkPreloads.replace(
    /:HL\[\\"\/_next\/static\/chunks\/[^"\\]+\.css\\",\\"style\\"(?:,\{[^}\n]*\})?\]\\n/gu,
    '',
  );
  const withoutScriptResources = withoutStylesheetHints.replace(
    /\[\\"\$\\",\\"script\\",\\"[^"\\]*\\",\{\\"src\\":\\"\/_next\/static\/chunks\/[^"\\]+\.js\\"[^}]*\}\]/gu,
    'null',
  );
  const withoutChunkResources = withoutScriptResources.replace(
    /\[\\"\$\\",\\"link\\",\\"[^"\\]*\\",\{\\"rel\\":\\"stylesheet\\",\\"href\\":\\"\/_next\/static\/chunks\/[^"\\]+\.css\\"[^}]*\}\]/gu,
    'null',
  );

  if (
    /\/_next\/static\/chunks\/[^"\\]+\.(?:css|js)/u.test(withoutChunkResources)
  ) {
    throw new Error('Standalone Flight data still contains a Next chunk URL.');
  }

  return replaceAsync(
    withoutChunkResources,
    /\/_next\/static\/media\/([^"\\]+\.woff2)/gu,
    async (match) => {
      const font = await readFile(await resolveAssetPath(inputPath, match[0]));
      return `data:font/woff2;base64,${font.toString('base64')}`;
    },
  );
}

export async function buildStandaloneHtml({
  inputPath = defaultInputPath,
  outputPath = defaultOutputPath,
} = {}) {
  let html = await readFile(inputPath, 'utf8');
  html = html.replace('<head>', `<head>${standaloneBootstrap}`);
  html = await inlineStylesheets(html, inputPath);
  html = html.replace(
    /<link\b(?=[^>]*\brel="preload")(?=[^>]*\bhref="\/_next\/)[^>]*\/?>/gu,
    '',
  );
  html = await inlineScripts(html, inputPath);
  html = await inlineFlightAssets(html, inputPath);
  html = html.replace('<html ', '<html data-standalone-export="true" ');
  html = html.replace('</body>', `${standaloneNavigation}</body>`);

  if (/(?:href|src)="\/_next\//u.test(html)) {
    throw new Error('Standalone export still contains an external Next asset.');
  }

  html = injectStandaloneContentSecurityPolicy(html);

  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, html, 'utf8');
  return Object.freeze({ bytes: Buffer.byteLength(html), outputPath });
}

const invokedPath = process.argv[1]
  ? pathToFileURL(resolve(process.argv[1])).href
  : null;

if (invokedPath === import.meta.url) {
  const result = await buildStandaloneHtml({
    outputPath: process.argv[2] ? resolve(process.argv[2]) : defaultOutputPath,
  });
  process.stdout.write(
    `Standalone HTML created: ${result.outputPath} (${result.bytes} bytes)\n`,
  );
}
