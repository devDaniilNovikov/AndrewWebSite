'use client';

import type { MouseEvent, ReactNode } from 'react';

export type LeadIntent = 'maintenance' | 'repair';

type LeadCtaProps = Readonly<{
  'aria-label'?: string;
  children: ReactNode;
  className?: string;
  intent?: LeadIntent;
  sourceSection: string;
}>;

const requestHeadingSelector = '[data-lead-heading], h1, h2, h3, h4, h5, h6';

function prefersReducedMotion() {
  return (
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

export function LeadCta({
  'aria-label': ariaLabel,
  children,
  className = '',
  intent = 'repair',
  sourceSection,
}: LeadCtaProps) {
  const handleClick = (event: MouseEvent<HTMLAnchorElement>) => {
    const context = { intent, sourceSection } as const;

    window.dispatchEvent(
      new CustomEvent('andrew:lead-context', { detail: context }),
    );
    window.dispatchEvent(
      new CustomEvent('andrew:analytics-request', {
        detail: { name: 'click_request', sourceSection },
      }),
    );

    const requestSection = document.getElementById('request');
    if (!requestSection) {
      return;
    }

    event.preventDefault();
    const nextUrl = `${window.location.pathname}${window.location.search}#request`;
    window.history.pushState(null, '', nextUrl);
    requestSection.scrollIntoView({
      behavior: prefersReducedMotion() ? 'auto' : 'smooth',
      block: 'start',
    });

    const heading = requestSection.querySelector<HTMLElement>(
      requestHeadingSelector,
    );
    if (!heading) {
      return;
    }

    if (!heading.hasAttribute('tabindex')) {
      heading.setAttribute('tabindex', '-1');
    }
    heading.focus({ preventScroll: true });
  };

  return (
    <a
      aria-label={ariaLabel}
      className={className}
      data-lead-intent={intent}
      data-lead-source={sourceSection}
      href="#request"
      onClick={handleClick}
    >
      {children}
    </a>
  );
}
