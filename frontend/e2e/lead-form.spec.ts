import {
  expect,
  test,
  type Page,
  type Request,
  type Route,
} from '@playwright/test';

const PREVIEW_ORIGIN = 'http://127.0.0.1:4173';
const API_ORIGIN = 'http://127.0.0.1:4174';
const API_URL = `${API_ORIGIN}/api/leads`;
const HOSTED_PREVIEW_URL = 'http://preview.invalid:4173/';
const SYNTHETIC_LEAD = {
  comment: 'Synthetic equipment failure',
  name: 'Test Operator',
  phone: '+7 (999) 000-00-00',
  phoneInput: '89990000000',
} as const;
const SUCCESS_MESSAGE =
  'Заявка принята. Свяжемся с вами в рабочее время после проверки доступности мастера.';
const FIRST_ERROR_MESSAGE =
  'Не удалось отправить заявку. Проверьте соединение и попробуйте ещё раз.';
const SECOND_ERROR_MESSAGE =
  'Заявка не отправлена. Попробуйте ещё раз или позвоните нам.';

type CapturedRequest = {
  body: Record<string, unknown>;
  contentType: string | undefined;
  method: string;
  url: string;
};

type MockResponse = {
  body?: string;
  delayMs?: number;
  headers?: Record<string, string>;
  status: number;
};

const UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu;

test.use({
  launchOptions: {
    args: [
      '--host-resolver-rules=MAP preview.invalid 127.0.0.1',
      '--no-proxy-server',
    ],
  },
});

test.beforeEach(async ({}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'The lead transport contract needs one Chromium execution per scenario.',
  );
});

function captureHttpOrigins(page: Page) {
  const origins = new Set<string>();

  page.on('request', (request) => {
    const url = new URL(request.url());

    if (url.protocol === 'http:' || url.protocol === 'https:') {
      origins.add(url.origin);
    }
  });

  return origins;
}

function captureRequest(request: Request): CapturedRequest {
  return {
    body: request.postDataJSON() as Record<string, unknown>,
    contentType: request.headers()['content-type'],
    method: request.method(),
    url: request.url(),
  };
}

async function installApiMock(
  page: Page,
  respond: (
    request: CapturedRequest,
    attempt: number,
  ) => MockResponse | Promise<MockResponse>,
) {
  const requests: CapturedRequest[] = [];

  await page.route(API_URL, async (route: Route) => {
    const request = route.request();

    if (request.method() === 'OPTIONS') {
      await route.fulfill({
        headers: {
          'access-control-allow-headers': 'content-type',
          'access-control-allow-methods': 'POST',
          'access-control-allow-origin': PREVIEW_ORIGIN,
        },
        status: 204,
      });
      return;
    }

    const captured = captureRequest(request);
    requests.push(captured);
    const response = await respond(captured, requests.length);

    if (response.delayMs) {
      await new Promise((resolve) => setTimeout(resolve, response.delayMs));
    }

    await route
      .fulfill({
        body: response.body,
        headers: {
          'access-control-expose-headers': 'retry-after',
          'access-control-allow-origin': PREVIEW_ORIGIN,
          ...response.headers,
        },
        status: response.status,
      })
      .catch(() => undefined);
  });

  return requests;
}

async function openEnabledForm(page: Page, path = '/') {
  await page.goto(path);

  const form = page.getByRole('form', { name: 'Форма заявки' });
  await expect(form).toBeVisible();
  await expect(form.getByRole('status')).toHaveText(
    'Локальная тестовая отправка включена. Используйте только синтетические данные.',
  );
  await expect(form.getByLabel('Имя')).toBeEnabled();
  await expect(form.getByLabel('Телефон')).toBeEnabled();
  await expect(form.getByLabel('Опишите неисправность')).toBeEnabled();
  await expect(form.getByRole('radio')).toHaveCount(0);
  await expect(form.getByRole('checkbox')).toHaveCount(0);

  return form;
}

async function fillSyntheticLead(page: Page, comment = SYNTHETIC_LEAD.comment) {
  const form = page.getByRole('form', { name: 'Форма заявки' });
  await form.getByLabel('Имя').fill(SYNTHETIC_LEAD.name);
  await form.getByLabel('Телефон').fill(SYNTHETIC_LEAD.phoneInput);
  await expect(form.getByLabel('Телефон')).toHaveValue(SYNTHETIC_LEAD.phone);
  await form.getByLabel('Опишите неисправность').fill(comment);

  const submit = form.getByRole('button');
  await expect(submit).toHaveAccessibleName('Отправить заявку');
  await expect(submit).toBeEnabled();

  return { form, submit, status: form.getByRole('status') };
}

function serviceUnavailableProblem() {
  return JSON.stringify({
    detail: 'The request cannot be accepted durably at this time.',
    instance: '/api/leads',
    status: 503,
    title: 'Service unavailable',
    type: 'urn:andrew:problem:service-unavailable',
  });
}

test('submits the exact OpenAPI payload on loopback and keeps synthetic lead data ephemeral', async ({
  page,
}) => {
  const origins = captureHttpOrigins(page);
  const requests = await installApiMock(page, () => ({ status: 202 }));
  await openEnabledForm(page);
  const { submit } = await fillSyntheticLead(page);

  await submit.click();

  await expect.poll(() => requests.length).toBe(1);
  await expect(page.getByText(SUCCESS_MESSAGE)).toBeVisible();
  await expect(
    page.getByRole('heading', { name: 'Заявка принята' }),
  ).toBeFocused();
  const [request] = requests;
  expect(request).toMatchObject({
    contentType: 'application/json',
    method: 'POST',
    url: API_URL,
  });
  expect(Object.keys(request.body).sort()).toEqual(
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
  expect(request.body).toEqual({
    comment: SYNTHETIC_LEAD.comment,
    consent: true,
    intent: 'repair',
    name: SYNTHETIC_LEAD.name,
    phone: SYNTHETIC_LEAD.phone,
    requestId: expect.stringMatching(UUID_V4),
    sourcePath: '/',
  });
  expect([...origins].sort()).toEqual([API_ORIGIN, PREVIEW_ORIGIN].sort());

  const browserUrl = page.url();
  const persistence = await page.evaluate(async () => ({
    cacheNames: await caches.keys(),
    cookie: document.cookie,
    databaseNames: (await indexedDB.databases()).map(({ name }) => name),
    local: Object.entries(localStorage),
    session: Object.entries(sessionStorage),
  }));
  for (const value of Object.values(SYNTHETIC_LEAD)) {
    expect(browserUrl).not.toContain(value);
    expect(JSON.stringify(persistence)).not.toContain(value);
  }
  expect(persistence).toEqual({
    cacheNames: [],
    cookie: '',
    databaseNames: [],
    local: [],
    session: [],
  });
});

test('derives sourcePath from a product pathname without query or fragment data', async ({
  page,
}) => {
  const requests = await installApiMock(page, () => ({ status: 202 }));
  await openEnabledForm(page, '/kontakty?preview=1#contact');
  const { submit } = await fillSyntheticLead(page);

  await submit.click();

  await expect.poll(() => requests.length).toBe(1);
  expect(requests[0]?.body.sourcePath).toBe('/kontakty');
  expect(JSON.stringify(requests[0]?.body)).not.toContain('preview=1');
  expect(JSON.stringify(requests[0]?.body)).not.toContain('#contact');

  const persistence = await page.evaluate(async () => ({
    cacheNames: await caches.keys(),
    cookie: document.cookie,
    databaseNames: (await indexedDB.databases()).map(({ name }) => name),
    local: Object.entries(localStorage),
    session: Object.entries(sessionStorage),
  }));
  for (const value of Object.values(SYNTHETIC_LEAD)) {
    expect(JSON.stringify(persistence)).not.toContain(value);
  }
  expect(persistence).toEqual({
    cacheNames: [],
    cookie: '',
    databaseNames: [],
    local: [],
    session: [],
  });
});

test('retries an unchanged payload with the same UUID', async ({ page }) => {
  const requests = await installApiMock(page, (_request, attempt) =>
    attempt === 1
      ? {
          body: serviceUnavailableProblem(),
          headers: { 'content-type': 'application/problem+json' },
          status: 503,
        }
      : { status: 202 },
  );
  await openEnabledForm(page);
  const { submit } = await fillSyntheticLead(page);

  await submit.click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(submit).toBeEnabled();
  await submit.click();

  await expect.poll(() => requests.length).toBe(2);
  await expect(page.getByText(SUCCESS_MESSAGE)).toBeVisible();
  expect(requests[1]?.body).toEqual(requests[0]?.body);
  expect(requests[1]?.body.requestId).toMatch(UUID_V4);
});

test('creates a new UUID after the user edits a failed payload', async ({
  page,
}) => {
  const requests = await installApiMock(page, (_request, attempt) =>
    attempt === 1
      ? {
          body: serviceUnavailableProblem(),
          headers: { 'content-type': 'application/problem+json' },
          status: 503,
        }
      : { status: 202 },
  );
  await openEnabledForm(page);
  const { form, submit } = await fillSyntheticLead(page);

  await submit.click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(submit).toBeEnabled();
  await form
    .getByLabel('Опишите неисправность')
    .fill('Synthetic equipment failure, edited');
  await submit.click();

  await expect.poll(() => requests.length).toBe(2);
  expect(requests[0]?.body.requestId).toMatch(UUID_V4);
  expect(requests[1]?.body.requestId).toMatch(UUID_V4);
  expect(requests[1]?.body.requestId).not.toBe(requests[0]?.body.requestId);
});

test('creates a new UUID for a manual retry after a 409 response', async ({
  page,
}) => {
  const requests = await installApiMock(page, (_request, attempt) =>
    attempt === 1 ? { status: 409 } : { status: 202 },
  );
  await openEnabledForm(page);
  const { status, submit } = await fillSyntheticLead(page);

  await submit.click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(status).toHaveText(FIRST_ERROR_MESSAGE);
  await expect(submit).toBeEnabled();
  await submit.click();

  await expect.poll(() => requests.length).toBe(2);
  expect(requests[0]?.body.requestId).toMatch(UUID_V4);
  expect(requests[1]?.body.requestId).toMatch(UUID_V4);
  expect(requests[1]?.body.requestId).not.toBe(requests[0]?.body.requestId);
});

test('honors Retry-After cooldown after a 429 response', async ({ page }) => {
  const requests = await installApiMock(page, (_request, attempt) =>
    attempt === 1
      ? {
          body: JSON.stringify({
            detail: 'Wait before submitting another request.',
            instance: '/api/leads',
            status: 429,
            title: 'Too many requests',
            type: 'urn:andrew:problem:rate-limit-exceeded',
          }),
          headers: {
            'content-type': 'application/problem+json',
            'retry-after': '1',
          },
          status: 429,
        }
      : { status: 202 },
  );
  await openEnabledForm(page);
  const { status, submit } = await fillSyntheticLead(page);

  await submit.click();

  await expect.poll(() => requests.length).toBe(1);
  await expect(status).toHaveText(FIRST_ERROR_MESSAGE);
  await expect(submit).toBeDisabled();
  await expect(submit).toBeEnabled({ timeout: 3000 });
  await submit.click();
  await expect.poll(() => requests.length).toBe(2);
});

test('shows exact validation and retry states without losing the draft', async ({
  page,
}) => {
  const requests = await installApiMock(page, () => ({ status: 503 }));
  const form = await openEnabledForm(page);

  await form.getByRole('button', { name: 'Отправить заявку' }).click();
  await expect(form.getByLabel('Имя')).toBeFocused();
  await expect(form.getByLabel('Имя')).toHaveAccessibleDescription(
    'Укажите имя длиной от 2 до 50 символов',
  );
  await expect(form.getByLabel('Телефон')).toHaveAccessibleDescription(
    /Введите номер телефона/u,
  );

  const { status, submit } = await fillSyntheticLead(page);
  await submit.click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(status).toHaveText(FIRST_ERROR_MESSAGE);
  await expect(form.getByLabel('Телефон')).toHaveValue(SYNTHETIC_LEAD.phone);
  await expect(submit).toHaveText('Попробовать ещё раз');

  await submit.click();
  await expect.poll(() => requests.length).toBe(2);
  await expect(status).toHaveText(SECOND_ERROR_MESSAGE);
  await expect(form.getByText('Телефон не опубликован')).toBeVisible();
  await expect(form.getByRole('link', { name: /Позвонить/u })).toHaveCount(0);
  expect(requests[1]?.body).toEqual(requests[0]?.body);
});

test('uses safe CTA context only in intent and PII-free analytics', async ({
  page,
}) => {
  await page.addInitScript(() => {
    const analytics: unknown[] = [];
    Object.assign(window, { __andrewAnalytics: analytics });
    window.addEventListener('andrew:analytics-request', (event) => {
      analytics.push((event as CustomEvent).detail);
    });
  });
  const requests = await installApiMock(page, () => ({ status: 202 }));
  await openEnabledForm(page);
  await page.evaluate(() => {
    window.dispatchEvent(
      new CustomEvent('andrew:lead-context', {
        detail: { intent: 'maintenance', sourceSection: 'maintenance' },
      }),
    );
  });
  const { submit } = await fillSyntheticLead(page);

  await submit.click();

  await expect.poll(() => requests.length).toBe(1);
  await expect(page.getByText(SUCCESS_MESSAGE)).toBeVisible();
  expect(requests[0]?.body.intent).toBe('maintenance');
  expect(requests[0]?.body).not.toHaveProperty('sourceSection');
  const analytics = await page.evaluate(
    () =>
      (
        window as typeof window & {
          __andrewAnalytics: Array<Record<string, unknown>>;
        }
      ).__andrewAnalytics,
  );
  const formAnalytics = analytics.filter(({ name }) =>
    typeof name === 'string' ? name.startsWith('form_') : false,
  );
  expect(formAnalytics).toEqual([
    { name: 'form_start', sourceSection: 'maintenance' },
    { name: 'form_submit', sourceSection: 'maintenance' },
    { name: 'form_success', sourceSection: 'maintenance' },
  ]);
  expect(JSON.stringify(formAnalytics)).not.toContain(SYNTHETIC_LEAD.name);
  expect(JSON.stringify(formAnalytics)).not.toContain(SYNTHETIC_LEAD.phone);
});

test('keeps entered values in memory while offline', async ({
  context,
  page,
}) => {
  const requests = await installApiMock(page, () => ({ status: 202 }));
  await openEnabledForm(page);
  const { form, submit, status } = await fillSyntheticLead(page);

  await context.setOffline(true);
  try {
    await submit.click();

    await expect(status).toHaveText(
      'Нет соединения. Введённые данные сохранены.',
    );
    await expect(form.getByLabel('Имя')).toHaveValue(SYNTHETIC_LEAD.name);
    await expect(form.getByLabel('Телефон')).toHaveValue(SYNTHETIC_LEAD.phone);
    await expect(form.getByText('Телефон не опубликован')).toBeVisible();
    expect(requests).toHaveLength(0);
  } finally {
    await context.setOffline(false);
  }
});

test('aborts a stalled request after the 15-second client timeout', async ({
  page,
}) => {
  test.setTimeout(25000);
  const requests = await installApiMock(page, () => ({
    delayMs: 16000,
    status: 202,
  }));
  await openEnabledForm(page);
  const { status, submit } = await fillSyntheticLead(page);

  await submit.click();

  await expect.poll(() => requests.length).toBe(1);
  await expect(status).toHaveText(FIRST_ERROR_MESSAGE, {
    timeout: 18000,
  });
  await expect(submit).toBeEnabled();
});

test('keeps a non-loopback hosted preview fail-closed', async ({ page }) => {
  const origins = captureHttpOrigins(page);
  let apiRequests = 0;
  page.on('request', (request) => {
    if (request.url().startsWith(API_ORIGIN)) {
      apiRequests += 1;
    }
  });

  const response = await page.goto(HOSTED_PREVIEW_URL);
  expect(response?.status()).toBe(200);

  const form = page.getByRole('form', { name: 'Форма заявки' });
  await expect(form).toBeVisible();
  await expect(form.getByLabel('Имя')).toBeDisabled();
  await expect(form.getByLabel('Телефон')).toBeDisabled();
  await expect(form.getByLabel('Опишите неисправность')).toBeDisabled();
  await expect(
    form.getByRole('button', { name: 'Отправить заявку' }),
  ).toBeDisabled();
  await expect(form.getByRole('status')).toHaveText(
    'Backend формы не подключён к этому предпросмотру.',
  );
  expect(apiRequests).toBe(0);
  expect([...origins]).toEqual([new URL(HOSTED_PREVIEW_URL).origin]);
});
