import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';

const WCAG_TAGS = [
  'wcag2a',
  'wcag2aa',
  'wcag21a',
  'wcag21aa',
  'wcag22a',
  'wcag22aa',
] as const;

const MAIN_SECTION_HEADINGS = [
  'Оборудование, с которым мы работаем',
  'Услуги',
  'Выполненные работы',
  'Не нашли похожую неисправность?',
  'Ориентиры по стоимости',
  'Как проходит заявка',
  'Команда мастеров',
  'Плановое обслуживание коммерческого холода',
  'Отзывы клиентов',
  'Опишите поломку — свяжемся и уточним детали',
] as const;

const SECTION_ANCHORS = [
  '#equipment',
  '#works',
  '#pricing',
  '#about',
  '#contact',
] as const;

function captureNetworkOrigins(page: Page) {
  const origins = new Set<string>();

  page.on('request', (request: { url: () => string }) => {
    const url = new URL(request.url());

    if (url.protocol === 'http:' || url.protocol === 'https:') {
      origins.add(url.origin);
    }
  });

  return origins;
}

async function expectOnlyPreviewOrigin(
  origins: ReadonlySet<string>,
  baseURL: string | undefined,
) {
  expect(baseURL).toBeTruthy();
  expect([...origins]).toEqual([new URL(baseURL!).origin]);
}

async function expectNoHorizontalOverflow(page: Page) {
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
}

async function expectNoWcagAaViolations(page: Page) {
  const accessibility = await new AxeBuilder({ page })
    .withTags([...WCAG_TAGS])
    .analyze();

  expect(accessibility.violations).toEqual([]);
}

test('serves the complete accessible, network-isolated landing preview', async ({
  baseURL,
  page,
}) => {
  const networkOrigins = captureNetworkOrigins(page);
  const response = await page.goto('/');
  await page.waitForLoadState('networkidle');

  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Ремонт коммерческого холодильного оборудования',
    }),
  ).toBeVisible();
  await expect(
    page.locator('[role="status"]').filter({
      hasText: 'Демонстрационная версия',
    }),
  ).toBeVisible();
  await expect(page.locator('header')).toBeVisible();
  await expect(page.locator('main#main-content')).toBeVisible();
  await expect(page.locator('footer')).toBeVisible();
  await expect(
    page.getByRole('region', { name: 'Преимущества' }),
  ).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    'content',
    /(?:noindex.*nofollow|nofollow.*noindex)/,
  );

  await expect(page.locator('main h2')).toHaveText([...MAIN_SECTION_HEADINGS]);

  for (const anchor of SECTION_ANCHORS) {
    await expect(page.locator(anchor)).toHaveCount(1);
  }

  await expect(page.locator('a[href^="http"]')).toHaveCount(0);
  await expect(page.locator('a[href^="tel:"]')).toHaveCount(0);

  if (page.viewportSize()!.width < 1024) {
    await expect(
      page.getByRole('link', {
        name: 'Заявка — оставить заявку, мобильная версия',
      }),
    ).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Открыть меню' }),
    ).toBeVisible();
    await expect(
      page.getByRole('navigation', { name: 'Основная навигация' }),
    ).not.toBeVisible();
  } else {
    await expect(
      page.getByRole('link', {
        name: 'Заявка — оставить заявку, мобильная версия',
      }),
    ).not.toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Открыть меню' }),
    ).not.toBeVisible();
    await expect(
      page.getByRole('navigation', { name: 'Основная навигация' }),
    ).toBeVisible();
  }

  await expectNoHorizontalOverflow(page);
  await expectNoWcagAaViolations(page);
  await expectOnlyPreviewOrigin(networkOrigins, baseURL);
});

test('keeps navigation and landing CTAs on verified in-page anchors', async ({
  page,
}) => {
  await page.goto('/');

  for (const anchor of SECTION_ANCHORS) {
    await expect(
      page.locator(
        `header nav[aria-label="Основная навигация"] a[href="${anchor}"]`,
      ),
    ).toHaveCount(1);
  }

  const requestLinks = page.getByRole('link', { name: 'Оставить заявку' });
  expect(await requestLinks.count()).toBeGreaterThan(5);

  for (const link of await requestLinks.all()) {
    await expect(link).toHaveAttribute('href', '#contact');
  }

  await page
    .locator('main a[href="#contact"]')
    .filter({ hasText: 'Оставить заявку' })
    .first()
    .click();
  await expect(page).toHaveURL(/#contact$/);
  await expect(page.locator('#contact')).toBeInViewport();
});

test('mobile drawer closes through keyboard, controls, and anchors', async ({
  page,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'mobile-chromium',
    'The drawer interaction contract is exercised at the mobile viewport.',
  );

  await page.goto('/');

  const trigger = page.getByRole('button', { name: 'Открыть меню' });
  const dialog = page.getByRole('dialog', { name: 'Мобильная навигация' });

  await expect(trigger).toBeVisible();
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await trigger.click();

  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveAttribute('open', '');
  await expect(trigger).toHaveAttribute('aria-expanded', 'true');
  await expect(page.locator('body')).toHaveCSS('overflow', 'hidden');

  await page.keyboard.press('Escape');

  await expect(dialog).not.toBeVisible();
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');

  await trigger.click();
  await page.getByRole('button', { name: 'Закрыть меню' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(trigger).toBeFocused();

  await trigger.click();
  await dialog.getByRole('link', { name: 'Цены' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page).toHaveURL(/#pricing$/);
  await expect(page.locator('#pricing')).toBeInViewport();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');
});

test('uses the progressive CSS reveal path below the fold', async ({
  page,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'One Chromium viewport is sufficient for the default reveal contract.',
  );

  await page.goto('/');

  const reveal = page.locator('#about [data-reveal="css"]').first();
  await expect(reveal).toHaveCSS('opacity', '0');
  await expect(reveal).toHaveCSS('transform', 'matrix(1, 0, 0, 1, 0, 16)');

  await reveal.scrollIntoViewIfNeeded();
  await expect(reveal).toHaveCSS('opacity', '1');
  await expect(reveal).toHaveCSS(
    'transform',
    /(?:none|matrix\(1, 0, 0, 1, 0, 0\))/,
  );
});

test('honors the reduced-motion preference', async ({ page }, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'One Chromium viewport is sufficient for the global motion contract.',
  );

  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/');

  expect(
    await page.evaluate(
      () => window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    ),
  ).toBe(true);
  const revealStates = await page
    .locator('main [data-reveal="css"]')
    .evaluateAll((nodes) =>
      nodes.map((node) => ({
        opacity: getComputedStyle(node).opacity,
        transform: getComputedStyle(node).transform,
      })),
    );
  expect(
    revealStates.every(
      ({ opacity, transform }) => opacity === '1' && transform === 'none',
    ),
  ).toBe(true);
  expect(
    await page.evaluate(
      () => getComputedStyle(document.documentElement).scrollBehavior,
    ),
  ).toBe('auto');
});

test('serves the custom static 404 without third-party requests', async ({
  baseURL,
  page,
}) => {
  const networkOrigins = captureNetworkOrigins(page);
  const response = await page.goto('/missing-preview-route');

  expect(response?.status()).toBe(404);
  await expect(
    page.getByRole('heading', { name: 'Страница не найдена' }),
  ).toBeVisible();
  const homeLink = page.getByRole('link', { name: 'Вернуться на главную' });
  await page.keyboard.press('Tab');
  await expect(homeLink).toBeFocused();

  await expectNoHorizontalOverflow(page);
  await expectNoWcagAaViolations(page);
  await expectOnlyPreviewOrigin(networkOrigins, baseURL);
});
