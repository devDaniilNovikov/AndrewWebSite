'use client';

import {
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
  type ChangeEvent,
  type FormEvent,
} from 'react';
import {
  createLeadAttemptState,
  invalidateLeadAttempt,
  markLeadAttemptFinished,
  markLeadAttemptStarted,
  markLeadFormEdited,
  prepareLeadAttempt,
} from '../../lib/leads/attempt-state';
import {
  DEFAULT_LEAD_CONTEXT,
  dispatchLeadAnalytics,
  parseLeadContext,
} from '../../lib/leads/context';
import type {
  LeadAnalyticsEventName,
  LeadFormValues,
  LeadResponseOutcome,
  LeadValidationErrors,
  LeadValidationField,
} from '../../lib/leads/domain-types';
import { resolveLeadEndpoint } from '../../lib/leads/endpoint-policy';
import {
  classifyLeadFailure,
  classifyLeadResponse,
} from '../../lib/leads/response-policy';
import { submitLeadAttempt } from '../../lib/leads/transport';
import {
  formatRussianPhoneInput,
  validateLeadForm,
} from '../../lib/leads/validation';
import { PlaceholderBadge } from '../landing/PreviewPrimitives';

const initialValues: LeadFormValues = Object.freeze({
  name: '',
  phone: '',
  comment: '',
  intent: 'repair',
  website: '',
});

const SUCCESS_MESSAGE =
  'Заявка принята. Свяжемся с вами в рабочее время после проверки доступности мастера.';
const FIRST_ERROR_MESSAGE =
  'Не удалось отправить заявку. Проверьте соединение и попробуйте ещё раз.';
const SECOND_ERROR_MESSAGE =
  'Заявка не отправлена. Попробуйте ещё раз или позвоните нам.';
const OFFLINE_MESSAGE = 'Нет соединения. Введённые данные сохранены.';

const validationMessages: Readonly<
  Record<NonNullable<LeadValidationErrors[LeadValidationField]>, string>
> = {
  name_required: 'Укажите имя длиной от 2 до 50 символов',
  name_length: 'Укажите имя длиной от 2 до 50 символов',
  phone_required: 'Введите номер телефона',
  phone_format: 'Введите номер телефона в формате +7 (999) 123-45-67',
  comment_length:
    'Опишите неисправность минимум в 10 символах или оставьте поле пустым',
  intent_invalid: 'Не удалось определить тип обращения.',
  source_path_invalid: 'Не удалось определить безопасный адрес страницы.',
};

const fieldOrder: readonly LeadValidationField[] = [
  'name',
  'phone',
  'comment',
  'intent',
  'sourcePath',
];

type Feedback = LeadResponseOutcome | null;

const subscribeToPageOrigin = () => () => undefined;
const readBrowserPageOrigin = () => window.location.origin;
const readServerPageOrigin = () => '';
const readBrowserOnline = () => window.navigator.onLine;
const readServerOnline = () => true;
const subscribeToOnline = (onStoreChange: () => void) => {
  window.addEventListener('online', onStoreChange);
  window.addEventListener('offline', onStoreChange);
  return () => {
    window.removeEventListener('online', onStoreChange);
    window.removeEventListener('offline', onStoreChange);
  };
};

function errorMessage(
  errors: LeadValidationErrors,
  field: LeadValidationField,
): string | undefined {
  const code = errors[field];
  return code === undefined ? undefined : validationMessages[code];
}

function describedBy(
  ...ids: Array<string | false | undefined>
): string | undefined {
  const value = ids.filter(Boolean).join(' ');
  return value.length > 0 ? value : undefined;
}

export function LeadForm() {
  const [values, setValues] = useState<LeadFormValues>(initialValues);
  const [errors, setErrors] = useState<LeadValidationErrors>({});
  const [attemptState, setAttemptState] = useState(createLeadAttemptState);
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [failureCount, setFailureCount] = useState(0);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const submissionInFlightRef = useRef(false);
  const formStartedRef = useRef(false);
  const contextRef = useRef(DEFAULT_LEAD_CONTEXT);
  const nameRef = useRef<HTMLInputElement>(null);
  const phoneRef = useRef<HTMLInputElement>(null);
  const commentRef = useRef<HTMLTextAreaElement>(null);
  const successRef = useRef<HTMLHeadingElement>(null);
  const pageOrigin = useSyncExternalStore(
    subscribeToPageOrigin,
    readBrowserPageOrigin,
    readServerPageOrigin,
  );
  const isOnline = useSyncExternalStore(
    subscribeToOnline,
    readBrowserOnline,
    readServerOnline,
  );
  const policy = resolveLeadEndpoint({
    buildMode: process.env.NEXT_PUBLIC_BUILD_MODE,
    pageOrigin,
    previewApiOrigin: process.env.NEXT_PUBLIC_PREVIEW_API_ORIGIN,
  });

  useEffect(() => {
    const handleLeadContext = (event: Event) => {
      if (submissionInFlightRef.current) {
        return;
      }

      const nextContext = parseLeadContext((event as CustomEvent).detail);
      if (nextContext === null) {
        return;
      }

      contextRef.current = nextContext;
      setValues((current) => ({ ...current, intent: nextContext.intent }));
      setAttemptState((current) => markLeadFormEdited(current));
      setErrors((current) => {
        if (current.intent === undefined) {
          return current;
        }
        const next = { ...current };
        delete next.intent;
        return next;
      });
      setFeedback(null);
      setFailureCount(0);
      setCooldownSeconds(0);
    };

    window.addEventListener('andrew:lead-context', handleLeadContext);
    return () => {
      window.removeEventListener('andrew:lead-context', handleLeadContext);
    };
  }, []);

  useEffect(() => {
    if (cooldownSeconds <= 0) {
      return;
    }

    const timer = window.setInterval(() => {
      setCooldownSeconds((seconds) => Math.max(0, seconds - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [cooldownSeconds]);

  useEffect(() => {
    if (feedback?.kind === 'accepted') {
      successRef.current?.focus();
    }
  }, [feedback]);

  const emitAnalytics = (
    name: LeadAnalyticsEventName,
    sourceSection = contextRef.current.sourceSection,
  ) => dispatchLeadAnalytics(name, sourceSection);

  const formLocked =
    !policy.enabled || attemptState.inFlight || cooldownSeconds > 0;
  const mustEditBeforeSubmitting =
    feedback !== null && feedback.kind !== 'accepted' && !feedback.retryable;
  const submitDisabled = formLocked || mustEditBeforeSubmitting;

  const beginForm = () => {
    if (formStartedRef.current) {
      return;
    }
    formStartedRef.current = true;
    emitAnalytics('form_start');
  };

  const updateValue = <Field extends keyof LeadFormValues>(
    field: Field,
    value: LeadFormValues[Field],
  ) => {
    if (submissionInFlightRef.current || cooldownSeconds > 0) {
      return;
    }

    if (field !== 'website') {
      beginForm();
    }
    setValues((current) => ({ ...current, [field]: value }));
    setAttemptState((current) => markLeadFormEdited(current));
    setErrors((current) => {
      if (!(field in current)) {
        return current;
      }

      const next = { ...current };
      delete next[field as keyof LeadValidationErrors];
      return next;
    });
    setFeedback(null);
    setFailureCount(0);
  };

  const focusFirstError = (validationErrors: LeadValidationErrors) => {
    const first = fieldOrder.find(
      (field) => validationErrors[field] !== undefined,
    );
    const controls: Partial<Record<LeadValidationField, HTMLElement | null>> = {
      name: nameRef.current,
      phone: phoneRef.current,
      comment: commentRef.current,
    };
    controls[first ?? 'name']?.focus();
  };

  const completeAttempt = (
    outcome: LeadResponseOutcome,
    sourceSection: string,
    analyticsEnabled: boolean,
  ) => {
    setAttemptState((current) => {
      const finished = current.inFlight
        ? markLeadAttemptFinished(current)
        : current;
      if (outcome.kind === 'accepted') {
        return createLeadAttemptState();
      }
      return outcome.invalidateAttempt
        ? invalidateLeadAttempt(finished)
        : finished;
    });
    setFeedback(outcome);

    if (outcome.retryAfterSeconds !== undefined) {
      setCooldownSeconds(outcome.retryAfterSeconds);
    }

    if (outcome.kind === 'accepted') {
      setValues(initialValues);
      setErrors({});
      setFailureCount(0);
      contextRef.current = DEFAULT_LEAD_CONTEXT;
      formStartedRef.current = false;
      if (analyticsEnabled) {
        emitAnalytics('form_success', sourceSection);
      }
      return;
    }

    if (outcome.retryable) {
      setFailureCount((count) => Math.min(2, count + 1));
    }
    if (analyticsEnabled) {
      emitAnalytics('form_error', sourceSection);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!policy.enabled || submitDisabled || submissionInFlightRef.current) {
      return;
    }

    const validation = validateLeadForm(values, window.location.pathname);
    if (!validation.ok) {
      setErrors(validation.errors);
      setFeedback(null);
      setFailureCount(0);
      emitAnalytics('form_validation_error');
      focusFirstError(validation.errors);
      return;
    }

    const analyticsEnabled = !('website' in validation.draft);
    const sourceSection = contextRef.current.sourceSection;
    if (analyticsEnabled && feedback?.retryable) {
      emitAnalytics('form_retry', sourceSection);
    }
    if (analyticsEnabled) {
      emitAnalytics('form_submit', sourceSection);
    }

    setErrors({});
    const prepared = prepareLeadAttempt(attemptState, validation.draft, () =>
      crypto.randomUUID(),
    );

    if (!isOnline) {
      setAttemptState(prepared.state);
      completeAttempt(
        classifyLeadFailure('offline'),
        sourceSection,
        analyticsEnabled,
      );
      return;
    }

    const started = markLeadAttemptStarted(prepared.state);
    submissionInFlightRef.current = true;
    setAttemptState(started);
    setFeedback(null);

    try {
      const result = await submitLeadAttempt(policy.endpoint, prepared.attempt);
      if (result.kind === 'response') {
        completeAttempt(
          classifyLeadResponse(result.status, result.retryAfter),
          sourceSection,
          analyticsEnabled,
        );
        return;
      }

      completeAttempt(
        classifyLeadFailure(
          result.kind === 'timeout' ? 'timeout' : 'network_error',
        ),
        sourceSection,
        analyticsEnabled,
      );
    } catch {
      completeAttempt(
        classifyLeadFailure('network_error'),
        sourceSection,
        analyticsEnabled,
      );
    } finally {
      submissionInFlightRef.current = false;
    }
  };

  const resetAfterSuccess = () => {
    setValues(initialValues);
    setErrors({});
    setAttemptState(createLeadAttemptState());
    setFeedback(null);
    setFailureCount(0);
    setCooldownSeconds(0);
    contextRef.current = DEFAULT_LEAD_CONTEXT;
    formStartedRef.current = false;
  };

  if (feedback?.kind === 'accepted') {
    return (
      <section
        aria-live="polite"
        className="rounded-md border border-emerald-300/40 bg-emerald-950/30 p-5"
        role="status"
      >
        <h3
          aria-label="Заявка принята"
          className="text-xl font-semibold text-white outline-none focus-visible:ring-2 focus-visible:ring-blue-300"
          ref={successRef}
          tabIndex={-1}
        >
          {SUCCESS_MESSAGE}
        </h3>
        <button
          className="mt-5 min-h-12 rounded-md border border-white/25 px-5 py-3 text-sm font-semibold text-white hover:bg-white/10"
          onClick={resetAfterSuccess}
          type="button"
        >
          Оставить ещё одну заявку
        </button>
      </section>
    );
  }

  const statusMessage = (() => {
    if (!policy.enabled) {
      return 'Backend формы не подключён к этому предпросмотру.';
    }
    if (attemptState.inFlight) {
      return 'Отправляем заявку…';
    }
    if (!isOnline || feedback?.kind === 'offline') {
      return OFFLINE_MESSAGE;
    }
    if (feedback?.retryable) {
      return failureCount >= 2 ? SECOND_ERROR_MESSAGE : FIRST_ERROR_MESSAGE;
    }
    if (feedback !== null) {
      return feedback.message;
    }
    return 'Локальная тестовая отправка включена. Используйте только синтетические данные.';
  })();

  const buttonLabel = (() => {
    if (attemptState.inFlight) {
      return 'Отправляем…';
    }
    if (cooldownSeconds > 0) {
      return `Повторить через ${cooldownSeconds} с`;
    }
    if (feedback?.retryable) {
      return 'Попробовать ещё раз';
    }
    return 'Отправить заявку';
  })();

  const nameError = errorMessage(errors, 'name');
  const phoneError = errorMessage(errors, 'phone');
  const commentError = errorMessage(errors, 'comment');
  const validationErrorMessages = fieldOrder
    .map((field) => errorMessage(errors, field))
    .filter((message): message is string => message !== undefined);
  const showOfflineFallback = !isOnline || feedback?.kind === 'offline';
  const showSecondErrorFallback = feedback?.retryable && failureCount >= 2;

  return (
    <form
      aria-busy={attemptState.inFlight}
      aria-label="Форма заявки"
      autoComplete="off"
      onSubmit={handleSubmit}
    >
      <div className="flex flex-col gap-2 border-b border-white/10 pb-4 sm:flex-row sm:items-center sm:justify-between">
        <h3 className="text-xl font-semibold">Оставить заявку</h3>
        <PlaceholderBadge inverse>
          {policy.enabled
            ? 'Подключение к форме активно'
            : 'Backend формы не подключён'}
        </PlaceholderBadge>
      </div>

      {validationErrorMessages.length > 0 ? (
        <div
          aria-label="Проверьте поля формы"
          className="mt-4 rounded-md border border-red-200 bg-red-950/35 p-3 text-sm text-red-100"
          role="alert"
        >
          <p className="font-semibold">Исправьте отмеченные поля.</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            {validationErrorMessages.map((message, index) => (
              <li key={`${index}-${message}`}>{message}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <div>
          <label
            className="text-sm font-semibold text-slate-200"
            htmlFor="lead-name"
          >
            Имя
          </label>
          <input
            aria-describedby={describedBy(nameError && 'lead-name-error')}
            aria-invalid={nameError !== undefined}
            autoComplete="name"
            className="mt-2 min-h-12 w-full rounded-md border border-white/15 bg-white/7 px-3 text-white placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={formLocked}
            id="lead-name"
            maxLength={50}
            name="name"
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              updateValue('name', event.target.value)
            }
            placeholder="Ваше имя"
            ref={nameRef}
            type="text"
            value={values.name}
          />
          {nameError === undefined ? null : (
            <span
              className="mt-1 block text-sm font-normal text-red-200"
              id="lead-name-error"
            >
              {nameError}
            </span>
          )}
        </div>

        <div>
          <label
            className="text-sm font-semibold text-slate-200"
            htmlFor="lead-phone"
          >
            Телефон
          </label>
          <input
            aria-describedby={describedBy(
              'lead-phone-hint',
              phoneError && 'lead-phone-error',
            )}
            aria-invalid={phoneError !== undefined}
            autoComplete="tel"
            className="mt-2 min-h-12 w-full rounded-md border border-white/15 bg-white/7 px-3 text-white placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={formLocked}
            id="lead-phone"
            inputMode="tel"
            maxLength={18}
            name="phone"
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              updateValue('phone', formatRussianPhoneInput(event.target.value))
            }
            placeholder="+7 (___) ___-__-__"
            ref={phoneRef}
            type="tel"
            value={values.phone}
          />
          <span
            className="mt-1 block text-sm font-normal text-slate-400"
            id="lead-phone-hint"
          >
            Формат: +7 (999) 123-45-67.
          </span>
          {phoneError === undefined ? null : (
            <span
              className="mt-1 block text-sm font-normal text-red-200"
              id="lead-phone-error"
            >
              {phoneError}
            </span>
          )}
        </div>
      </div>

      <div className="mt-4">
        <label
          className="block text-sm font-semibold text-slate-200"
          htmlFor="lead-comment"
        >
          Опишите неисправность
        </label>
        <textarea
          aria-describedby={describedBy(
            'lead-comment-hint',
            commentError && 'lead-comment-error',
          )}
          aria-invalid={commentError !== undefined}
          className="mt-2 min-h-24 w-full resize-y rounded-md border border-white/15 bg-white/7 p-3 text-white placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={formLocked}
          id="lead-comment"
          maxLength={1000}
          name="comment"
          onChange={(event: ChangeEvent<HTMLTextAreaElement>) =>
            updateValue('comment', event.target.value)
          }
          placeholder="Тип оборудования, симптомы и район выезда"
          ref={commentRef}
          value={values.comment}
        />
        <span
          className="mt-1 block text-sm font-normal text-slate-400"
          id="lead-comment-hint"
        >
          Необязательное поле, от 10 до 1000 символов.
        </span>
        {commentError === undefined ? null : (
          <span
            className="mt-1 block text-sm font-normal text-red-200"
            id="lead-comment-error"
          >
            {commentError}
          </span>
        )}
      </div>

      <input name="intent" type="hidden" value={values.intent} />

      <div
        aria-hidden="true"
        className="fixed -left-[100vw] top-0 h-px w-px overflow-hidden"
        hidden
      >
        <label htmlFor="lead-website">Website</label>
        <input
          aria-hidden="true"
          autoComplete="off"
          disabled={formLocked}
          id="lead-website"
          name="website"
          onChange={(event) => updateValue('website', event.target.value)}
          tabIndex={-1}
          type="text"
          value={values.website}
        />
      </div>

      <button
        className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-md bg-primary px-5 py-3 text-base font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={submitDisabled}
        type="submit"
      >
        {attemptState.inFlight ? (
          <svg
            aria-hidden="true"
            className="h-4 w-4 animate-spin motion-reduce:animate-none"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="9"
              stroke="currentColor"
              strokeWidth="3"
            />
            <path
              className="opacity-90"
              d="M21 12a9 9 0 0 0-9-9"
              stroke="currentColor"
              strokeLinecap="round"
              strokeWidth="3"
            />
          </svg>
        ) : null}
        <span>{buttonLabel}</span>
      </button>
      <p className="mt-3 text-sm leading-5 text-slate-300">
        Нажимая «Отправить заявку», вы соглашаетесь на{' '}
        <a
          className="inline-flex min-h-11 items-center align-middle underline decoration-white/50 underline-offset-2 hover:decoration-white"
          href="#personal-data"
        >
          обработку персональных данных
        </a>{' '}
        и принимаете{' '}
        <a
          className="inline-flex min-h-11 items-center align-middle underline decoration-white/50 underline-offset-2 hover:decoration-white"
          href="#privacy-policy"
        >
          политику конфиденциальности
        </a>
        .
      </p>
      <p
        aria-live="polite"
        className="mt-3 text-sm leading-5 text-slate-300"
        role="status"
      >
        {statusMessage}
      </p>
      {showOfflineFallback || showSecondErrorFallback ? (
        <p className="mt-2 text-sm font-semibold text-white flex flex-col gap-2">
          <span>
            Телефон:{' '}
            <a
              href="tel:+79032375861"
              className="underline hover:text-blue-300"
            >
              +7 (903) 237-58-61
            </a>
          </span>
          <span>
            Telegram:{' '}
            <a
              href="https://t.me/AndrewGukovBot_bot"
              className="underline hover:text-blue-300"
              target="_blank"
              rel="noopener noreferrer"
            >
              t.me/AndrewGukovBot_bot
            </a>
          </span>
        </p>
      ) : null}
    </form>
  );
}
