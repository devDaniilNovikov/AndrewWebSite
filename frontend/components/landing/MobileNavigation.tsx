'use client';

import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type MouseEvent,
} from 'react';
import type { NavigationItem } from '../../content/preview-content';
import { LineIcon } from './PreviewPrimitives';

type MobileNavigationProps = Readonly<{
  items: readonly NavigationItem[];
}>;

const focusableSelector = [
  'a[href]:not([aria-disabled="true"])',
  'button:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

const requestHeadingSelector = '[data-lead-heading], h1, h2, h3, h4, h5, h6';
const bodyScrollLockClass = 'andrew-scroll-locked';

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function dispatchRequestEvents(sourceSection: string) {
  window.dispatchEvent(
    new CustomEvent('andrew:lead-context', {
      detail: { intent: 'repair', sourceSection },
    }),
  );
  window.dispatchEvent(
    new CustomEvent('andrew:analytics-request', {
      detail: { name: 'click_request', sourceSection },
    }),
  );
}

export function MobileNavigation({ items }: MobileNavigationProps) {
  const dialogId = useId();
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const closeCompletedRef = useRef(true);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const bodyHadScrollLockRef = useRef<boolean | null>(null);
  const restoreFocusRef = useRef(true);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const lockBodyScroll = useCallback(() => {
    if (bodyHadScrollLockRef.current === null) {
      bodyHadScrollLockRef.current =
        document.body.classList.contains(bodyScrollLockClass);
    }

    document.body.classList.add(bodyScrollLockClass);
  }, []);

  const unlockBodyScroll = useCallback(() => {
    if (bodyHadScrollLockRef.current === null) {
      return;
    }

    if (!bodyHadScrollLockRef.current) {
      document.body.classList.remove(bodyScrollLockClass);
    }
    bodyHadScrollLockRef.current = null;
  }, []);

  const finishClose = useCallback(
    (restoreFocus: boolean) => {
      if (closeCompletedRef.current) {
        return;
      }

      closeCompletedRef.current = true;
      unlockBodyScroll();
      setIsOpen(false);

      if (restoreFocus && triggerRef.current?.isConnected) {
        queueMicrotask(() => triggerRef.current?.focus());
      }
    },
    [unlockBodyScroll],
  );

  const handleNativeClose = useCallback(() => {
    finishClose(restoreFocusRef.current);
  }, [finishClose]);

  const closeDialog = useCallback(
    (restoreFocus = true) => {
      const dialog = dialogRef.current;

      restoreFocusRef.current = restoreFocus;
      finishClose(restoreFocus);

      if (dialog?.open) {
        dialog.close();
      }
    },
    [finishClose],
  );

  const openDialog = () => {
    const dialog = dialogRef.current;

    if (!dialog || dialog.open) {
      return;
    }

    closeCompletedRef.current = false;
    restoreFocusRef.current = true;
    lockBodyScroll();
    dialog.showModal();
    setIsOpen(true);

    queueMicrotask(() => {
      if (dialog.open) {
        closeButtonRef.current?.focus();
      }
    });
  };

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    lockBodyScroll();

    return unlockBodyScroll;
  }, [isOpen, lockBodyScroll, unlockBodyScroll]);

  useEffect(() => {
    window.dispatchEvent(
      new CustomEvent('andrew:menu-state', { detail: { open: isOpen } }),
    );
  }, [isOpen]);

  const navigateToAnchor = useCallback(
    (href: string, focusRequestHeading = false) => {
      const destination = new URL(href, window.location.href);
      const destinationPath = `${destination.pathname}${destination.search}`;
      const currentPath = `${window.location.pathname}${window.location.search}`;

      if (
        destination.origin !== window.location.origin ||
        destinationPath !== currentPath ||
        !destination.hash
      ) {
        window.location.assign(
          `${destination.pathname}${destination.search}${destination.hash}`,
        );
        return;
      }

      let target: HTMLElement | null = null;

      try {
        target = document.getElementById(
          decodeURIComponent(destination.hash.slice(1)),
        );
      } catch {
        return;
      }

      const previousUrl = window.location.href;
      if (window.location.hash !== destination.hash) {
        window.history.pushState(
          null,
          '',
          `${destinationPath}${destination.hash}`,
        );
      }

      window.dispatchEvent(
        new HashChangeEvent('hashchange', {
          newURL: window.location.href,
          oldURL: previousUrl,
        }),
      );

      if (typeof target?.scrollIntoView === 'function') {
        target.scrollIntoView({
          behavior:
            focusRequestHeading && !prefersReducedMotion() ? 'smooth' : 'auto',
          block: 'start',
        });
      }

      if (!focusRequestHeading || !target) {
        return;
      }

      const heading = target.querySelector<HTMLElement>(requestHeadingSelector);
      if (!heading) {
        return;
      }

      if (!heading.hasAttribute('tabindex')) {
        heading.setAttribute('tabindex', '-1');
      }
      heading.focus({ preventScroll: true });
    },
    [],
  );

  const handleNavigationClick = (
    event: MouseEvent<HTMLAnchorElement>,
    href: string,
  ) => {
    event.preventDefault();
    closeDialog(false);
    navigateToAnchor(href);
  };

  const handleRequestClick = (event: MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    dispatchRequestEvents('mobile_menu');
    closeDialog(false);
    navigateToAnchor('#request', true);
  };

  const handleDialogKeyDown = (event: KeyboardEvent<HTMLDialogElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeDialog();
      return;
    }

    if (event.key !== 'Tab') {
      return;
    }

    const focusableElements = Array.from(
      event.currentTarget.querySelectorAll<HTMLElement>(focusableSelector),
    );
    const firstElement = focusableElements.at(0);
    const lastElement = focusableElements.at(-1);

    if (!firstElement || !lastElement) {
      event.preventDefault();
      return;
    }

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
      return;
    }

    if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  };

  const handleOverlayClick = (event: MouseEvent<HTMLDialogElement>) => {
    if (event.target === event.currentTarget) {
      closeDialog();
    }
  };

  return (
    <div className="lg:hidden">
      <button
        aria-controls={dialogId}
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-label="Открыть меню"
        className="grid size-11 place-items-center rounded-md border border-slate-300 bg-white text-navy transition-colors duration-200 hover:border-primary hover:text-primary-ink"
        onClick={openDialog}
        ref={triggerRef}
        type="button"
      >
        <LineIcon name="menu" />
      </button>

      <dialog
        aria-label="Мобильная навигация"
        aria-modal="true"
        className="m-0 ml-auto h-dvh max-h-none w-[min(22rem,calc(100%_-_2rem))] max-w-none border-0 bg-transparent p-0 shadow-2xl backdrop:bg-navy/70 lg:hidden"
        id={dialogId}
        onCancel={(event) => {
          event.preventDefault();
          closeDialog();
        }}
        onClick={handleOverlayClick}
        onClose={handleNativeClose}
        onKeyDown={handleDialogKeyDown}
        ref={dialogRef}
      >
        <div className="flex h-full flex-col overflow-y-auto overscroll-contain bg-white p-6 text-navy">
          <div className="flex items-center justify-between border-b border-slate-200 pb-5">
            <span className="text-sm font-semibold">Навигация</span>
            <button
              aria-label="Закрыть меню"
              className="grid size-11 place-items-center rounded-md border border-slate-300 bg-white transition-colors duration-200 hover:border-primary hover:text-primary-ink"
              onClick={() => closeDialog()}
              ref={closeButtonRef}
              type="button"
            >
              <LineIcon name="close" />
            </button>
          </div>

          <nav aria-label="Разделы страницы" className="mt-6">
            <ul className="grid gap-1">
              {items.map((item) => (
                <li key={item.href}>
                  <a
                    className="flex min-h-11 items-center rounded-md px-3 py-2 text-base font-medium transition-colors duration-200 hover:bg-primary/5 hover:text-primary-ink"
                    href={item.href}
                    onClick={(event) => handleNavigationClick(event, item.href)}
                  >
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div
            aria-label="Контактные каналы"
            className="mt-6 border-t border-slate-200 pt-5"
            role="group"
          >
            <p className="text-sm font-semibold">Контакты</p>
            <ul className="mt-2 grid gap-1 text-sm text-slate-600">
              {(['Телефон', 'Telegram', 'WhatsApp'] as const).map((channel) => (
                <li key={channel}>
                  <span
                    aria-disabled="true"
                    aria-label={`${channel} — канал не опубликован`}
                    className="flex min-h-11 cursor-not-allowed items-center rounded-md px-3 py-2 text-slate-500"
                  >
                    {channel} — не опубликован
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <a
            className="mt-auto inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-[0.9375rem] font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
            href="#request"
            onClick={handleRequestClick}
          >
            Оставить заявку
          </a>
        </div>
      </dialog>
    </div>
  );
}
