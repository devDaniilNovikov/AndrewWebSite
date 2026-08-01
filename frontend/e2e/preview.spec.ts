import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';

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

test('serves an accessible, network-isolated preview export', async ({
  baseURL,
  page,
}) => {
  const networkOrigins = captureNetworkOrigins(page);
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', { name: 'Сайт готовится к наполнению' }),
  ).toBeVisible();
  await expect(page.getByText('Демонстрационная версия')).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    'content',
    /(?:noindex.*nofollow|nofollow.*noindex)/,
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);

  const accessibility = await new AxeBuilder({ page })
    .withTags([
      'wcag2a',
      'wcag2aa',
      'wcag21a',
      'wcag21aa',
      'wcag22a',
      'wcag22aa',
    ])
    .analyze();
  expect(accessibility.violations).toEqual([]);
  expect([...networkOrigins]).toEqual([new URL(baseURL!).origin]);
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

  const accessibility = await new AxeBuilder({ page })
    .withTags([
      'wcag2a',
      'wcag2aa',
      'wcag21a',
      'wcag21aa',
      'wcag22a',
      'wcag22aa',
    ])
    .analyze();
  expect(accessibility.violations).toEqual([]);
  expect([...networkOrigins]).toEqual([new URL(baseURL!).origin]);
});
