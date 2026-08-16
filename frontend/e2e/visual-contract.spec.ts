import { expect, test } from '@playwright/test';

const SECTION_ORDER = [
  'hero',
  'benefits',
  'equipment',
  'services',
  'works',
  'repair',
  'pricing',
  'process',
  'about',
  'maintenance',
  'request',
] as const;

test('enforces the behavior-based responsive visual contract', async ({
  page,
}) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  await expect(page.locator('html')).toHaveAttribute('lang', 'ru');
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    'content',
    /(?:noindex.*nofollow|nofollow.*noindex)/,
  );
  expect(
    await page
      .locator('main > section[data-section]')
      .evaluateAll((sections) =>
        sections.map((section) => section.getAttribute('data-section')),
      ),
  ).toEqual([...SECTION_ORDER]);

  const expectedColumns = page.viewportSize()!.width < 640 ? 1 : 2;
  const equipmentColumns = await page
    .locator('[data-grid="equipment"] > [data-grid-item]')
    .evaluateAll((items) => {
      const positions = items.map((item) =>
        Math.round(item.getBoundingClientRect().x),
      );
      return new Set(positions).size;
    });
  expect(equipmentColumns).toBe(
    page.viewportSize()!.width >= 1024 ? 3 : expectedColumns,
  );

  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
  await expect(page.locator('[data-media-slot="placeholder"]')).not.toHaveCount(
    0,
  );
  await expect(page.locator('[data-media-slot="verified"]')).toHaveCount(0);
  await expect(page.locator('img')).toHaveCount(0);
  await expect(page.locator('link[rel="preload"][as="font"]')).toHaveCount(1);
  await expect(page.locator('[data-section="hero"] [data-reveal]')).toHaveCount(
    0,
  );

  const skipLink = page.getByRole('link', { name: 'Перейти к содержимому' });
  await page.keyboard.press('Tab');
  await expect(skipLink).toBeFocused();
  await skipLink.press('Enter');
  await expect(page.locator('main#main-content')).toBeFocused();
});
