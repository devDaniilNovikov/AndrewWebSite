'use client';

import { useEffect } from 'react';

const highlightClasses = [
  'outline',
  'outline-2',
  'outline-offset-[-2px]',
  'outline-primary/30',
  'transition-[outline-color]',
  'duration-200',
] as const;

const fadingClasses = ['outline-transparent'] as const;

export function DeepLinkController() {
  useEffect(() => {
    let activeTarget: HTMLElement | null = null;
    let cleanupTimer: number | undefined;
    let fadeTimer: number | undefined;
    let isDisposed = false;

    const clearHighlight = () => {
      if (fadeTimer !== undefined) {
        window.clearTimeout(fadeTimer);
      }
      if (cleanupTimer !== undefined) {
        window.clearTimeout(cleanupTimer);
      }
      fadeTimer = undefined;
      cleanupTimer = undefined;

      if (!activeTarget) {
        return;
      }

      activeTarget.classList.remove(...highlightClasses, ...fadingClasses);
      delete activeTarget.dataset.deepLinkHighlight;
      activeTarget = null;
    };

    const highlightHashTarget = () => {
      clearHighlight();

      if (!window.location.hash || window.location.hash === '#') {
        return;
      }

      let targetId: string;
      try {
        targetId = decodeURIComponent(window.location.hash.slice(1));
      } catch {
        return;
      }

      const target = document.getElementById(targetId);
      if (!target) {
        return;
      }

      activeTarget = target;
      target.dataset.deepLinkHighlight = 'true';
      target.classList.add(...highlightClasses);

      fadeTimer = window.setTimeout(() => {
        if (activeTarget !== target) {
          return;
        }

        target.dataset.deepLinkHighlight = 'fading';
        target.classList.remove('outline-primary/30');
        target.classList.add(...fadingClasses);
      }, 500);

      cleanupTimer = window.setTimeout(() => {
        if (activeTarget === target) {
          clearHighlight();
        }
      }, 700);
    };

    const highlightAfterLeadNavigation = () => {
      queueMicrotask(() => {
        if (!isDisposed) {
          highlightHashTarget();
        }
      });
    };

    highlightHashTarget();
    window.addEventListener('hashchange', highlightHashTarget);
    window.addEventListener(
      'andrew:lead-context',
      highlightAfterLeadNavigation,
    );

    return () => {
      isDisposed = true;
      window.removeEventListener('hashchange', highlightHashTarget);
      window.removeEventListener(
        'andrew:lead-context',
        highlightAfterLeadNavigation,
      );
      clearHighlight();
    };
  }, []);

  return null;
}
