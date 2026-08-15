import { act, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SectionViewAnalytics } from '../components/analytics/SectionViewAnalytics';

describe('SectionViewAnalytics', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('requests each pricing and maintenance view event only once', () => {
    let callback: IntersectionObserverCallback | undefined;
    const unobserve = vi.fn();
    const observer = vi.fn(function Observer(
      this: IntersectionObserver,
      nextCallback: IntersectionObserverCallback,
    ) {
      callback = nextCallback;
      return {
        disconnect: vi.fn(),
        observe: vi.fn(),
        root: null,
        rootMargin: '0px',
        takeRecords: () => [],
        thresholds: [0.45],
        unobserve,
      };
    });
    Object.defineProperty(window, 'IntersectionObserver', {
      configurable: true,
      value: observer,
    });

    const events: Array<{ name: string; sourceSection?: string }> = [];
    const capture = (event: Event) => {
      events.push(
        (event as CustomEvent<{ name: string; sourceSection?: string }>).detail,
      );
    };
    window.addEventListener('andrew:analytics-request', capture);

    const { container } = render(
      <>
        <section id="pricing" />
        <section id="maintenance" />
        <SectionViewAnalytics />
      </>,
    );

    const pricing = container.querySelector('#pricing')!;
    const maintenance = container.querySelector('#maintenance')!;
    act(() => {
      callback?.(
        [
          { isIntersecting: true, target: pricing },
          { isIntersecting: true, target: maintenance },
        ] as IntersectionObserverEntry[],
        {} as IntersectionObserver,
      );
      callback?.(
        [
          { isIntersecting: true, target: pricing },
        ] as IntersectionObserverEntry[],
        {} as IntersectionObserver,
      );
    });

    expect(events).toEqual([
      { name: 'view_pricing', sourceSection: 'pricing' },
      { name: 'view_maintenance', sourceSection: 'maintenance' },
    ]);
    expect(unobserve).toHaveBeenCalledTimes(2);

    window.removeEventListener('andrew:analytics-request', capture);
  });
});
