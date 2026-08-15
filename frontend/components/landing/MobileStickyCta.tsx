'use client';

import { useEffect, useState } from 'react';
import { LeadCta } from './LeadCta';

type OpenStateDetail = Readonly<{ open: boolean }>;

function eventOpenState(event: Event) {
  const detail = (event as CustomEvent<OpenStateDetail>).detail;

  return typeof detail?.open === 'boolean' ? detail.open : false;
}

export function MobileStickyCta() {
  const [hasPassedHero, setHasPassedHero] = useState(false);
  const [isCookieOpen, setIsCookieOpen] = useState(false);
  const [isKeyboardOpen, setIsKeyboardOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [isRequestVisible, setIsRequestVisible] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 767px)');
    const updateMobileState = () => setIsMobile(mediaQuery.matches);

    updateMobileState();
    mediaQuery.addEventListener('change', updateMobileState);

    return () => mediaQuery.removeEventListener('change', updateMobileState);
  }, []);

  useEffect(() => {
    const hero =
      document.querySelector<HTMLElement>('[data-section="hero"]') ??
      document.querySelector<HTMLElement>('main > section:first-of-type');
    const request =
      document.getElementById('request') ?? document.getElementById('contact');

    if (!hero) {
      return;
    }

    const IntersectionObserverConstructor = window.IntersectionObserver;

    if (typeof IntersectionObserverConstructor !== 'function') {
      const updatePositionState = () => {
        setHasPassedHero(hero.getBoundingClientRect().bottom < 0);

        if (!request) {
          return;
        }

        const requestRect = request.getBoundingClientRect();
        const visibleHeight = Math.max(
          0,
          Math.min(requestRect.bottom, window.innerHeight) -
            Math.max(requestRect.top, 0),
        );
        setIsRequestVisible(
          requestRect.height > 0 && visibleHeight / requestRect.height >= 0.4,
        );
      };

      updatePositionState();
      window.addEventListener('scroll', updatePositionState, { passive: true });
      window.addEventListener('resize', updatePositionState);

      return () => {
        window.removeEventListener('scroll', updatePositionState);
        window.removeEventListener('resize', updatePositionState);
      };
    }

    const observer = new IntersectionObserverConstructor(
      (entries) => {
        for (const entry of entries) {
          if (entry.target === hero) {
            setHasPassedHero(
              !entry.isIntersecting && entry.boundingClientRect.bottom < 0,
            );
          }

          if (request && entry.target === request) {
            setIsRequestVisible(
              entry.isIntersecting && entry.intersectionRatio >= 0.4,
            );
          }
        }
      },
      { threshold: [0, 0.4] },
    );

    observer.observe(hero);
    if (request) {
      observer.observe(request);
    }

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const visualViewport = window.visualViewport;
    if (!visualViewport) {
      return;
    }

    let baselineHeight = visualViewport.height;
    const updateKeyboardState = () => {
      const currentHeight = visualViewport.height;
      const heightLoss =
        Math.max(window.innerHeight, baselineHeight) - currentHeight;
      const likelyOpen =
        heightLoss >= 150 || currentHeight <= baselineHeight * 0.75;

      setIsKeyboardOpen(likelyOpen);
      if (!likelyOpen) {
        baselineHeight = Math.max(baselineHeight, currentHeight);
      }
    };

    updateKeyboardState();
    visualViewport.addEventListener('resize', updateKeyboardState);

    return () =>
      visualViewport.removeEventListener('resize', updateKeyboardState);
  }, []);

  useEffect(() => {
    const handleMenuState = (event: Event) => {
      setIsMenuOpen(eventOpenState(event));
    };
    const handleCookieState = (event: Event) => {
      setIsCookieOpen(eventOpenState(event));
    };

    window.addEventListener('andrew:menu-state', handleMenuState);
    window.addEventListener('andrew:cookie-state', handleCookieState);

    return () => {
      window.removeEventListener('andrew:menu-state', handleMenuState);
      window.removeEventListener('andrew:cookie-state', handleCookieState);
    };
  }, []);

  const shouldShow =
    isMobile &&
    hasPassedHero &&
    !isRequestVisible &&
    !isKeyboardOpen &&
    !isMenuOpen &&
    !isCookieOpen;

  if (!shouldShow) {
    return null;
  }

  return (
    <div
      className="fixed inset-x-0 bottom-0 z-30 px-4 pt-3 md:hidden"
      data-mobile-sticky-cta
      style={{
        paddingBottom: 'calc(0.75rem + env(safe-area-inset-bottom))',
      }}
    >
      <LeadCta
        aria-label="Оставить заявку — закреплённая кнопка"
        className="mx-auto flex min-h-12 w-full max-w-md items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white shadow-[0_14px_36px_rgba(15,23,42,0.24)] transition-colors duration-200 hover:bg-blue-700"
        sourceSection="mobile_sticky"
      >
        Оставить заявку
      </LeadCta>
    </div>
  );
}
