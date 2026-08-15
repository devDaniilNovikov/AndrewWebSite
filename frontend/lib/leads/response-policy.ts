import type { LeadResponseKind, LeadResponseOutcome } from './domain-types';

const DEFAULT_RETRY_AFTER_SECONDS = 60;

const messages: Readonly<Record<LeadResponseKind, string>> = {
  accepted: 'Заявка принята.',
  invalid_request: 'Проверьте заполненные поля и отправьте заявку снова.',
  conflict:
    'Не удалось подтвердить повторную отправку. Отправьте заявку ещё раз.',
  unsupported_media_type: 'Форма сейчас не может отправить заявку.',
  rate_limited: 'Слишком много попыток. Повторите отправку позже.',
  unavailable: 'Сервис временно недоступен. Повторите отправку.',
  offline: 'Нет соединения. Введённые данные сохранены.',
  network_error: 'Нет соединения с сервисом. Повторите отправку.',
  timeout: 'Сервис не ответил вовремя. Повторите отправку.',
  unexpected: 'Не удалось отправить заявку.',
};

function outcome(
  kind: LeadResponseKind,
  retryable: boolean,
  invalidateAttempt: boolean,
  retryAfterSeconds?: number,
): LeadResponseOutcome {
  return {
    kind,
    message: messages[kind],
    retryable,
    invalidateAttempt,
    ...(retryAfterSeconds === undefined ? {} : { retryAfterSeconds }),
  };
}

export function parseRetryAfterSeconds(header: string | null): number {
  if (header === null || !/^\d+$/u.test(header)) {
    return DEFAULT_RETRY_AFTER_SECONDS;
  }

  const seconds = Number(header);
  return Number.isSafeInteger(seconds) && seconds >= 1 && seconds <= 3600
    ? seconds
    : DEFAULT_RETRY_AFTER_SECONDS;
}

export function classifyLeadResponse(
  status: number,
  retryAfterHeader: string | null,
): LeadResponseOutcome {
  if (status >= 200 && status <= 299) {
    return outcome('accepted', false, false);
  }
  if (status >= 500 && status <= 599) {
    return outcome('unavailable', true, false);
  }

  switch (status) {
    case 400:
    case 413:
      return outcome('invalid_request', false, false);
    case 409:
      return outcome('conflict', true, true);
    case 415:
      return outcome('unsupported_media_type', false, false);
    case 429:
      return outcome(
        'rate_limited',
        true,
        false,
        parseRetryAfterSeconds(retryAfterHeader),
      );
    default:
      return outcome('unexpected', false, false);
  }
}

export function classifyLeadFailure(
  kind: 'network_error' | 'offline' | 'timeout',
): LeadResponseOutcome {
  return outcome(kind, true, false);
}
