'use client';

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from 'react';
import { parseLeadContext } from '../../lib/leads/context';

export const ANALYTICS_CONSENT_KEY = 'andrew.analytics-consent.v1';
const ANALYTICS_CONSENT_CHANGE_EVENT = 'andrew:analytics-consent-change';

type AnalyticsConsent = 'accepted' | 'rejected' | null;

const ANALYTICS_EVENT_NAMES = new Set([
  'click_request',
  'click_phone',
  'click_telegram',
  'click_whatsapp',
  'expand_equipment',
  'expand_case',
  'view_pricing',
  'view_maintenance',
  'form_start',
  'form_validation_error',
  'form_submit',
  'form_success',
  'form_error',
  'form_retry',
]);

type AnalyticsRequest = Readonly<{
  name: string;
  sourceSection: string;
}>;

function readStoredConsent(): AnalyticsConsent {
  try {
    const stored = window.localStorage.getItem(ANALYTICS_CONSENT_KEY);
    return stored === 'accepted' || stored === 'rejected' ? stored : null;
  } catch {
    return null;
  }
}

function storeConsent(consent: Exclude<AnalyticsConsent, null>): boolean {
  let persisted = false;
  try {
    window.localStorage.setItem(ANALYTICS_CONSENT_KEY, consent);
    persisted = true;
  } catch {
    // Consent still applies for this page view when storage is unavailable.
  }
  window.dispatchEvent(new Event(ANALYTICS_CONSENT_CHANGE_EVENT));
  return persisted;
}

function subscribeToConsent(onStoreChange: () => void) {
  window.addEventListener('storage', onStoreChange);
  window.addEventListener(ANALYTICS_CONSENT_CHANGE_EVENT, onStoreChange);
  return () => {
    window.removeEventListener('storage', onStoreChange);
    window.removeEventListener(ANALYTICS_CONSENT_CHANGE_EVENT, onStoreChange);
  };
}

const readServerConsent = (): AnalyticsConsent => null;

function parseAnalyticsRequest(value: unknown): AnalyticsRequest | null {
  if (typeof value !== 'object' || value === null || !('name' in value)) {
    return null;
  }

  const detail = value as Record<string, unknown>;
  if (
    Object.keys(detail).some(
      (key) => key !== 'name' && key !== 'sourceSection',
    ) ||
    typeof detail.name !== 'string' ||
    !ANALYTICS_EVENT_NAMES.has(detail.name) ||
    typeof detail.sourceSection !== 'string'
  ) {
    return null;
  }

  const context = parseLeadContext({
    intent: 'repair',
    sourceSection: detail.sourceSection,
  });
  if (context === null) {
    return null;
  }

  return Object.freeze({
    name: detail.name,
    sourceSection: context.sourceSection,
  });
}

function publishCookieState(open: boolean) {
  window.dispatchEvent(
    new CustomEvent('andrew:cookie-state', { detail: { open } }),
  );
}

export function CookieConsent() {
  const storedConsent = useSyncExternalStore(
    subscribeToConsent,
    readStoredConsent,
    readServerConsent,
  );
  const [sessionConsent, setSessionConsent] = useState<
    Exclude<AnalyticsConsent, null> | undefined
  >();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const effectiveConsent = sessionConsent ?? storedConsent ?? null;
  const isOpen = settingsOpen || effectiveConsent === null;

  const openSettings = useCallback((focusHeading = false) => {
    setSettingsOpen(true);
    if (focusHeading) {
      window.requestAnimationFrame(() => headingRef.current?.focus());
    }
  }, []);

  useEffect(() => {
    const handleSettings = () => openSettings(true);
    const handleAnalyticsRequest = (event: Event) => {
      const request = parseAnalyticsRequest(
        (event as CustomEvent<unknown>).detail,
      );
      if (effectiveConsent !== 'accepted' || request === null) {
        return;
      }

      window.dispatchEvent(
        new CustomEvent('andrew:analytics', { detail: request }),
      );
    };

    window.addEventListener('andrew:cookie-settings', handleSettings);
    window.addEventListener(
      'andrew:analytics-request',
      handleAnalyticsRequest as EventListener,
    );

    return () => {
      window.removeEventListener('andrew:cookie-settings', handleSettings);
      window.removeEventListener(
        'andrew:analytics-request',
        handleAnalyticsRequest as EventListener,
      );
    };
  }, [effectiveConsent, openSettings]);

  useEffect(() => {
    const clearSessionFallback = (event: StorageEvent) => {
      if (event.key === null || event.key === ANALYTICS_CONSENT_KEY) {
        setSessionConsent(undefined);
      }
    };

    window.addEventListener('storage', clearSessionFallback);
    return () => window.removeEventListener('storage', clearSessionFallback);
  }, []);

  useEffect(() => {
    publishCookieState(isOpen);
  }, [isOpen]);

  const chooseConsent = (consent: Exclude<AnalyticsConsent, null>) => {
    const persisted = storeConsent(consent);
    setSessionConsent(persisted ? undefined : consent);
    setSettingsOpen(false);
  };

  if (!isOpen) {
    return null;
  }

  return (
    <aside
      aria-label="Настройки аналитики"
      className="fixed inset-x-3 bottom-[max(0.75rem,env(safe-area-inset-bottom))] z-[70] mx-auto max-w-3xl rounded-xl border border-slate-200 bg-white p-4 text-navy shadow-[0_24px_70px_rgba(11,18,32,0.24)] sm:inset-x-6 sm:flex sm:items-center sm:gap-6 sm:p-5"
      data-cookie-consent
      role="region"
    >
      <div className="min-w-0 flex-1">
        <h2
          className="font-heading text-lg font-bold"
          ref={headingRef}
          tabIndex={-1}
        >
          Аналитические cookies
        </h2>
        <p className="mt-2 text-sm leading-5 text-slate-600">
          Мы используем аналитические cookies, чтобы понимать эффективность
          сайта.
        </p>
        <a
          className="mt-2 inline-flex min-h-11 items-center text-sm font-semibold text-primary-ink underline decoration-primary/30 underline-offset-4 hover:decoration-primary"
          href="#privacy-policy"
        >
          Политика конфиденциальности
        </a>
      </div>
      <div className="mt-4 grid shrink-0 gap-2 sm:mt-0 sm:grid-cols-2">
        <button
          className="min-h-12 rounded-md bg-primary px-5 py-3 text-[0.9375rem] font-semibold text-white transition-colors duration-150 hover:bg-blue-700"
          onClick={() => chooseConsent('accepted')}
          type="button"
        >
          Принять
        </button>
        <button
          className="min-h-12 rounded-md border border-slate-300 bg-white px-5 py-3 text-[0.9375rem] font-semibold text-navy transition-colors duration-150 hover:border-primary hover:text-primary-ink"
          onClick={() => chooseConsent('rejected')}
          type="button"
        >
          Отклонить аналитику
        </button>
      </div>
    </aside>
  );
}
