import { expect, test, type Page } from '@playwright/test';

const PRODUCT_PAGES = [
  { heading: 'Услуги', path: '/uslugi' },
  {
    heading: 'Ремонт торгового холодильного оборудования',
    path: '/remont-torgovogo-holodilnogo-oborudovaniya',
  },
  {
    heading: 'Ремонт ледогенераторов',
    path: '/remont-ledogeneratorov',
  },
  { heading: 'О компании', path: '/o-kompanii' },
  { heading: 'Выполненные работы', path: '/raboty' },
  { heading: 'Цены', path: '/tseny' },
  { heading: 'Контакты', path: '/kontakty' },
] as const;

const PRIMARY_NAVIGATION = [
  { label: 'Услуги', path: '/uslugi' },
  { label: 'Работы', path: '/raboty' },
  { label: 'Цены', path: '/tseny' },
  { label: 'О компании', path: '/o-kompanii' },
  { label: 'Контакты', path: '/kontakty' },
] as const;

function captureHttpOrigins(page: Page) {
  const origins = new Set<string>();

  page.on('request', (request) => {
    const url = new URL(request.url());

    if (url.protocol === 'http:' || url.protocol === 'https:') {
      origins.add(url.origin);
    }
  });

  return origins;
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

test('serves every provisional product route as unique, isolated placeholder content', async ({
  baseURL,
  page,
}) => {
  expect(baseURL).toBeTruthy();
  const expectedOrigin = new URL(baseURL!).origin;
  const networkOrigins = captureHttpOrigins(page);
  const headings = new Set<string>();
  const titles = new Set<string>();

  for (const productPage of PRODUCT_PAGES) {
    const response = await page.goto(productPage.path);
    await page.waitForLoadState('networkidle');

    expect(response?.status(), productPage.path).toBe(200);
    await expect(page.locator('h1')).toHaveCount(1);
    await expect(
      page.getByRole('heading', {
        level: 1,
        name: productPage.heading,
      }),
    ).toBeVisible();

    const heading = (await page.locator('h1').innerText()).trim();
    const title = (await page.title()).trim();
    expect(title, productPage.path).toContain(productPage.heading);
    headings.add(heading);
    titles.add(title);

    await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
      'content',
      /(?:noindex.*nofollow|nofollow.*noindex)/u,
    );
    await expect(
      page.locator('[data-media-slot="placeholder"]'),
    ).not.toHaveCount(0);
    await expect(page.locator('[data-media-slot="verified"]')).toHaveCount(0);
    await expect(page.locator('img')).toHaveCount(0);
    await expect(page.locator('a[href^="tel:"]')).toHaveCount(0);
    await expect(page.locator('a[href^="http"]')).toHaveCount(0);

    const hrefs = await page
      .locator('a[href]')
      .evaluateAll((links) =>
        links.map((link) => link.getAttribute('href') ?? ''),
      );
    expect(hrefs.length, productPage.path).toBeGreaterThan(0);
    expect(
      hrefs.every((href) => /^(?:#|\/(?!\/))/u.test(href)),
      productPage.path,
    ).toBe(true);

    await expectNoHorizontalOverflow(page);
  }

  expect(headings.size).toBe(PRODUCT_PAGES.length);
  expect(titles.size).toBe(PRODUCT_PAGES.length);
  expect([...networkOrigins]).toEqual([expectedOrigin]);
});

test('keeps product navigation internal and preserves the custom static 404', async ({
  baseURL,
  page,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'One desktop execution covers route transitions and the shared 404.',
  );

  expect(baseURL).toBeTruthy();
  const expectedOrigin = new URL(baseURL!).origin;
  const networkOrigins = captureHttpOrigins(page);
  await page.goto('/uslugi');

  const navigation = page.getByRole('navigation', {
    name: 'Основная навигация',
  });
  for (const item of PRIMARY_NAVIGATION) {
    await expect(
      navigation.getByRole('link', { name: item.label, exact: true }),
    ).toHaveAttribute('href', item.path);
  }

  for (const path of [
    '/remont-torgovogo-holodilnogo-oborudovaniya',
    '/remont-ledogeneratorov',
  ]) {
    await expect(page.locator(`main a[href="${path}"]`)).not.toHaveCount(0);
  }

  await navigation.getByRole('link', { name: 'Контакты', exact: true }).click();
  await expect(page).toHaveURL(new URL('/kontakty', baseURL!).toString());
  await expect(
    page.getByRole('heading', { level: 1, name: 'Контакты' }),
  ).toBeVisible();

  const response = await page.goto('/missing-product-route');
  expect(response?.status()).toBe(404);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Страница не найдена' }),
  ).toBeVisible();
  await expect(
    page.getByRole('link', { name: 'Вернуться на главную' }),
  ).toHaveAttribute('href', '/');
  await expectNoHorizontalOverflow(page);
  expect([...networkOrigins]).toEqual([expectedOrigin]);
});
