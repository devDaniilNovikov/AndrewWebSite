import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ANALYTICS_CONSENT_KEY,
  CookieConsent,
} from '../components/privacy/CookieConsent';
import { CookieSettingsButton } from '../components/privacy/CookieSettingsButton';

describe('CookieConsent', () => {
  const storage = new Map<string, string>();

  beforeEach(() => {
    storage.clear();
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        clear: () => storage.clear(),
        getItem: (key: string) => storage.get(key) ?? null,
        key: (index: number) => [...storage.keys()][index] ?? null,
        get length() {
          return storage.size;
        },
        removeItem: (key: string) => storage.delete(key),
        setItem: (key: string, value: string) => storage.set(key, value),
      } satisfies Storage,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('blocks analytics requests until the visitor accepts analytics', async () => {
    const forwarded = vi.fn();
    window.addEventListener('andrew:analytics', forwarded);

    render(<CookieConsent />);

    expect(
      await screen.findByRole('region', { name: 'Настройки аналитики' }),
    ).toBeInTheDocument();

    act(() => {
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'click_request', sourceSection: 'hero' },
        }),
      );
    });
    expect(forwarded).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Принять' }));

    expect(window.localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('accepted');
    expect(
      screen.queryByRole('region', { name: 'Настройки аналитики' }),
    ).not.toBeInTheDocument();

    act(() => {
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'click_request', sourceSection: 'pricing' },
        }),
      );
    });

    expect(forwarded).toHaveBeenCalledTimes(1);
    expect(forwarded.mock.calls[0]?.[0]).toMatchObject({
      detail: { name: 'click_request', sourceSection: 'pricing' },
    });

    window.removeEventListener('andrew:analytics', forwarded);
  });

  it('rejects analytics events with unknown names, unsafe sources, or extra fields', async () => {
    const forwarded = vi.fn();
    window.addEventListener('andrew:analytics', forwarded);

    render(<CookieConsent />);
    fireEvent.click(await screen.findByRole('button', { name: 'Принять' }));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: {
            name: 'click_request',
            phone: '+79991234567',
            sourceSection: 'hero',
          },
        }),
      );
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'unknown_event', sourceSection: 'hero' },
        }),
      );
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'click_request', sourceSection: 'Имя клиента' },
        }),
      );
    });

    expect(forwarded).not.toHaveBeenCalled();
    window.removeEventListener('andrew:analytics', forwarded);
  });

  it('stops forwarding analytics after consent is rejected in another tab', async () => {
    const forwarded = vi.fn();
    window.addEventListener('andrew:analytics', forwarded);

    render(<CookieConsent />);
    fireEvent.click(await screen.findByRole('button', { name: 'Принять' }));

    act(() => {
      window.localStorage.setItem(ANALYTICS_CONSENT_KEY, 'rejected');
      window.dispatchEvent(
        new StorageEvent('storage', {
          key: ANALYTICS_CONSENT_KEY,
          newValue: 'rejected',
          oldValue: 'accepted',
        }),
      );
    });

    act(() => {
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'click_request', sourceSection: 'pricing' },
        }),
      );
    });

    expect(forwarded).not.toHaveBeenCalled();
    window.removeEventListener('andrew:analytics', forwarded);
  });

  it('honors an in-memory rejection when replacing stored acceptance fails', async () => {
    window.localStorage.setItem(ANALYTICS_CONSENT_KEY, 'accepted');
    vi.spyOn(window.localStorage, 'setItem').mockImplementation(() => {
      throw new Error('storage unavailable');
    });
    const forwarded = vi.fn();
    window.addEventListener('andrew:analytics', forwarded);

    render(<CookieConsent />);
    act(() => window.dispatchEvent(new Event('andrew:cookie-settings')));
    fireEvent.click(
      await screen.findByRole('button', { name: 'Отклонить аналитику' }),
    );

    act(() => {
      window.dispatchEvent(
        new CustomEvent('andrew:analytics-request', {
          detail: { name: 'click_request', sourceSection: 'pricing' },
        }),
      );
    });

    expect(forwarded).not.toHaveBeenCalled();
    window.removeEventListener('andrew:analytics', forwarded);
  });

  it('remembers a rejection and reopens only through cookie settings', async () => {
    const cookieStates: boolean[] = [];
    const captureState = (event: Event) => {
      cookieStates.push((event as CustomEvent<{ open: boolean }>).detail.open);
    };
    window.addEventListener('andrew:cookie-state', captureState);

    render(<CookieConsent />);
    fireEvent.click(
      await screen.findByRole('button', { name: 'Отклонить аналитику' }),
    );

    expect(window.localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('rejected');

    act(() => {
      window.dispatchEvent(new Event('andrew:cookie-settings'));
    });

    expect(
      await screen.findByRole('region', { name: 'Настройки аналитики' }),
    ).toBeInTheDocument();
    await waitFor(() => expect(cookieStates).toContain(true));

    window.removeEventListener('andrew:cookie-state', captureState);
  });

  it('exposes an explicit footer control for reopening cookie settings', () => {
    const openSettings = vi.fn();
    window.addEventListener('andrew:cookie-settings', openSettings);

    render(<CookieSettingsButton />);
    fireEvent.click(screen.getByRole('button', { name: 'Настройки cookies' }));

    expect(openSettings).toHaveBeenCalledTimes(1);
    window.removeEventListener('andrew:cookie-settings', openSettings);
  });
});
