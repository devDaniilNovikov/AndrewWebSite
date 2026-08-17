import { spawnSync } from 'node:child_process';
import { createServer } from 'node:http';
import { mkdir, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { extname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';
import { chromium } from '@playwright/test';
import * as chromeLauncher from 'chrome-launcher';
import lighthouse from 'lighthouse';
import {
  assertLighthouseMedians,
  calculateCategoryMedians,
  extractLighthouseScores,
  LIGHTHOUSE_CATEGORIES,
  LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN,
  LIGHTHOUSE_RUN_COUNT,
  shouldGzipStaticResponse,
} from './lib/lighthouse-runner.mjs';
import { assertPinnedNodeVersion } from './lib/toolchain.mjs';

const LIGHTHOUSE_THRESHOLD = 90;
const frontendDirectory = resolve(
  fileURLToPath(new URL('..', import.meta.url)),
);
const outputDirectory = resolve(frontendDirectory, 'out');
const reportDirectory = resolve(frontendDirectory, 'test-results/lighthouse');
const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.map', 'application/json; charset=utf-8'],
  ['.woff2', 'font/woff2'],
]);

assertPinnedNodeVersion();

function runPreviewBuild() {
  const build = spawnSync(process.execPath, ['scripts/build.mjs', 'preview'], {
    cwd: frontendDirectory,
    env: {
      ...process.env,
      NEXT_PUBLIC_PREVIEW_API_ORIGIN: '',
    },
    stdio: 'inherit',
  });

  if (build.status !== 0) {
    throw new Error('Lighthouse preview build failed.');
  }
}

async function resolveStaticFile(pathname) {
  const decodedPath = decodeURIComponent(pathname);
  const relativePath =
    decodedPath === '/' ? 'index.html' : decodedPath.slice(1);
  const absolutePath = resolve(outputDirectory, relativePath);
  const escapedPath = relative(outputDirectory, absolutePath);

  if (escapedPath.startsWith('..') || escapedPath.includes('\0')) {
    return undefined;
  }

  try {
    const fileStatus = await stat(absolutePath);
    return fileStatus.isFile() ? absolutePath : undefined;
  } catch {
    return undefined;
  }
}

async function loadContentSecurityPolicies() {
  const config = JSON.parse(
    await readFile(resolve(outputDirectory, 'serve.json'), 'utf8'),
  );
  if (!Array.isArray(config.headers)) {
    throw new Error('Static export CSP configuration is missing header rules.');
  }

  return new Map(
    config.headers.map((rule) => {
      const header = rule.headers?.find(
        ({ key }) => key.toLowerCase() === 'content-security-policy',
      );
      if (
        typeof rule.source !== 'string' ||
        typeof header?.value !== 'string'
      ) {
        throw new Error('Static export CSP header rule is malformed.');
      }
      return [rule.source, header.value];
    }),
  );
}

async function startStaticServer() {
  const contentSecurityPolicies = await loadContentSecurityPolicies();
  const server = createServer(async (request, response) => {
    try {
      const requestUrl = new URL(request.url ?? '/', 'http://127.0.0.1');
      const requestedFile = await resolveStaticFile(requestUrl.pathname);
      const filePath = requestedFile ?? resolve(outputDirectory, '404.html');
      const fileExtension = extname(filePath);
      const relativeFilePath = relative(outputDirectory, filePath).replaceAll(
        '\\',
        '/',
      );
      const contentSecurityPolicy =
        contentSecurityPolicies.get(relativeFilePath);
      if (fileExtension === '.html' && contentSecurityPolicy === undefined) {
        throw new Error(`Missing CSP header for ${relativeFilePath}.`);
      }
      const sourceBody = await readFile(filePath);
      const useGzip = shouldGzipStaticResponse(
        fileExtension,
        request.headers['accept-encoding'],
      );
      const body = useGzip ? gzipSync(sourceBody, { level: 9 }) : sourceBody;

      response.writeHead(requestedFile ? 200 : 404, {
        'Cache-Control': requestUrl.pathname.startsWith('/_next/static/')
          ? 'public, max-age=31536000, immutable'
          : 'no-store',
        ...(contentSecurityPolicy
          ? { 'Content-Security-Policy': contentSecurityPolicy }
          : {}),
        ...(useGzip
          ? { 'Content-Encoding': 'gzip', Vary: 'Accept-Encoding' }
          : {}),
        'Content-Type':
          contentTypes.get(fileExtension) ?? 'application/octet-stream',
      });
      response.end(body);
    } catch {
      response.writeHead(500, { 'Cache-Control': 'no-store' });
      response.end('Static preview server failure.');
    }
  });

  await new Promise((resolveListening, rejectListening) => {
    server.once('error', rejectListening);
    server.listen(0, '127.0.0.1', resolveListening);
  });

  const address = server.address();
  if (!address || typeof address === 'string') {
    server.close();
    throw new Error('Static preview server did not expose a TCP port.');
  }

  return {
    close: () =>
      new Promise((resolveClose, rejectClose) => {
        server.close((error) => {
          if (error) {
            rejectClose(error);
          } else {
            resolveClose();
          }
        });
      }),
    url: `http://127.0.0.1:${address.port}/`,
  };
}

async function launchChromium() {
  return chromeLauncher.launch({
    chromePath: chromium.executablePath(),
    chromeFlags: [
      '--headless=new',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--no-default-browser-check',
      '--no-first-run',
      '--no-sandbox',
    ],
    logLevel: 'silent',
  });
}

async function writeLighthouseReport(runNumber, attemptNumber, report, lhr) {
  const htmlReport = Array.isArray(report) ? report.join('\n') : report;
  const attemptSuffix = `run-${runNumber}-attempt-${attemptNumber}`;

  await mkdir(reportDirectory, { recursive: true });
  await Promise.all([
    writeFile(resolve(reportDirectory, `${attemptSuffix}.html`), htmlReport),
    writeFile(
      resolve(reportDirectory, `${attemptSuffix}.json`),
      `${JSON.stringify(lhr, null, 2)}\n`,
    ),
  ]);
}

async function writeCanonicalLighthouseReport(runNumber, report, lhr) {
  const htmlReport = Array.isArray(report) ? report.join('\n') : report;

  await mkdir(reportDirectory, { recursive: true });
  await Promise.all([
    writeFile(resolve(reportDirectory, `run-${runNumber}.html`), htmlReport),
    writeFile(
      resolve(reportDirectory, `run-${runNumber}.json`),
      `${JSON.stringify(lhr, null, 2)}\n`,
    ),
  ]);
}

async function runAuditAttempt(url, runNumber, attemptNumber) {
  const chrome = await launchChromium();
  activeChrome = chrome;

  try {
    const result = await lighthouse(url, {
      disableStorageReset: false,
      formFactor: 'mobile',
      logLevel: 'error',
      onlyCategories: [...LIGHTHOUSE_CATEGORIES],
      output: 'html',
      port: chrome.port,
      throttlingMethod: 'simulate',
    });

    if (!result) {
      throw new Error(
        `Lighthouse run ${runNumber}, attempt ${attemptNumber} returned no result.`,
      );
    }

    await writeLighthouseReport(
      runNumber,
      attemptNumber,
      result.report,
      result.lhr,
    );
    const scores = extractLighthouseScores(result.lhr);
    await writeCanonicalLighthouseReport(runNumber, result.report, result.lhr);

    console.log(
      `Lighthouse cold run ${runNumber}/${LIGHTHOUSE_RUN_COUNT}: ${LIGHTHOUSE_CATEGORIES.map((category) => `${category}=${scores[category]}`).join(', ')}`,
    );
    return scores;
  } finally {
    await chrome.kill();
    if (activeChrome === chrome) {
      activeChrome = undefined;
    }
  }
}

async function runAudit(url, runNumber) {
  let lastError;

  for (
    let attemptNumber = 1;
    attemptNumber <= LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN;
    attemptNumber += 1
  ) {
    try {
      return await runAuditAttempt(url, runNumber, attemptNumber);
    } catch (error) {
      lastError = error;

      if (attemptNumber >= LIGHTHOUSE_MAX_ATTEMPTS_PER_RUN) {
        break;
      }

      console.warn(
        `Lighthouse cold run ${runNumber} attempt ${attemptNumber} produced an incomplete audit; retrying with a fresh Chrome instance.`,
      );
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error(`Lighthouse run ${runNumber} failed.`);
}

let activeChrome;
let staticServer;
let cleanupStarted = false;

async function cleanup() {
  if (cleanupStarted) {
    return;
  }
  cleanupStarted = true;

  const cleanupTasks = [];
  if (activeChrome) {
    cleanupTasks.push(activeChrome.kill());
    activeChrome = undefined;
  }
  if (staticServer) {
    cleanupTasks.push(staticServer.close());
    staticServer = undefined;
  }
  await Promise.allSettled(cleanupTasks);
}

function handleSignal(signal, exitCode) {
  process.once(signal, () => {
    void cleanup().finally(() => process.exit(exitCode));
  });
}

handleSignal('SIGINT', 130);
handleSignal('SIGTERM', 143);

try {
  await rm(reportDirectory, { force: true, recursive: true });
  runPreviewBuild();
  await mkdir(reportDirectory, { recursive: true });
  staticServer = await startStaticServer();

  const runs = [];
  for (let runNumber = 1; runNumber <= LIGHTHOUSE_RUN_COUNT; runNumber += 1) {
    runs.push(await runAudit(staticServer.url, runNumber));
  }

  const medians = calculateCategoryMedians(runs);
  assertLighthouseMedians(medians, LIGHTHOUSE_THRESHOLD);
  await writeFile(
    resolve(reportDirectory, 'summary.json'),
    `${JSON.stringify(
      {
        categories: LIGHTHOUSE_CATEGORIES,
        medians,
        runCount: LIGHTHOUSE_RUN_COUNT,
        runs,
        threshold: LIGHTHOUSE_THRESHOLD,
      },
      null,
      2,
    )}\n`,
  );

  console.log(
    `Lighthouse medians passed at >=${LIGHTHOUSE_THRESHOLD}: ${LIGHTHOUSE_CATEGORIES.map((category) => `${category}=${medians[category]}`).join(', ')}`,
  );
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
} finally {
  await cleanup();
}
