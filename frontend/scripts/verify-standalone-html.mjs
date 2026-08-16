import { access } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { chromium } from '@playwright/test';

const inputPath = resolve(process.argv[2] ?? 'out/andrew-website-updated.html');
await access(inputPath);

const fileUrl = pathToFileURL(inputPath).href;
const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { height: 844, width: 390 },
});
const page = await context.newPage();
page.setDefaultTimeout(5_000);
const consoleErrors = [];
const failedRequests = [];
const externalRequests = [];

await page.route('**/*', async (route) => {
  const url = route.request().url();
  if (url === fileUrl || url.startsWith('data:')) {
    await route.continue();
    return;
  }

  await route.abort('blockedbyclient');
});

page.on('console', (message) => {
  if (message.type() === 'error') consoleErrors.push(message.text());
});
page.on('pageerror', (error) =>
  consoleErrors.push(error.stack ?? error.message),
);
page.on('request', (request) => {
  const url = request.url();
  if (url !== fileUrl && !url.startsWith('data:')) externalRequests.push(url);
});
page.on('requestfailed', (request) => failedRequests.push(request.url()));

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

try {
  await page.goto(fileUrl, { waitUntil: 'load' });
  await page.waitForTimeout(1_000);

  const heading = page.getByRole('heading', {
    level: 1,
    name: 'Ремонт коммерческого холодильного оборудования',
  });
  await heading.waitFor({ state: 'visible' });
  const headingStyle = await heading.evaluate((element) => {
    const style = getComputedStyle(element);
    return { fontFamily: style.fontFamily, fontSize: style.fontSize };
  });
  assert(headingStyle.fontSize === '32px', 'Standalone CSS was not applied.');
  assert(
    headingStyle.fontFamily.toLowerCase().includes('manrope'),
    'Standalone heading font was not applied.',
  );

  const acceptCookies = page.getByRole('button', { name: 'Принять' });
  if (await acceptCookies.isVisible()) await acceptCookies.click();

  await page.getByRole('button', { name: 'Открыть меню' }).click();
  const dialog = page.getByRole('dialog', { name: 'Мобильная навигация' });
  await dialog.waitFor({ state: 'visible' });
  await dialog.getByRole('link', { name: 'Цены' }).click();
  await page.waitForFunction(() => location.hash === '#pricing');
  assert(!(await dialog.isVisible()), 'Mobile navigation did not close.');

  await page
    .locator('button[aria-controls="equipment-refrigerated-cabinets-details"]')
    .click();
  await page
    .getByText('Точная причина определяется после диагностики')
    .first()
    .waitFor({ state: 'visible' });

  await page
    .getByRole('link', { name: 'Оставить заявку', exact: true })
    .first()
    .click();
  await page.waitForFunction(() => location.hash === '#request');
  assert(
    (await page.evaluate(() => document.activeElement?.id)) ===
      'request-heading',
    'Lead CTA did not focus the request heading.',
  );

  assert(
    await page
      .getByRole('form', { name: 'Форма заявки' })
      .getByRole('button', { name: 'Отправить заявку' })
      .isDisabled(),
    'The file preview must keep backend submission disabled.',
  );

  assert(
    externalRequests.length === 0,
    `Standalone file requested external assets: ${externalRequests.join(', ')}`,
  );
  assert(
    failedRequests.length === 0,
    `Standalone file had failed requests: ${failedRequests.join(', ')}`,
  );
  assert(
    consoleErrors.length === 0,
    `Standalone file logged browser errors: ${consoleErrors.join(' | ')}`,
  );

  process.stdout.write(
    `Standalone file verified through file:// (${inputPath})\n`,
  );
} catch (error) {
  process.stderr.write(
    `${JSON.stringify({ consoleErrors, externalRequests, failedRequests }, null, 2)}\n`,
  );
  throw error;
} finally {
  await context.close();
  await browser.close();
}
