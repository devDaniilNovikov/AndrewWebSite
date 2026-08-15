import { fireEvent, render, screen, within } from '@testing-library/react';
import { axe } from 'jest-axe';
import { describe, expect, it, vi } from 'vitest';
import { metadata } from '../app/layout';
import { PreviewShell } from '../components/PreviewShell';
import { MediaSlot } from '../components/landing/PreviewPrimitives';

const sectionIds = [
  'equipment',
  'services',
  'works',
  'pricing',
  'process',
  'about',
  'maintenance',
  'reviews',
  'request',
  'contacts',
  'contact',
];

const primaryNavigation = [
  ['Оборудование', '/#equipment'],
  ['Услуги', '/#services'],
  ['Работы', '/#works'],
  ['Цены', '/#pricing'],
  ['О компании', '/#about'],
  ['Контакты', '/#request'],
] as const;

describe('PreviewShell', () => {
  it('renders the corrected full-page composition as a marked preview', () => {
    const { container } = render(<PreviewShell />);

    expect(
      screen.getAllByText(/Предпубликационная версия/iu).length,
    ).toBeGreaterThan(0);
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
        name: 'Заявка — оставить заявку, мобильная версия',
      }),
    ).toHaveAttribute('href', '#request');
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
      screen.getByRole('heading', {
        name: 'Команда мастеров с техническим контролем каждой заявки',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        name: 'Плановое обслуживание коммерческого холода',
      }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Отзывы клиентов' }),
    ).not.toBeInTheDocument();
    expect(container.querySelector('#reviews')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        name: 'Опишите неисправность — уточним задачу и доступность мастера',
      }),
    ).toBeInTheDocument();
  });

  it('keeps every blocked business fact visibly placeholder-only', () => {
    const { container } = render(<PreviewShell />);

    expect(screen.queryByText('Фото будет добавлено')).not.toBeInTheDocument();
    expect(screen.queryByText('Цена уточняется')).not.toBeInTheDocument();
    expect(
      screen.queryByText('Отзыв ожидает подтверждения'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText('Телефон будет добавлен'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText('Название компании уточняется'),
    ).not.toBeInTheDocument();
    expect(screen.getByText('Телефон не опубликован')).toBeInTheDocument();
    expect(
      screen.getAllByText('Данные не опубликованы.').length,
    ).toBeGreaterThan(0);
    expect(container.querySelector('img')).not.toBeInTheDocument();
    expect(
      container.querySelectorAll('[data-media-slot="placeholder"]').length,
    ).toBeGreaterThan(0);
    expect(
      container.querySelector('[data-media-slot="verified"]'),
    ).not.toBeInTheDocument();
    expect(container.querySelector('a[href^="tel:"]')).not.toBeInTheDocument();
    expect(container.querySelector('a[href^="http"]')).not.toBeInTheDocument();
  });

  it('keeps primary navigation within the scrollable landing composition', () => {
    const { container } = render(<PreviewShell />);
    const navigation = screen.getByRole('navigation', {
      name: 'Основная навигация',
    });

    for (const [label, href] of primaryNavigation) {
      expect(
        within(navigation).getByRole('link', { name: label }),
      ).toHaveAttribute('href', href);
      expect(
        container.querySelector(href.replace('/#', '#')),
      ).toBeInTheDocument();
    }
  });

  it('keeps the lead form inert when preview submission is not configured', () => {
    render(<PreviewShell />);

    const form = screen.getByRole('form', {
      name: 'Форма заявки',
    });
    expect(within(form).getByLabelText('Имя')).toBeDisabled();
    expect(within(form).getByLabelText('Телефон')).toBeDisabled();
    expect(within(form).getByLabelText('Опишите неисправность')).toBeDisabled();
    expect(
      within(form).getByRole('button', { name: 'Отправить заявку' }),
    ).toBeDisabled();
    expect(within(form).getByRole('status')).toHaveTextContent(
      'Backend формы не подключён к этому предпросмотру.',
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

  it('does not restore trigger focus when a drawer link controls scrolling', () => {
    render(<PreviewShell />);

    const trigger = screen.getByRole('button', { name: 'Открыть меню' });
    fireEvent.click(trigger);

    const drawer = screen.getByRole('dialog', { name: 'Мобильная навигация' });
    const servicesLink = within(drawer).getByRole('link', { name: 'Услуги' });
    servicesLink.focus();
    fireEvent.click(servicesLink);

    expect(drawer).not.toHaveAttribute('open');
    expect(trigger).not.toHaveFocus();
  });

  it('uses explicit same-page anchor scrolling from the mobile drawer', () => {
    const scrollOverflowStates: string[] = [];
    const scrollIntoView = vi.fn(() => {
      scrollOverflowStates.push(document.body.style.overflow);
    });
    const originalClose = HTMLDialogElement.prototype.close;
    const originalScrollIntoView = HTMLElement.prototype.scrollIntoView;
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {
      configurable: true,
      value(this: HTMLDialogElement) {
        this.removeAttribute('open');
      },
    });
    HTMLElement.prototype.scrollIntoView = scrollIntoView;
    window.history.replaceState(null, '', '/');

    try {
      render(<PreviewShell />);

      const trigger = screen.getByRole('button', { name: 'Открыть меню' });
      fireEvent.click(trigger);

      const drawer = screen.getByRole('dialog', {
        name: 'Мобильная навигация',
      });
      fireEvent.click(within(drawer).getByRole('link', { name: 'О компании' }));

      expect(drawer).not.toHaveAttribute('open');
      expect(window.location.hash).toBe('#about');
      expect(scrollIntoView).toHaveBeenCalledWith({
        behavior: 'auto',
        block: 'start',
      });
      expect(scrollOverflowStates).toEqual(['']);
      expect(document.body).not.toHaveStyle({ overflow: 'hidden' });
    } finally {
      Object.defineProperty(HTMLDialogElement.prototype, 'close', {
        configurable: true,
        value: originalClose,
      });
      HTMLElement.prototype.scrollIntoView = originalScrollIntoView;
      window.history.replaceState(null, '', '/');
    }
  });

  it('routes every request CTA to the request form and preserves maintenance intent', () => {
    render(<PreviewShell />);

    const requestLinks = screen.getAllByRole('link', {
      name: 'Оставить заявку',
    });
    expect(requestLinks.length).toBeGreaterThan(1);
    for (const link of requestLinks) {
      expect(link).toHaveAttribute('href', '#request');
    }
    expect(
      screen.getByRole('link', { name: 'Запросить обслуживание' }),
    ).toHaveAttribute('href', '#request');
    expect(
      screen.getByRole('link', { name: 'Запросить обслуживание' }),
    ).toHaveAttribute('data-lead-intent', 'maintenance');
  });

  it('publishes noindex and nofollow metadata', () => {
    expect(metadata.title).toBe(
      'Ремонт коммерческого холодильного оборудования для бизнеса',
    );
    expect(metadata.robots).toMatchObject({ index: false, follow: false });
    expect(metadata.icons).toEqual({ icon: 'data:,' });
  });

  it('has no WCAG A or AA accessibility violations', async () => {
    const { container } = render(<PreviewShell />);

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  }, 15_000);
});

describe('MediaSlot', () => {
  it('keeps a placeholder by default and accepts a verified local photo later', () => {
    const { rerender } = render(<MediaSlot />);

    expect(screen.getByText('Иллюстрация раздела')).toBeInTheDocument();
    expect(screen.getByRole('img')).toHaveAttribute(
      'data-media-slot',
      'placeholder',
    );
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
    expect(
      screen
        .getByRole('img', { name: 'Мастер у оборудования' })
        .closest('[data-media-slot]'),
    ).toHaveAttribute('data-media-slot', 'verified');
    expect(screen.queryByText('Иллюстрация раздела')).not.toBeInTheDocument();
  });
});
