import { render, screen, within } from '@testing-library/react';
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
});
