'use client';

import { useEffect, useRef } from 'react';
import type { CSSProperties, ReactNode } from 'react';

type RevealStyle = CSSProperties & Readonly<{ '--reveal-delay': string }>;

type RevealProps = Readonly<{
  children: ReactNode;
  className?: string;
  delay?: number;
}>;

export function Reveal({ children, className, delay = 0 }: RevealProps) {
  const elementRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = elementRef.current;
    const reduceMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;

    if (!element || reduceMotion || !('IntersectionObserver' in window)) {
      return;
    }

    const viewportThreshold = window.innerHeight * 0.9;
    if (element.getBoundingClientRect().top <= viewportThreshold) {
      return;
    }

    element.dataset.revealState = 'pending';
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          element.dataset.revealState = 'visible';
          observer.disconnect();
        }
      },
      { rootMargin: '0px 0px -10% 0px', threshold: 0.05 },
    );
    observer.observe(element);

    return () => observer.disconnect();
  }, []);

  return (
    <div
      className={className}
      data-reveal="css"
      data-reveal-state="visible"
      ref={elementRef}
      style={{ '--reveal-delay': `${delay}s` } as RevealStyle}
    >
      {children}
    </div>
  );
}
