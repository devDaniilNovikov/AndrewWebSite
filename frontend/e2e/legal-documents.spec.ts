import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

test('opens both complete legal documents from the footer', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Отклонить аналитику' }).click();
  const footer = page.getByRole('contentinfo');
  await expect(footer.getByText('ИНН: 771549669484')).toBeVisible();

  for (const legalDocument of [
    {
      link: 'Политика конфиденциальности',
      path: '/privacy',
      title: 'Политика в отношении обработки персональных данных',
      lastSection: '14. Контактные данные Оператора',
    },
    {
      link: 'Информация об обработке персональных данных',
      path: '/personal-data',
      title: 'Согласие на обработку персональных данных',
      lastSection: '10. Контакты Оператора',
    },
  ]) {
    await footer
      .getByRole('link', { name: legalDocument.link, exact: true })
      .click();
    await expect(page).toHaveURL(new RegExp(`${legalDocument.path}$`));
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      legalDocument.title,
    );
    await expect(
      page.getByRole('heading', { name: legalDocument.lastSection }),
    ).toBeVisible();
    await expect(page.getByRole('main')).toContainText(
      'Редакция от 06 сентября 2026 года.',
    );
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    const accessibility = await new AxeBuilder({ page })
      .include('main')
      .analyze();
    expect(accessibility.violations).toEqual([]);
    await page
      .getByRole('main')
      .getByRole('link', { name: 'На главную' })
      .click();
    await expect(page).toHaveURL('/');
  }
});
