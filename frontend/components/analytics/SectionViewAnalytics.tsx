'use client';

import { useEffect } from 'react';

const observedSections = {
  maintenance: 'view_maintenance',
  pricing: 'view_pricing',
} as const;

export function SectionViewAnalytics() {
  useEffect(() => {
    if (!('IntersectionObserver' in window)) {
      return;
    }

    const seen = new Set<string>();
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const sectionId = entry.target.id;
          if (
            !entry.isIntersecting ||
            seen.has(sectionId) ||
            !(sectionId in observedSections)
          ) {
            continue;
          }

          seen.add(sectionId);
          observer.unobserve(entry.target);
          window.dispatchEvent(
            new CustomEvent('andrew:analytics-request', {
              detail: {
                name: observedSections[
                  sectionId as keyof typeof observedSections
                ],
                sourceSection: sectionId,
              },
            }),
          );
        }
      },
      { threshold: 0.45 },
    );

    for (const sectionId of Object.keys(observedSections)) {
      const section = document.getElementById(sectionId);
      if (section) {
        observer.observe(section);
      }
    }

    return () => observer.disconnect();
  }, []);

  return null;
}
