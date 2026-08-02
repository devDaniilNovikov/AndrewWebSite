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

const enableLoopbackPreview = () => {
  vi.stubEnv('NEXT_PUBLIC_BUILD_MODE', 'preview');
  vi.stubEnv('NEXT_PUBLIC_PREVIEW_API_ORIGIN', 'http://127.0.0.1:8080');
};

const fillValidLead = () => {
  fireEvent.change(screen.getByLabelText('Имя'), {
    target: { value: 'Тест' },
  });
  fireEvent.change(screen.getByLabelText('Телефон'), {
    target: { value: '0000000' },
  });
  fireEvent.change(screen.getByLabelText('Комментарий'), {
    target: { value: 'Синтетическая заявка' },
  });
  fireEvent.click(screen.getByLabelText('Плановое обслуживание'));
  fireEvent.click(
    screen.getByLabelText(
      'Я явно соглашаюсь на обработку данных. Юридический текст уточняется.',
    ),
  );
};

describe('LeadForm', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
    vi.stubEnv('NEXT_PUBLIC_BUILD_MODE', 'preview');
    vi.stubEnv('NEXT_PUBLIC_PREVIEW_API_ORIGIN', '');
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('keeps a hosted or unconfigured preview inert without accepting PII', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const { container } = render(<LeadForm />);
    const form = screen.getByRole('form', { name: 'Форма заявки' });

    expect(within(form).getByLabelText('Имя')).toBeDisabled();
    expect(within(form).getByLabelText('Телефон')).toBeDisabled();
    expect(within(form).getByLabelText('Комментарий')).toBeDisabled();
    expect(within(form).getByLabelText('Ремонт')).toBeDisabled();
    expect(within(form).getByLabelText('Плановое обслуживание')).toBeDisabled();
    expect(
      within(form).getByLabelText(
        'Я явно соглашаюсь на обработку данных. Юридический текст уточняется.',
      ),
    ).toBeDisabled();
    expect(
      within(form).getByRole('button', { name: 'Отправить заявку' }),
    ).toBeDisabled();
    expect(within(form).getByRole('status')).toHaveTextContent(
      'Отправка заявок в опубликованной демонстрации отключена.',
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

  it('summarizes invalid fields and focuses the first invalid control', () => {
    enableLoopbackPreview();
    render(<LeadForm />);

    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    const summary = screen.getByRole('alert', {
      name: 'Проверьте поля формы',
    });
    expect(summary).toHaveTextContent('Исправьте отмеченные поля.');

    const name = screen.getByLabelText('Имя');
    expect(name).toHaveFocus();
    expect(name).toHaveAttribute('aria-invalid', 'true');
    expect(name).toHaveAccessibleDescription('Укажите имя.');
    expect(screen.getByLabelText('Телефон')).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    expect(
      screen.getByLabelText(
        'Я явно соглашаюсь на обработку данных. Юридический текст уточняется.',
      ),
    ).toHaveAttribute('aria-invalid', 'true');
  });

  it('submits an enabled loopback form and clears PII after acceptance', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 202,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);

    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    expect(await screen.findByText('Заявка принята.')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText('Имя')).toHaveValue('');
    expect(screen.getByLabelText('Телефон')).toHaveValue('');
    expect(screen.getByLabelText('Комментарий')).toHaveValue('');
    expect(screen.getByLabelText('Ремонт')).toBeChecked();
    expect(screen.getByLabelText('Плановое обслуживание')).not.toBeChecked();
    expect(
      screen.getByLabelText(
        'Я явно соглашаюсь на обработку данных. Юридический текст уточняется.',
      ),
    ).not.toBeChecked();
  });

  it('keeps a failed attempt retryable until an edit invalidates it', async () => {
    enableLoopbackPreview();
    const fetchMock = vi.fn().mockResolvedValue({
      headers: new Headers(),
      status: 503,
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<LeadForm />);

    fillValidLead();
    fireEvent.click(screen.getByRole('button', { name: 'Отправить заявку' }));

    expect(
      await screen.findByText(
        'Сервис временно недоступен. Повторите отправку.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Повторить отправку' }),
    ).toBeEnabled();

    fireEvent.change(screen.getByLabelText('Имя'), {
      target: { value: 'Тест изменён' },
    });

    expect(
      screen.getByRole('button', { name: 'Отправить заявку' }),
    ).toBeEnabled();
    expect(
      screen.queryByText('Сервис временно недоступен. Повторите отправку.'),
    ).not.toBeInTheDocument();
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

  it('guards against duplicate submit events before React rerenders', async () => {
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

    expect(fetchMock).toHaveBeenCalledTimes(1);
    await act(async () => {
      resolveRequest({ headers: new Headers(), status: 202 });
    });
    expect(screen.getByRole('status')).toHaveTextContent('Заявка принята.');
  });

  it('locks every field during a rate-limit cooldown and unlocks at zero', async () => {
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
    expect(screen.getByLabelText('Комментарий')).toBeDisabled();
    expect(screen.getByLabelText('Ремонт')).toBeDisabled();
    expect(
      screen.getByLabelText(
        'Я явно соглашаюсь на обработку данных. Юридический текст уточняется.',
      ),
    ).toBeDisabled();

    await act(async () => {
      vi.advanceTimersByTime(2000);
    });

    expect(
      screen.getByRole('button', { name: 'Повторить отправку' }),
    ).toBeEnabled();
    expect(screen.getByLabelText('Имя')).toBeEnabled();
  });

  it('has no WCAG A or AA accessibility violations', async () => {
    enableLoopbackPreview();
    const { container } = render(<LeadForm />);

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  });
});
