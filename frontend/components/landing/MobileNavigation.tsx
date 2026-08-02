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

export function MobileNavigation({ items }: MobileNavigationProps) {
  const dialogId = useId();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const finishClose = useCallback(() => {
    setIsOpen(false);

    if (triggerRef.current?.isConnected) {
      triggerRef.current.focus();
    }
  }, []);

  const closeDialog = useCallback(() => {
    const dialog = dialogRef.current;

    if (dialog?.open) {
      dialog.close();
      return;
    }

    finishClose();
  }, [finishClose]);

  const openDialog = () => {
    const dialog = dialogRef.current;

    if (!dialog || dialog.open) {
      return;
    }

    dialog.showModal();
    setIsOpen(true);
  };

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  const handleDialogKeyDown = (event: KeyboardEvent<HTMLDialogElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeDialog();
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
        className="m-0 ml-auto h-dvh max-h-none w-[min(22rem,calc(100%_-_2rem))] max-w-none border-0 bg-transparent p-0 shadow-2xl backdrop:bg-navy/70 lg:hidden"
        id={dialogId}
        onCancel={(event) => {
          event.preventDefault();
          closeDialog();
        }}
        onClick={handleOverlayClick}
        onClose={finishClose}
        onKeyDown={handleDialogKeyDown}
        ref={dialogRef}
      >
        <div className="flex h-full flex-col bg-white p-6 text-navy">
          <div className="flex items-center justify-between border-b border-slate-200 pb-5">
            <span className="text-sm font-semibold">Навигация</span>
            <button
              aria-label="Закрыть меню"
              className="grid size-11 place-items-center rounded-md border border-slate-300 bg-white transition-colors duration-200 hover:border-primary hover:text-primary-ink"
              onClick={closeDialog}
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
                    onClick={closeDialog}
                  >
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <a
            className="mt-auto inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
            href="#contact"
            onClick={closeDialog}
          >
            Оставить заявку
          </a>
        </div>
      </dialog>
    </div>
  );
}
