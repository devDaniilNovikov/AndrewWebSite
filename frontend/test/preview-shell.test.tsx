import { fireEvent, render, screen, within } from '@testing-library/react';
import { axe } from 'jest-axe';
import { describe, expect, it } from 'vitest';
import { metadata } from '../app/layout';
import { PreviewShell } from '../components/PreviewShell';
import { MediaSlot } from '../components/landing/PreviewPrimitives';

const sectionIds = ['equipment', 'works', 'pricing', 'about', 'contact'];

describe('PreviewShell', () => {
  it('renders the corrected full-page composition as a marked preview', () => {
    const { container } = render(<PreviewShell />);

    expect(screen.getByText('Демонстрационная версия')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Ремонт коммерческого холодильного оборудования',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: 'Преимущества' }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole('banner')).getByRole('link', {
        name: 'Оставить заявку — мобильная версия',
      }),
    ).toHaveAttribute('href', '#contact');
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Перейти к содержимому' }),
    ).toHaveAttribute('href', '#main-content');
    expect(container.querySelectorAll('h1')).toHaveLength(1);

    for (const sectionId of sectionIds) {
      expect(container.querySelector(`#${sectionId}`)).toBeInTheDocument();
    }

    expect(
      screen.getByRole('heading', {
        name: 'Оборудование, с которым мы работаем',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Услуги' })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Выполненные работы' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Ориентиры по стоимости' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Как проходит заявка' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Команда мастеров' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        name: 'Плановое обслуживание коммерческого холода',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Отзывы клиентов' }),
    ).toBeInTheDocument();
  });

  it('keeps every blocked business fact visibly placeholder-only', () => {
    const { container } = render(<PreviewShell />);

    expect(screen.getAllByText('Фото будет добавлено').length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText('Цена уточняется').length).toBeGreaterThan(0);
    expect(
      screen.getAllByText('Отзыв ожидает подтверждения').length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText('Телефон будет добавлен').length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText('Название компании уточняется').length,
    ).toBeGreaterThan(0);
    expect(container.querySelector('img')).not.toBeInTheDocument();
    expect(container.querySelector('a[href^="tel:"]')).not.toBeInTheDocument();
    expect(container.querySelector('a[href^="http"]')).not.toBeInTheDocument();
  });

  it('renders a labelled, inert form shell for the later F5 integration', () => {
    render(<PreviewShell />);

    const form = screen.getByRole('form', {
      name: 'Форма заявки недоступна в демонстрации',
    });
    expect(within(form).getByLabelText('Имя')).toBeDisabled();
    expect(within(form).getByLabelText('Телефон')).toBeDisabled();
    expect(within(form).getByLabelText('Комментарий')).toBeDisabled();
    expect(
      within(form).getByRole('button', { name: 'Отправить заявку' }),
    ).toBeDisabled();
    expect(within(form).getByRole('status')).toHaveTextContent(
      'Отправка данных будет подключена на этапе F5',
    );
  });

  it('opens and closes the mobile drawer with complete focus restoration', () => {
    render(<PreviewShell />);

    const trigger = screen.getByRole('button', { name: 'Открыть меню' });
    trigger.focus();
    fireEvent.click(trigger);

    const drawer = screen.getByRole('dialog', { name: 'Мобильная навигация' });
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    expect(drawer).toHaveAttribute('open');
    expect(document.body).toHaveStyle({ overflow: 'hidden' });

    fireEvent.keyDown(drawer, { key: 'Escape' });

    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(document.body).not.toHaveStyle({ overflow: 'hidden' });
    expect(trigger).toHaveFocus();
  });

  it('routes every request CTA to the contact shell', () => {
    render(<PreviewShell />);

    const requestLinks = screen.getAllByRole('link', {
      name: 'Оставить заявку',
    });
    expect(requestLinks.length).toBeGreaterThan(1);
    for (const link of requestLinks) {
      expect(link).toHaveAttribute('href', '#contact');
    }
    expect(
      screen.getByRole('link', { name: 'Обсудить обслуживание объекта' }),
    ).toHaveAttribute('href', '#contact');
  });

  it('publishes noindex and nofollow metadata', () => {
    expect(metadata.title).toBe(
      'Ремонт коммерческого холодильного оборудования — демонстрация',
    );
    expect(metadata.robots).toMatchObject({ index: false, follow: false });
  });

  it('has no WCAG A or AA accessibility violations', async () => {
    const { container } = render(<PreviewShell />);

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  });
});

describe('MediaSlot', () => {
  it('keeps a placeholder by default and accepts a verified local photo later', () => {
    const { rerender } = render(<MediaSlot />);

    expect(screen.getByText('Фото будет добавлено')).toBeInTheDocument();
    expect(
      screen.queryByRole('img', { name: 'Мастер у оборудования' }),
    ).not.toBeInTheDocument();

    rerender(
      <MediaSlot
        photo={{
          alt: 'Мастер у оборудования',
          src: '/media/verified/hero.webp',
        }}
      />,
    );

    expect(
      screen.getByRole('img', { name: 'Мастер у оборудования' }),
    ).toHaveAttribute('src', '/media/verified/hero.webp');
    expect(screen.queryByText('Фото будет добавлено')).not.toBeInTheDocument();
  });
});
