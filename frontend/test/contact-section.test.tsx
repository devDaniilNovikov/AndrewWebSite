import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ContactSection } from '../components/landing/TrustContactSections';

describe('contact section channels', () => {
  it('shows each published contact value as a link', () => {
    render(<ContactSection />);
    const channels = screen.getByRole('list', { name: 'Способы связи' });

    const phone = within(channels).getByRole('link', {
      name: 'Телефон +7 (903) 237-58-61',
    });
    expect(phone).toHaveAttribute('href', 'tel:+79032375861');
    expect(phone).not.toHaveAttribute('target');

    for (const [name, href] of [
      ['Telegram @AndrewGukovBot_bot', 'https://t.me/AndrewGukovBot_bot'],
      ['WhatsApp +7 (903) 237-58-61', 'https://wa.me/79032375861'],
    ] as const) {
      const link = within(channels).getByRole('link', { name });
      expect(link).toHaveAttribute('href', href);
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    }
  });
});
