import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DeepLinkController } from '../components/landing/DeepLinkController';
import { LandingHeader } from '../components/landing/LandingHeader';
import { MobileNavigation } from '../components/landing/MobileNavigation';
import { MobileStickyCta } from '../components/landing/MobileStickyCta';
import type { NavigationItem } from '../content/preview-content';

type MutableVisualViewport = VisualViewport & {
  height: number;
};

const menuItems = [
  { href: '/#equipment', label: 'Оборудование' },
  { href: '/#services', label: 'Услуги' },
] as const satisfies readonly NavigationItem[];

const originalInnerHeight = Object.getOwnPropertyDescriptor(
  window,
  'innerHeight',
);
const originalMatchMedia = Object.getOwnPropertyDescriptor(
  window,
  'matchMedia',
);
const originalVisualViewport = Object.getOwnPropertyDescriptor(
  window,
  'visualViewport',
);

class ControlledIntersectionObserver implements IntersectionObserver {
  static instances: ControlledIntersectionObserver[] = [];

  readonly observed = new Set<Element>();
  readonly root: Element | Document | null = null;
  readonly rootMargin: string;
  readonly thresholds: readonly number[];

  constructor(
    private readonly callback: IntersectionObserverCallback,
    options: IntersectionObserverInit = {},
  ) {
    this.rootMargin = options.rootMargin ?? '0px';
    this.thresholds = Array.isArray(options.threshold)
      ? options.threshold
      : [options.threshold ?? 0];
    ControlledIntersectionObserver.instances.push(this);
  }

  disconnect() {
    this.observed.clear();
  }

  observe(target: Element) {
    this.observed.add(target);
  }

  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }

  trigger(
    target: Element,
    values: Readonly<{
      bottom?: number;
      intersectionRatio?: number;
      isIntersecting?: boolean;
    }>,
  ) {
    const boundingClientRect = {
      ...target.getBoundingClientRect(),
      bottom: values.bottom ?? target.getBoundingClientRect().bottom,
    };
    const entry = {
      boundingClientRect,
      intersectionRatio: values.intersectionRatio ?? 0,
      intersectionRect: boundingClientRect,
      isIntersecting: values.isIntersecting ?? false,
      rootBounds: null,
      target,
      time: 0,
    } as IntersectionObserverEntry;

    this.callback([entry], this);
  }

  unobserve(target: Element) {
    this.observed.delete(target);
  }
}

function installMobileViewport(matches = true) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn((query: string) => ({
      addEventListener: vi.fn(),
      dispatchEvent: vi.fn(() => false),
      matches: query === '(max-width: 767px)' ? matches : false,
      media: query,
      onchange: null,
      removeEventListener: vi.fn(),
    })),
  });

  Object.defineProperty(window, 'innerHeight', {
    configurable: true,
    value: 800,
  });

  const visualViewport = new EventTarget() as MutableVisualViewport;
  Object.defineProperties(visualViewport, {
    height: { configurable: true, value: 800, writable: true },
    offsetLeft: { configurable: true, value: 0 },
    offsetTop: { configurable: true, value: 0 },
    onresize: { configurable: true, value: null, writable: true },
    onscroll: { configurable: true, value: null, writable: true },
    pageLeft: { configurable: true, value: 0 },
    pageTop: { configurable: true, value: 0 },
    scale: { configurable: true, value: 1 },
    width: { configurable: true, value: 390 },
  });
  Object.defineProperty(window, 'visualViewport', {
    configurable: true,
    value: visualViewport,
  });

  return visualViewport;
}

function getObserverFor(target: Element) {
  const observer = ControlledIntersectionObserver.instances.find((candidate) =>
    candidate.observed.has(target),
  );

  if (!observer) {
    throw new Error('Expected an IntersectionObserver for the target.');
  }

  return observer;
}

function listenForDetails<T>(eventName: string) {
  const details: T[] = [];
  const listener = (event: Event) => {
    details.push((event as CustomEvent<T>).detail);
  };
  window.addEventListener(eventName, listener);

  return {
    details,
    stop: () => window.removeEventListener(eventName, listener),
  };
}

beforeEach(() => {
  ControlledIntersectionObserver.instances = [];
  vi.stubGlobal('IntersectionObserver', ControlledIntersectionObserver);
  window.history.replaceState(null, '', '/');
});

afterEach(() => {
  cleanup();
  vi.clearAllTimers();
  vi.useRealTimers();
  vi.unstubAllGlobals();
  window.history.replaceState(null, '', '/');

  if (originalInnerHeight) {
    Object.defineProperty(window, 'innerHeight', originalInnerHeight);
  }
  if (originalMatchMedia) {
    Object.defineProperty(window, 'matchMedia', originalMatchMedia);
  }
  if (originalVisualViewport) {
    Object.defineProperty(window, 'visualViewport', originalVisualViewport);
  } else {
    Reflect.deleteProperty(window, 'visualViewport');
  }
});

describe('LandingHeader', () => {
  it('stays sticky and compacts from 78px to 64px after the hero', () => {
    const { container } = render(
      <>
        <LandingHeader />
        <section data-section="hero" />
      </>,
    );
    const header = screen.getByRole('banner');
    const headerInner = container.querySelector('[data-header-inner]');
    const hero = container.querySelector('[data-section="hero"]');

    expect(header).toHaveClass('sticky');
    expect(header).toHaveAttribute('data-compact', 'false');
    expect(headerInner).toHaveClass('min-h-[78px]');
    expect(
      within(
        screen.getByRole('navigation', { name: 'Основная навигация' }),
      ).getByRole('list'),
    ).toHaveClass('text-sm', 'font-semibold');

    if (!hero) {
      throw new Error('Hero fixture was not rendered.');
    }

    act(() => {
      getObserverFor(hero).trigger(hero, {
        bottom: -1,
        isIntersecting: false,
      });
    });

    expect(header).toHaveAttribute('data-compact', 'true');
    expect(headerInner).toHaveClass('min-h-16');
  });

  it('routes the desktop request CTA to #request and dispatches lead events', () => {
    const leadEvents = listenForDetails<{
      intent: string;
      sourceSection: string;
    }>('andrew:lead-context');
    const analyticsEvents = listenForDetails<{
      name: string;
      sourceSection: string;
    }>('andrew:analytics-request');

    render(<LandingHeader />);
    const requestCta = screen.getByRole('link', {
      name: 'Оставить заявку',
    });

    expect(requestCta).toHaveAttribute('href', '#request');
    fireEvent.click(requestCta);
    expect(leadEvents.details).toContainEqual({
      intent: 'repair',
      sourceSection: 'header',
    });
    expect(analyticsEvents.details).toContainEqual({
      name: 'click_request',
      sourceSection: 'header',
    });

    leadEvents.stop();
    analyticsEvents.stop();
  });
});

describe('MobileNavigation', () => {
  it('preserves modal keyboard behavior and reports menu state', async () => {
    const menuEvents = listenForDetails<{ open: boolean }>('andrew:menu-state');
    render(<MobileNavigation items={menuItems} />);

    const trigger = screen.getByRole('button', { name: 'Открыть меню' });
    trigger.focus();
    fireEvent.click(trigger);

    const dialog = screen.getByRole('dialog', {
      name: 'Мобильная навигация',
    });
    const closeButton = within(dialog).getByRole('button', {
      name: 'Закрыть меню',
    });
    const requestCta = within(dialog).getByRole('link', {
      name: 'Оставить заявку',
    });

    await waitFor(() => expect(closeButton).toHaveFocus());
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(menuEvents.details.at(-1)).toEqual({ open: true });

    fireEvent.keyDown(dialog, { key: 'Tab', shiftKey: true });
    expect(requestCta).toHaveFocus();
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(closeButton).toHaveFocus();

    fireEvent.keyDown(dialog, { key: 'Escape' });
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(menuEvents.details.at(-1)).toEqual({ open: false });
    menuEvents.stop();
  });

  it('renders disabled contact slots without inventing channel URLs', () => {
    render(<MobileNavigation items={menuItems} />);
    fireEvent.click(screen.getByRole('button', { name: 'Открыть меню' }));
    const dialog = screen.getByRole('dialog', {
      name: 'Мобильная навигация',
    });

    for (const channel of ['Телефон', 'Telegram', 'WhatsApp']) {
      const slot = within(dialog).getByLabelText(
        `${channel} — канал не опубликован`,
      );
      expect(slot).toHaveAttribute('aria-disabled', 'true');
      expect(slot).not.toHaveAttribute('role', 'link');
      expect(slot).not.toHaveAttribute('href');
    }
  });

  it('closes on its request anchor and dispatches the shared CTA contract', () => {
    const leadEvents = listenForDetails<{
      intent: string;
      sourceSection: string;
    }>('andrew:lead-context');
    const analyticsEvents = listenForDetails<{
      name: string;
      sourceSection: string;
    }>('andrew:analytics-request');
    render(
      <>
        <MobileNavigation items={menuItems} />
        <section id="request" />
      </>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Открыть меню' }));
    const dialog = screen.getByRole('dialog', {
      name: 'Мобильная навигация',
    });

    fireEvent.click(
      within(dialog).getByRole('link', { name: 'Оставить заявку' }),
    );

    expect(dialog).not.toHaveAttribute('open');
    expect(window.location.hash).toBe('#request');
    expect(leadEvents.details).toContainEqual({
      intent: 'repair',
      sourceSection: 'mobile_menu',
    });
    expect(analyticsEvents.details).toContainEqual({
      name: 'click_request',
      sourceSection: 'mobile_menu',
    });

    leadEvents.stop();
    analyticsEvents.stop();
  });
});

describe('MobileStickyCta', () => {
  it('shows after the hero, hides at 40% form visibility, and uses safe area', () => {
    installMobileViewport();
    const { container } = render(
      <>
        <section data-section="hero" />
        <section id="request" />
        <MobileStickyCta />
      </>,
    );
    const hero = container.querySelector('[data-section="hero"]');
    const request = container.querySelector('#request');

    if (!hero || !request) {
      throw new Error('Sticky CTA fixtures were not rendered.');
    }

    const observer = getObserverFor(hero);
    act(() => {
      observer.trigger(hero, { bottom: -1, isIntersecting: false });
      observer.trigger(request, {
        intersectionRatio: 0.39,
        isIntersecting: true,
      });
    });

    const stickyCta = screen.getByRole('link', {
      name: 'Оставить заявку — закреплённая кнопка',
    });
    expect(stickyCta).toHaveAttribute('href', '#request');
    expect(stickyCta).toHaveClass('min-h-12');
    expect(stickyCta.parentElement).toHaveClass('mobile-sticky-safe-area');
    expect(stickyCta.parentElement).not.toHaveAttribute('style');

    act(() => {
      observer.trigger(request, {
        intersectionRatio: 0.4,
        isIntersecting: true,
      });
    });
    expect(
      screen.queryByRole('link', {
        name: 'Оставить заявку — закреплённая кнопка',
      }),
    ).not.toBeInTheDocument();
  });

  it('hides for menu, cookie banner, and likely on-screen keyboard states', () => {
    const visualViewport = installMobileViewport();
    const { container } = render(
      <>
        <section data-section="hero" />
        <section id="request" />
        <MobileStickyCta />
      </>,
    );
    const hero = container.querySelector('[data-section="hero"]');

    if (!hero) {
      throw new Error('Hero fixture was not rendered.');
    }

    act(() => {
      getObserverFor(hero).trigger(hero, {
        bottom: -1,
        isIntersecting: false,
      });
    });
    expect(
      screen.getByRole('link', {
        name: 'Оставить заявку — закреплённая кнопка',
      }),
    ).toBeInTheDocument();

    for (const eventName of ['andrew:menu-state', 'andrew:cookie-state']) {
      act(() => {
        window.dispatchEvent(
          new CustomEvent(eventName, { detail: { open: true } }),
        );
      });
      expect(
        screen.queryByRole('link', {
          name: 'Оставить заявку — закреплённая кнопка',
        }),
      ).not.toBeInTheDocument();
      act(() => {
        window.dispatchEvent(
          new CustomEvent(eventName, { detail: { open: false } }),
        );
      });
    }

    act(() => {
      visualViewport.height = 520;
      visualViewport.dispatchEvent(new Event('resize'));
    });
    expect(
      screen.queryByRole('link', {
        name: 'Оставить заявку — закреплённая кнопка',
      }),
    ).not.toBeInTheDocument();

    act(() => {
      visualViewport.height = 800;
      visualViewport.dispatchEvent(new Event('resize'));
    });
    expect(
      screen.getByRole('link', {
        name: 'Оставить заявку — закреплённая кнопка',
      }),
    ).toBeInTheDocument();
  });

  it('dispatches lead and analytics context when activated', () => {
    installMobileViewport();
    const leadEvents = listenForDetails<{
      intent: string;
      sourceSection: string;
    }>('andrew:lead-context');
    const analyticsEvents = listenForDetails<{
      name: string;
      sourceSection: string;
    }>('andrew:analytics-request');
    const { container } = render(
      <>
        <section data-section="hero" />
        <MobileStickyCta />
      </>,
    );
    const hero = container.querySelector('[data-section="hero"]');

    if (!hero) {
      throw new Error('Hero fixture was not rendered.');
    }

    act(() => {
      getObserverFor(hero).trigger(hero, {
        bottom: -1,
        isIntersecting: false,
      });
    });
    fireEvent.click(
      screen.getByRole('link', {
        name: 'Оставить заявку — закреплённая кнопка',
      }),
    );

    expect(leadEvents.details).toContainEqual({
      intent: 'repair',
      sourceSection: 'mobile_sticky',
    });
    expect(analyticsEvents.details).toContainEqual({
      name: 'click_request',
      sourceSection: 'mobile_sticky',
    });

    leadEvents.stop();
    analyticsEvents.stop();
  });
});

describe('DeepLinkController', () => {
  it('highlights the initial hash target for 500-700ms', () => {
    vi.useFakeTimers();
    window.history.replaceState(null, '', '/#pricing');
    render(
      <>
        <DeepLinkController />
        <section id="pricing" />
      </>,
    );
    const pricing = document.getElementById('pricing');

    expect(pricing).toHaveAttribute('data-deep-link-highlight', 'true');
    act(() => vi.advanceTimersByTime(500));
    expect(pricing).toHaveAttribute('data-deep-link-highlight', 'fading');
    act(() => vi.advanceTimersByTime(200));
    expect(pricing).not.toHaveAttribute('data-deep-link-highlight');
  });

  it('moves the highlight to a new hash target', () => {
    vi.useFakeTimers();
    render(
      <>
        <DeepLinkController />
        <section id="equipment" />
        <section id="request" />
      </>,
    );

    act(() => {
      window.history.pushState(null, '', '/#equipment');
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(document.getElementById('equipment')).toHaveAttribute(
      'data-deep-link-highlight',
      'true',
    );

    act(() => {
      window.history.pushState(null, '', '/#request');
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(document.getElementById('equipment')).not.toHaveAttribute(
      'data-deep-link-highlight',
    );
    expect(document.getElementById('request')).toHaveAttribute(
      'data-deep-link-highlight',
      'true',
    );
  });
});
