import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import { axe } from 'jest-axe';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LeadForm } from '../components/leads/LeadForm';

const SUCCESS_MESSAGE =
  'Заявка принята. Свяжемся с вами в рабочее время после проверки доступности мастера.';
const FIRST_ERROR_MESSAGE =
  'Не удалось отправить заявку. Проверьте соединение и попробуйте ещё раз.';
const SECOND_ERROR_MESSAGE =
  'Заявка не отправлена. Попробуйте ещё раз или позвоните нам.';
const OFFLINE_MESSAGE = 'Нет соединения. Введённые данные сохранены.';

const enableLoopbackPreview = () => {
  vi.stubEnv('NEXT_PUBLIC_BUILD_MODE', 'preview');
  vi.stubEnv('NEXT_PUBLIC_PREVIEW_API_ORIGIN', 'http://127.0.0.1:8080');
};

const setOnline = (online: boolean) => {
  Object.defineProperty(window.navigator, 'onLine', {
    configurable: true,
    value: online,
  });
};

const fillValidLead = () => {
  fireEvent.change(screen.getByLabelText('Имя'), {
    target: { value: 'Тест' },
  });
  fireEvent.change(screen.getByLabelText('Телефон'), {
    target: { value: '89991234567' },
  });
  fireEvent.change(screen.getByLabelText('Опишите неисправность'), {
    target: { value: 'Синтетическая заявка' },
  });
};

const dispatchLeadContext = (
  sourceSection: string,
  intent: 'repair' | 'maintenance',
) => {
  window.dispatchEvent(
    new CustomEvent('andrew:lead-context', {
      detail: { intent, sourceSection },
    }),
  );
};

describe('LeadForm', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
    setOnline(true);
    vi.stubEnv('NEXT_PUBLIC_BUILD_MODE', 'preview');
    vi.stubEnv('NEXT_PUBLIC_PREVIEW_API_ORIGIN', '');
  });

  afterEach(() => {
    setOnline(true);
    vi.useRealTimers();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('keeps a hosted preview inert and exposes only the approved visible fields', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const { container } = render(<LeadForm />);
    const form = screen.getByRole('form', { name: 'Форма заявки' });

    expect(within(form).getByLabelText('Имя')).toBeDisabled();
    expect(within(form).getByLabelText('Телефон')).toBeDisabled();
    expect(within(form).getByLabelText('Опишите неисправность')).toBeDisabled();
    expect(within(form).queryByRole('radio')).not.toBeInTheDocument();
    expect(within(form).queryByRole('checkbox')).not.toBeInTheDocument();
    expect(form).toHaveTextContent(
      'Нажимая «Отправить заявку», вы соглашаетесь на обработку персональных данных и принимаете политику конфиденциальности.',
    );
    expect(
      within(form).getByRole('link', {
        name: 'обработку персональных данных',
      }),
    ).toHaveAttribute('href', '#personal-data');
    expect(
      within(form).getByRole('link', {
        name: 'политику конфиденциальности',
      }),
    ).toHaveAttribute('href', '#privacy-policy');
    expect(
      within(form).getByRole('button', { name: 'Отправить заявку' }),
    ).toBeDisabled();
    expect(within(form).getByRole('status')).toHaveTextContent(
      'Backend формы не подключён к этому предпросмотру.',
    );

    const honeypot = container.querySelector<HTMLInputElement>(
      'input[name="website"]',
    );
    expect(honeypot).toHaveAttribute('aria-hidden', 'true');
    expect(honeypot).toHaveAttribute('autocomplete', 'off');
    expect(honeypot).toHaveAttribute('tabindex', '-1');
    expect(honeypot).toBeDisabled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows the exact field errors and focuses the first invalid control', () => {
    enableLoopbackPreview();
    const analytics: unknown[] = [];
    window.addEventListener('andrew:analytics-request', (event) => {
      analytics.push((event as CustomEvent).detail);
    });
    render(<LeadForm />);

    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    const summary = screen.getByRole('alert', {
      name: 'Проверьте поля формы',
    });
    expect(summary).toHaveTextContent('Исправьте отмеченные поля.');
    const name = screen.getByLabelText('Имя');
    expect(name).toHaveFocus();
    expect(name).toHaveAccessibleDescription(
      'Укажите имя длиной от 2 до 50 символов',
    );
    expect(screen.getByLabelText('Телефон')).toHaveAccessibleDescription(
      expect.stringContaining('Введите номер телефона'),
    );
    expect(analytics).toContainEqual({
      name: 'form_validation_error',
      sourceSection: 'request',
    });

    fireEvent.change(name, { target: { value: 'x'.repeat(51) } });
    fireEvent.change(screen.getByLabelText('Телефон'), {
      target: { value: '69991234567' },
    });
    fireEvent.change(screen.getByLabelText('Опишите неисправность'), {
      target: { value: 'коротко' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    expect(name).toHaveAccessibleDescription(
      'Укажите имя длиной от 2 до 50 символов',
    );
    expect(screen.getByLabelText('Телефон')).toHaveAccessibleDescription(
      expect.stringContaining(
        'Введите номер телефона в формате +7 (999) 123-45-67',
      ),
    );
    expect(
      screen.getByLabelText('Опишите неисправность'),
    ).toHaveAccessibleDescription(
      expect.stringContaining(
        'Опишите неисправность минимум в 10 символах или оставьте поле пустым',
      ),
    );
  });

  it('masks the phone and submits safe in-memory context without unknown API fields', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 202,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);

    act(() => dispatchLeadContext('maintenance', 'maintenance'));
    fillValidLead();
    expect(screen.getByLabelText('Телефон')).toHaveValue('+7 (999) 123-45-67');
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    const payload = JSON.parse(String(request.body)) as Record<string, unknown>;
    expect(Object.keys(payload).sort()).toEqual(
      [
        'comment',
        'consent',
        'intent',
        'name',
        'phone',
        'requestId',
        'sourcePath',
      ].sort(),
    );
    expect(payload).toMatchObject({
      consent: true,
      intent: 'maintenance',
      phone: '+7 (999) 123-45-67',
      sourcePath: '/',
    });
    expect(payload).not.toHaveProperty('sourceSection');
  });

  it('ignores unsafe lead context and keeps the default repair context', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 202,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);

    window.dispatchEvent(
      new CustomEvent('andrew:lead-context', {
        detail: {
          intent: 'other',
          sourceSection: 'phone=89991234567',
        },
      }),
    );
    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(String(request.body))).toMatchObject({
      intent: 'repair',
    });
  });

  it('shows a stable sending spinner, blocks duplicates, and focuses exact success', async () => {
    enableLoopbackPreview();
    let resolveRequest!: (response: {
      headers: Headers;
      status: number;
    }) => void;
    const fetchMock = vi.fn(
      () =>
        new Promise<{ headers: Headers; status: number }>((resolve) => {
          resolveRequest = resolve;
        }),
    );
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);
    fillValidLead();

    const form = screen.getByRole('form', { name: 'Форма заявки' });
    fireEvent.submit(form);
    fireEvent.submit(form);

    const sendingButton = screen.getByRole('button', { name: 'Отправляем…' });
    expect(sendingButton).toBeDisabled();
    expect(sendingButton.querySelector('[aria-hidden="true"]')).not.toBeNull();
    expect(form).toHaveAttribute('aria-busy', 'true');
    expect(fetchMock).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveRequest({ headers: new Headers(), status: 202 });
    });

    expect(screen.getByText(SUCCESS_MESSAGE)).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Заявка принята' }),
    ).toHaveFocus();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
  });

  it('creates a fresh request ID for a new form after success', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 202,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);

    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));
    await screen.findByText(SUCCESS_MESSAGE);
    fireEvent.click(
      screen.getByRole('button', { name: 'Оставить ещё одну заявку' }),
    );
    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const requestIds = fetchMock.mock.calls.map(([, request]) => {
      const payload = JSON.parse(String((request as RequestInit).body)) as {
        requestId: string;
      };
      return payload.requestId;
    });
    expect(requestIds[0]).not.toBe(requestIds[1]);
  });

  it('reuses request data and ID while progressing through first and second retry states', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 503,
    });
    vi.stubGlobal('fetch', fetchMock);
    const analytics: Array<Record<string, unknown>> = [];
    window.addEventListener('andrew:analytics-request', (event) => {
      analytics.push((event as CustomEvent<Record<string, unknown>>).detail);
    });
    render(<LeadForm />);

    act(() => dispatchLeadContext('maintenance', 'maintenance'));
    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    expect(await screen.findByText(FIRST_ERROR_MESSAGE)).toBeInTheDocument();
    expect(screen.getByLabelText('Имя')).toHaveValue('Тест');
    expect(screen.getByLabelText('Телефон')).toHaveValue('+7 (999) 123-45-67');
    fireEvent.click(
      screen.getByRole('button', { name: 'Попробовать ещё раз' }),
    );

    expect(await screen.findByText(SECOND_ERROR_MESSAGE)).toBeInTheDocument();
    expect(screen.getByText('Телефон не опубликован')).not.toBe(
      screen.queryByRole('link', { name: /Позвонить/ }),
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const bodies = fetchMock.mock.calls.map(([, request]) =>
      JSON.parse(String((request as RequestInit).body)),
    );
    expect(bodies[1]).toEqual(bodies[0]);
    expect(analytics.map(({ name }) => name)).toEqual([
      'form_start',
      'form_submit',
      'form_error',
      'form_retry',
      'form_submit',
      'form_error',
    ]);
    expect(
      analytics.every(({ sourceSection }) => sourceSection === 'maintenance'),
    ).toBe(true);
    expect(JSON.stringify(analytics)).not.toContain('89991234567');
    expect(JSON.stringify(analytics)).not.toContain('Синтетическая заявка');
  });

  it('retains the in-memory draft and exposes an honest phone fallback offline', async () => {
    enableLoopbackPreview();
    setOnline(false);
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);
    fillValidLead();

    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    expect(await screen.findByText(OFFLINE_MESSAGE)).toBeInTheDocument();
    expect(screen.getByLabelText('Имя')).toHaveValue('Тест');
    expect(screen.getByLabelText('Телефон')).toHaveValue('+7 (999) 123-45-67');
    expect(screen.getByText('Телефон не опубликован')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Телефон/ })).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();

    await act(async () => {
      setOnline(true);
      window.dispatchEvent(new Event('online'));
    });
    expect(
      screen.getByRole('button', { name: 'Попробовать ещё раз' }),
    ).toBeEnabled();
  });

  it('sends only the honeypot value when it is filled', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 202,
    });
    vi.stubGlobal('fetch', fetchMock);
    const { container } = render(<LeadForm />);

    const honeypot = container.querySelector<HTMLInputElement>(
      'input[name="website"]',
    );
    fireEvent.change(honeypot!, { target: { value: 'robot-check' } });
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(String(request.body))).toEqual({
      website: 'robot-check',
    });
  });

  it('honors Retry-After before allowing the same attempt to retry', async () => {
    vi.useFakeTimers();
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers({ 'Retry-After': '2' }),
      status: 429,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);
    fillValidLead();

    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));
    await act(async () => Promise.resolve());

    expect(
      screen.getByRole('button', { name: 'Повторить через 2 с' }),
    ).toBeDisabled();
    expect(screen.getByLabelText('Имя')).toBeDisabled();
    expect(screen.getByLabelText('Телефон')).toBeDisabled();
    expect(screen.getByLabelText('Опишите неисправность')).toBeDisabled();

    await act(async () => {
      vi.advanceTimersByTime(2000);
    });

    expect(
      screen.getByRole('button', { name: 'Попробовать ещё раз' }),
    ).toBeEnabled();
    expect(screen.getByLabelText('Имя')).toBeEnabled();
  });

  it('has no WCAG A or AA accessibility violations', async () => {
    enableLoopbackPreview();
    const { container } = render(<LeadForm />);

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  });
});
