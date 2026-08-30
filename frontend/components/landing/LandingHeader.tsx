'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { navigationItems } from '../../content/preview-content';
import { LeadCta } from './LeadCta';
import { MobileNavigation } from './MobileNavigation';
import { LineIcon } from './PreviewPrimitives';

function useCompactHeader() {
  const [isCompact, setIsCompact] = useState(false);

  useEffect(() => {
    const hero =
      document.querySelector<HTMLElement>('[data-section="hero"]') ??
      document.querySelector<HTMLElement>('main > section:first-of-type');

    if (!hero) {
      return;
    }

    const updateFromPosition = () => {
      setIsCompact(hero.getBoundingClientRect().bottom < 0);
    };

    const IntersectionObserverConstructor = window.IntersectionObserver;

    if (typeof IntersectionObserverConstructor !== 'function') {
      updateFromPosition();
      window.addEventListener('scroll', updateFromPosition, { passive: true });
      window.addEventListener('resize', updateFromPosition);

      return () => {
        window.removeEventListener('scroll', updateFromPosition);
        window.removeEventListener('resize', updateFromPosition);
      };
    }

    const observer = new IntersectionObserverConstructor(([entry]) => {
      if (!entry) {
        return;
      }

      setIsCompact(
        !entry.isIntersecting && entry.boundingClientRect.bottom < 0,
      );
    });
    observer.observe(hero);

    return () => observer.disconnect();
  }, []);

  return isCompact;
}

export function LandingHeader() {
  const isCompact = useCompactHeader();

  return (
    <header
      className={`sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-md transition-[background-color,box-shadow] duration-200 ${
        isCompact
          ? 'shadow-[0_6px_20px_rgba(15,23,42,0.09)]'
          : 'shadow-[0_2px_12px_rgba(15,23,42,0.05)]'
      }`}
      data-compact={isCompact ? 'true' : 'false'}
    >
      <div
        className={`mx-auto flex w-full max-w-[72rem] items-center gap-3 px-5 transition-[min-height] duration-200 sm:gap-4 sm:px-7 lg:px-8 ${
          isCompact ? 'min-h-16' : 'min-h-[78px]'
        }`}
        data-header-inner
      >
        <Link
          aria-label="Сервис холодильного оборудования — на главную"
          className="flex min-h-11 min-w-0 items-center gap-3 text-navy"
          href="/"
        >
          <span className="grid size-10 shrink-0 place-items-center rounded-md bg-primary/10 text-primary-ink">
            <LineIcon name="snowflake" />
          </span>
          <span className="min-w-0">
            <span className="hidden truncate text-sm font-semibold leading-5 min-[360px]:block sm:hidden">
              Сервис холода
            </span>
            <span className="hidden truncate text-sm font-semibold leading-5 sm:block">
              Холодильный сервис
            </span>
            <span className="hidden text-[0.8125rem] leading-[1.125rem] text-slate-500 sm:block">
              Для организаций
            </span>
          </span>
        </Link>

        <nav
          aria-label="Основная навигация"
          className="ml-auto hidden lg:block"
        >
          <ul className="flex items-center gap-4 text-sm font-semibold text-slate-600 xl:gap-6">
            {navigationItems.map((item) => (
              <li key={item.href}>
                <Link
                  className="inline-flex min-h-11 min-w-11 items-center justify-center transition-colors hover:text-primary-ink"
                  href={item.href}
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="ml-auto hidden shrink-0 items-center gap-3 lg:flex">
          <span className="text-right">
            <a
              className="block text-[0.8125rem] font-semibold leading-[1.125rem] text-navy underline hover:text-blue-600"
              href="tel:+79032375861"
            >
              +7 (903) 237-58-61
            </a>
          </span>
          <LeadCta
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-4 py-2 text-[0.9375rem] font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
            sourceSection="header"
          >
            Оставить заявку
          </LeadCta>
        </div>

        <LeadCta
          aria-label="Заявка — оставить заявку, мобильная версия"
          className="ml-auto inline-flex min-h-11 shrink-0 items-center justify-center rounded-md bg-primary px-3 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700 lg:hidden"
          sourceSection="mobile_header"
        >
          Заявка
        </LeadCta>

        <div className="lg:hidden">
          <MobileNavigation items={navigationItems} />
        </div>
      </div>
    </header>
  );
}
