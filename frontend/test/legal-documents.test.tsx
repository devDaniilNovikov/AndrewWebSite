import { render, screen, within } from '@testing-library/react';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { LandingFooter } from '../components/landing/TrustContactSections';

describe('published legal documents', () => {
  it('makes both footer documents navigable and displays the confirmed INN', () => {
    render(<LandingFooter />);
    const footer = within(screen.getByRole('contentinfo'));

    expect(
      footer.getByRole('link', { name: 'Политика конфиденциальности' }),
    ).toHaveAttribute('href', '/privacy');
    expect(
      footer.getByRole('link', {
        name: 'Информация об обработке персональных данных',
      }),
    ).toHaveAttribute('href', '/personal-data');
    expect(footer.getByText('ИНН: 771549669484')).toBeVisible();
    expect(footer.queryByText('Реквизиты ИП')).not.toBeInTheDocument();
    expect(
      footer.getByRole('button', { name: 'Настройки cookies' }),
    ).toBeEnabled();
  });

  it('serves extensionless document URLs before matching export directories', async () => {
    const dockerfile = await readFile(
      resolve(process.cwd(), '..', 'Dockerfile'),
      'utf8',
    );

    expect(dockerfile).toContain('try_files $uri.html $uri $uri/ /index.html;');
  });
});
