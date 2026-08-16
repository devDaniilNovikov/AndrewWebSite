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
  it('expands an equipment card through an accessible controlled button', () => {
    const analyticsListener = vi.fn();
    window.addEventListener('andrew:analytics-request', analyticsListener, {
      once: true,
    });
    const { container } = render(<EquipmentSection />);
    const card = container.querySelector('#equipment-refrigerated-cabinets');

    expect(card).toBeInTheDocument();
    expect(card).toHaveAccessibleName('Холодильные шкафы');

    const toggle = within(card as HTMLElement).getByRole('button', {
      name: 'Частые неисправности',
    });
    const detailsId = 'equipment-refrigerated-cabinets-details';
    const details = container.querySelector(`#${detailsId}`);

    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAttribute('aria-controls', detailsId);
    expect(toggle).toHaveClass('min-h-11');
    expect(details).toHaveAttribute('aria-hidden', 'true');
    expect(details).toHaveClass(
      'duration-200',
      'motion-reduce:transition-none',
    );

    fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveTextContent('Свернуть');
    expect(details).toHaveAttribute('aria-hidden', 'false');
    expect(analyticsListener).toHaveBeenCalledTimes(1);
    expect(
      (analyticsListener.mock.calls[0]?.[0] as CustomEvent).detail,
    ).toEqual({
      name: 'expand_equipment',
      sourceSection: 'equipment',
    });
    expect(
      within(details as HTMLElement).getByText(
        'Точная причина определяется после диагностики',
      ),
    ).toBeInTheDocument();
    expect(
      within(details as HTMLElement).getByRole('heading', {
        name: 'Примеры работ',
      }),
    ).toBeInTheDocument();

    fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveTextContent('Частые неисправности');
    expect(details).toHaveAttribute('aria-hidden', 'true');
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

  it('keeps placeholder cases honest and gives every card a stable id', () => {
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
