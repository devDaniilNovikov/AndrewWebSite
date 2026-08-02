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
import type {
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
import { validateLeadForm } from '../../lib/leads/validation';
import { PlaceholderBadge } from '../landing/PreviewPrimitives';

const initialValues: LeadFormValues = {
  name: '',
  phone: '',
  comment: '',
  intent: 'repair',
  consent: false,
  website: '',
};

const validationMessages: Readonly<
  Record<NonNullable<LeadValidationErrors[LeadValidationField]>, string>
> = {
  name_required: 'Укажите имя.',
  name_length: 'Укажите имя от 2 до 100 символов.',
  phone_required: 'Укажите телефон.',
  phone_length: 'Телефон должен содержать от 7 до 15 цифр.',
  phone_format: 'Используйте только цифры и символы + ( ) . пробел или дефис.',
  comment_length: 'Сократите комментарий до 1000 символов.',
  intent_invalid: 'Выберите вид обращения.',
  consent_required: 'Подтвердите согласие перед отправкой.',
  source_path_invalid: 'Не удалось определить безопасный адрес страницы.',
};

const fieldOrder: readonly LeadValidationField[] = [
  'name',
  'phone',
  'comment',
  'intent',
  'consent',
  'sourcePath',
];

type Feedback = LeadResponseOutcome | null;

const subscribeToPageOrigin = () => () => undefined;
const readBrowserPageOrigin = () => window.location.origin;
const readServerPageOrigin = () => '';

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
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const submissionInFlightRef = useRef(false);
  const nameRef = useRef<HTMLInputElement>(null);
  const phoneRef = useRef<HTMLInputElement>(null);
  const commentRef = useRef<HTMLTextAreaElement>(null);
  const repairRef = useRef<HTMLInputElement>(null);
  const consentRef = useRef<HTMLInputElement>(null);
  const pageOrigin = useSyncExternalStore(
    subscribeToPageOrigin,
    readBrowserPageOrigin,
    readServerPageOrigin,
  );
  const policy = resolveLeadEndpoint({
    buildMode: process.env.NEXT_PUBLIC_BUILD_MODE,
    pageOrigin,
    previewApiOrigin: process.env.NEXT_PUBLIC_PREVIEW_API_ORIGIN,
  });

  useEffect(() => {
    if (cooldownSeconds <= 0) {
      return;
    }

    const timer = window.setInterval(() => {
      setCooldownSeconds((seconds) => Math.max(0, seconds - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [cooldownSeconds]);

  const formLocked =
    !policy.enabled || attemptState.inFlight || cooldownSeconds > 0;
  const mustEditBeforeSubmitting =
    feedback !== null && feedback.kind !== 'accepted' && !feedback.retryable;
  const submitDisabled = formLocked || mustEditBeforeSubmitting;

  const updateValue = <Field extends keyof LeadFormValues>(
    field: Field,
    value: LeadFormValues[Field],
  ) => {
    if (submissionInFlightRef.current || cooldownSeconds > 0) {
      return;
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
  };

  const focusFirstError = (validationErrors: LeadValidationErrors) => {
    const first = fieldOrder.find(
      (field) => validationErrors[field] !== undefined,
    );
    const controls: Partial<Record<LeadValidationField, HTMLElement | null>> = {
      name: nameRef.current,
      phone: phoneRef.current,
      comment: commentRef.current,
      intent: repairRef.current,
      consent: consentRef.current,
    };
    controls[first ?? 'name']?.focus();
  };

  const completeAttempt = (outcome: LeadResponseOutcome) => {
    setAttemptState((current) => {
      const finished = current.inFlight
        ? markLeadAttemptFinished(current)
        : current;
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
      setAttemptState(createLeadAttemptState());
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
      focusFirstError(validation.errors);
      return;
    }

    setErrors({});
    const prepared = prepareLeadAttempt(attemptState, validation.draft, () =>
      crypto.randomUUID(),
    );
    const started = markLeadAttemptStarted(prepared.state);
    submissionInFlightRef.current = true;
    setAttemptState(started);
    setFeedback(null);

    try {
      const result = await submitLeadAttempt(policy.endpoint, prepared.attempt);
      if (result.kind === 'response') {
        completeAttempt(classifyLeadResponse(result.status, result.retryAfter));
        return;
      }

      completeAttempt(
        classifyLeadFailure(
          result.kind === 'timeout' ? 'timeout' : 'network_error',
        ),
      );
    } catch {
      completeAttempt(classifyLeadFailure('network_error'));
    } finally {
      submissionInFlightRef.current = false;
    }
  };

  const statusMessage = (() => {
    if (!policy.enabled) {
      return 'Отправка заявок в опубликованной демонстрации отключена.';
    }
    if (attemptState.inFlight) {
      return 'Отправляем заявку…';
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
      return 'Повторить отправку';
    }
    return 'Отправить заявку';
  })();

  const nameError = errorMessage(errors, 'name');
  const phoneError = errorMessage(errors, 'phone');
  const commentError = errorMessage(errors, 'comment');
  const intentError = errorMessage(errors, 'intent');
  const consentError = errorMessage(errors, 'consent');
  const validationErrorMessages = fieldOrder
    .map((field) => errorMessage(errors, field))
    .filter((message): message is string => message !== undefined);

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
          {policy.enabled ? 'Локальный тестовый режим' : 'Отправка отключена'}
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
            {validationErrorMessages.map((message) => (
              <li key={message}>{message}</li>
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
            autoComplete="off"
            className="mt-2 min-h-11 w-full rounded-md border border-white/15 bg-white/7 px-3 text-white placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={formLocked}
            id="lead-name"
            maxLength={100}
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
              className="mt-1 block text-xs font-normal text-red-200"
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
            autoComplete="off"
            className="mt-2 min-h-11 w-full rounded-md border border-white/15 bg-white/7 px-3 text-white placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={formLocked}
            id="lead-phone"
            inputMode="tel"
            maxLength={32}
            name="phone"
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              updateValue('phone', event.target.value)
            }
            placeholder="Только синтетический номер"
            ref={phoneRef}
            type="tel"
            value={values.phone}
          />
          <span
            className="mt-1 block text-xs font-normal text-slate-400"
            id="lead-phone-hint"
          >
            До подтверждения юридических текстов не вводите реальные данные.
          </span>
          {phoneError === undefined ? null : (
            <span
              className="mt-1 block text-xs font-normal text-red-200"
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
          Комментарий
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
          placeholder="Кратко опишите тестовый сценарий"
          ref={commentRef}
          value={values.comment}
        />
        <span
          className="mt-1 block text-xs font-normal text-slate-400"
          id="lead-comment-hint"
        >
          Необязательное поле, до 1000 символов.
        </span>
        {commentError === undefined ? null : (
          <span
            className="mt-1 block text-xs font-normal text-red-200"
            id="lead-comment-error"
          >
            {commentError}
          </span>
        )}
      </div>

      <fieldset
        aria-describedby={describedBy(intentError && 'lead-intent-error')}
        aria-invalid={intentError !== undefined}
        className="mt-4"
        disabled={formLocked}
      >
        <legend className="text-sm font-semibold text-slate-200">
          Тип обращения
        </legend>
        <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:gap-5">
          <label className="inline-flex min-h-10 items-center gap-2 text-sm text-slate-200">
            <input
              checked={values.intent === 'repair'}
              className="h-4 w-4 accent-primary"
              name="intent"
              onChange={() => updateValue('intent', 'repair')}
              ref={repairRef}
              type="radio"
              value="repair"
            />
            Ремонт
          </label>
          <label className="inline-flex min-h-10 items-center gap-2 text-sm text-slate-200">
            <input
              checked={values.intent === 'maintenance'}
              className="h-4 w-4 accent-primary"
              name="intent"
              onChange={() => updateValue('intent', 'maintenance')}
              type="radio"
              value="maintenance"
            />
            Плановое обслуживание
          </label>
        </div>
        {intentError === undefined ? null : (
          <p className="mt-1 text-xs text-red-200" id="lead-intent-error">
            {intentError}
          </p>
        )}
      </fieldset>

      <label className="mt-4 flex items-start gap-3 text-xs leading-5 text-slate-300">
        <input
          aria-describedby={describedBy(
            'lead-consent-hint',
            consentError && 'lead-consent-error',
          )}
          aria-invalid={consentError !== undefined}
          checked={values.consent}
          className="mt-1 h-4 w-4 shrink-0 accent-primary"
          disabled={formLocked}
          name="consent"
          onChange={(event) => updateValue('consent', event.target.checked)}
          ref={consentRef}
          type="checkbox"
        />
        <span id="lead-consent-hint">
          Я явно соглашаюсь на обработку данных. Юридический текст уточняется.
        </span>
      </label>
      {consentError === undefined ? null : (
        <p className="mt-1 pl-7 text-xs text-red-200" id="lead-consent-error">
          {consentError}
        </p>
      )}

      <div
        aria-hidden="true"
        className="fixed -left-[100vw] top-0 h-px w-px overflow-hidden"
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
        className="mt-4 min-h-11 w-full rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={submitDisabled}
        type="submit"
      >
        {buttonLabel}
      </button>
      <p
        aria-live="polite"
        className="mt-3 text-xs leading-5 text-slate-300"
        role="status"
      >
        {statusMessage}
      </p>
    </form>
  );
}
