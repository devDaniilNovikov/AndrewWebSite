import { fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  EquipmentSection,
  ServicesSection,
} from '../components/landing/HeroEquipmentSections';
import { LeadCta } from '../components/landing/LeadCta';
import {
  PricingSection,
  ProcessSection,
  RepairCallout,
  WorksSection,
} from '../components/landing/WorkPricingSections';

afterEach(() => {
  window.history.replaceState(null, '', '/');
});

describe('interactive landing sections', () => {
  it('shows the supplied photos in their matching equipment cards', () => {
    const { container } = render(<EquipmentSection />);
    const expectedPhotos = [
      {
        alt: 'Промышленная холодильная камера',
        height: '941',
        id: 'equipment-refrigerated-cabinets',
        src: '/media/verified/equipment-cold-rooms.jpg',
        width: '1672',
      },
      {
        alt: 'Холодильная витрина для выкладки товаров',
        height: '676',
        id: 'equipment-display-cases',
        src: '/media/verified/equipment-display-cases.webp',
        width: '1200',
      },
      {
        alt: 'Профессиональный льдогенератор',
        height: '676',
        id: 'equipment-ice-makers',
        src: '/media/verified/equipment-ice-makers.webp',
        width: '1200',
      },
      {
        alt: 'Профессиональный морозильный ларь',
        height: '941',
        id: 'equipment-chest-freezers',
        src: '/media/verified/equipment-chest-freezers.jpg',
        width: '1672',
      },
      {
        alt: 'Промышленная холодильная система',
        height: '941',
        id: 'equipment-refrigeration-systems',
        src: '/media/verified/equipment-refrigeration-systems.jpg',
        width: '1672',
      },
      {
        alt: 'Профессиональные холодильные шкафы и столы',
        height: '941',
        id: 'equipment-cabinets-and-tables',
        src: '/media/verified/equipment-cabinets-and-tables.jpg',
        width: '1672',
      },
    ];

    for (const expectedPhoto of expectedPhotos) {
      const card = container.querySelector(`#${expectedPhoto.id}`);
      const photo = within(card as HTMLElement).getByRole('img', {
        name: expectedPhoto.alt,
      });

      expect(photo).toHaveClass('object-contain', 'h-auto', 'w-full');
      expect(photo).not.toHaveClass('object-cover');
      expect(photo).toHaveAttribute('src', expectedPhoto.src);
      expect(photo).toHaveAttribute('width', expectedPhoto.width);
      expect(photo).toHaveAttribute('height', expectedPhoto.height);
      expect(photo.closest('[data-media-slot="verified"]')).not.toHaveClass(
        'min-h-40',
      );
    }
  });

  it('keeps equipment cards concise without duplicated detail sections', () => {
    const { container } = render(<EquipmentSection />);
    const card = container.querySelector('#equipment-refrigerated-cabinets');

    expect(card).toBeInTheDocument();
    expect(card).toHaveAccessibleName('Холодильные камеры');
    expect(
      screen.queryByRole('button', { name: 'Частые неисправности' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Типичные симптомы' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Примеры работ' }),
    ).not.toBeInTheDocument();
    expect(container.querySelector('[id$="-details"]')).not.toBeInTheDocument();
    expect(
      screen.getAllByRole('link', { name: /Оставить заявку:/ }),
    ).toHaveLength(6);
  });

  it('sends lead context and analytics before scrolling to and focusing the request heading', () => {
    const originalScrollIntoView = HTMLElement.prototype.scrollIntoView;
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });
    const leadListener = vi.fn();
    const analyticsListener = vi.fn();
    window.addEventListener('andrew:lead-context', leadListener);
    window.addEventListener('andrew:analytics-request', analyticsListener);

    try {
      render(
        <>
          <LeadCta intent="maintenance" sourceSection="maintenance">
            Запросить обслуживание
          </LeadCta>
          <section id="request">
            <h2>Оставить заявку</h2>
          </section>
        </>,
      );

      const cta = screen.getByRole('link', {
        name: 'Запросить обслуживание',
      });
      fireEvent.click(cta);

      expect(cta).toHaveAttribute('href', '#request');
      expect(leadListener).toHaveBeenCalledTimes(1);
      expect((leadListener.mock.calls[0]?.[0] as CustomEvent).detail).toEqual({
        intent: 'maintenance',
        sourceSection: 'maintenance',
      });
      expect(analyticsListener).toHaveBeenCalledTimes(1);
      expect(
        (analyticsListener.mock.calls[0]?.[0] as CustomEvent).detail,
      ).toEqual({
        name: 'click_request',
        sourceSection: 'maintenance',
      });
      expect(scrollIntoView).toHaveBeenCalledWith({
        behavior: 'smooth',
        block: 'start',
      });
      expect(
        screen.getByRole('heading', { name: 'Оставить заявку' }),
      ).toHaveFocus();
      expect(window.location.hash).toBe('#request');
    } finally {
      window.removeEventListener('andrew:lead-context', leadListener);
      window.removeEventListener('andrew:analytics-request', analyticsListener);
      Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
        configurable: true,
        value: originalScrollIntoView,
      });
    }
  });

  it('provides contextual request actions after equipment, cases, and pricing', () => {
    const leadDetails: Array<Readonly<Record<string, string>>> = [];
    const originalScrollIntoView = HTMLElement.prototype.scrollIntoView;
    const listener = (event: Event) => {
      leadDetails.push(
        (event as CustomEvent<Readonly<Record<string, string>>>).detail,
      );
    };
    window.addEventListener('andrew:lead-context', listener);

    try {
      const { container } = render(
        <>
          <EquipmentSection />
          <WorksSection />
          <RepairCallout />
          <PricingSection />
          <section id="request">
            <h2>Форма заявки</h2>
          </section>
        </>,
      );
      Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
        configurable: true,
        value: vi.fn(),
      });

      fireEvent.click(
        within(
          container.querySelector('[data-section="equipment"]') as HTMLElement,
        ).getByRole('link', { name: 'Описать неисправность' }),
      );
      fireEvent.click(
        within(
          container.querySelector('[data-section="repair"]') as HTMLElement,
        ).getByRole('link', { name: 'Оставить заявку' }),
      );
      fireEvent.click(
        within(
          container.querySelector('[data-section="pricing"]') as HTMLElement,
        ).getByRole('link', { name: 'Уточнить стоимость' }),
      );

      expect(leadDetails).toEqual([
        { intent: 'repair', sourceSection: 'equipment' },
        { intent: 'repair', sourceSection: 'works' },
        { intent: 'repair', sourceSection: 'pricing' },
      ]);
    } finally {
      window.removeEventListener('andrew:lead-context', listener);
      Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
        configurable: true,
        value: originalScrollIntoView,
      });
    }
  });

  it('renders supplied case photos without inventing missing case facts', () => {
    const { container } = render(<WorksSection />);

    expect(
      screen.queryByRole('link', { name: /Подробнее/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /Подробнее/i }),
    ).not.toBeInTheDocument();
    expect(container.querySelector('#work-retail-site')).toBeInTheDocument();
    expect(
      container.querySelector('#work-refrigerated-cabinet'),
    ).toBeInTheDocument();
    expect(container.querySelector('#work-ice-maker')).toBeInTheDocument();
    const retailSitePhoto = screen.getByRole('img', {
      name: 'Испаритель внутри промышленного холодильного шкафа',
    });
    expect(retailSitePhoto).toHaveAttribute(
      'src',
      '/media/verified/work-retail-site.jpg',
    );
    expect(retailSitePhoto).toHaveClass(
      'block',
      'h-auto',
      'w-full',
      'object-contain',
    );
    expect(retailSitePhoto).not.toHaveClass('object-cover');
    expect(
      screen.getByRole('heading', {
        name: 'Реставрация испарителя холодильного шкафа/стола',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Утечка фреона в системе.')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Реставрация испарителя путем замены трубок и уголков. Утечки нет, пожизненная гарантия.',
      ),
    ).toBeInTheDocument();
    const refrigeratedCabinetPhoto = screen.getByRole('img', {
      name: 'Холодильная установка с ресиверами и медными трубопроводами',
    });
    expect(refrigeratedCabinetPhoto).toHaveAttribute(
      'src',
      '/media/verified/work-refrigerated-cabinet.jpg',
    );
    expect(refrigeratedCabinetPhoto).toHaveClass(
      'block',
      'h-auto',
      'w-full',
      'object-contain',
    );
    expect(refrigeratedCabinetPhoto).not.toHaveClass('object-cover');
    expect(
      screen.getByRole('heading', {
        name: 'Поиск утечки промышленного агрегата',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Утечка фреона в централи холодильной системы.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Система была опрессована, была найдена утечка в нескольких местах и была устранена.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('img', {
        name: 'Внутренний узел льдогенератора с компрессором и теплообменником',
      }),
    ).toHaveAttribute('src', '/media/verified/work-ice-maker.jpg');
    expect(
      container.querySelectorAll('[data-media-slot="placeholder"]'),
    ).toHaveLength(0);
    expect(
      container.querySelectorAll('[data-media-slot="verified"]'),
    ).toHaveLength(3);
    expect(
      screen.getByRole('heading', {
        name: 'Переход ледогенератора на воздушное охлаждение',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Чрезмерное потребление воды и утечка фреона.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Ледогенератор стабильно работает и меньше потребляет воды.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Данные не опубликованы.'),
    ).not.toBeInTheDocument();
  });

  it('publishes stable section ids and the exact result-control promise', () => {
    const { container } = render(
      <>
        <EquipmentSection />
        <ServicesSection />
        <WorksSection />
        <PricingSection />
        <ProcessSection />
      </>,
    );

    for (const id of ['equipment', 'services', 'works', 'pricing', 'process']) {
      expect(container.querySelector(`#${id}`)).toBeInTheDocument();
    }
    expect(screen.getByText('Контролируем результат')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Остаёмся единым контактом по выполненной работе и гарантийному обращению.',
      ),
    ).toBeInTheDocument();
  });
});
