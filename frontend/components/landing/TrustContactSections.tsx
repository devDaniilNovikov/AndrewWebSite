import {
  maintenanceItems,
  navigationItems,
} from '../../content/preview-content';
import { CookieSettingsButton } from '../privacy/CookieSettingsButton';
import { LeadForm } from '../leads/LeadForm';
import {
  Container,
  LineIcon,
  MediaSlot,
  PlaceholderBadge,
  SectionHeading,
} from './PreviewPrimitives';
import { LeadCta } from './LeadCta';
import { Reveal } from './Reveal';

const serviceModelItems = [
  'Квалифицируем заявку и уточняем задачу.',
  'Подбираем специалиста под оборудование и район.',
  'Согласовываем объём и стоимость до основных работ.',
  'Контролируем результат выполненной работы.',
  'Остаёмся единым контактом по гарантийному обращению.',
] as const;

const unavailableContactChannels = [
  { icon: 'phone', label: 'Телефон' },
  { icon: 'message', label: 'Telegram' },
  { icon: 'message', label: 'WhatsApp' },
] as const;

const unavailableLegalDocuments = [
  { id: 'privacy-policy', label: 'Политика конфиденциальности' },
  {
    id: 'personal-data',
    label: 'Информация об обработке персональных данных',
  },
  { id: 'requisites', label: 'Реквизиты ИП' },
] as const;

export function AboutSection() {
  return (
    <section
      className="border-y border-slate-200 bg-surface py-14 sm:py-16 lg:py-24"
      data-section="about"
      id="about"
    >
      <Container className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-stretch lg:gap-10">
        <Reveal className="overflow-hidden rounded-xl border border-slate-200 bg-white">
          <MediaSlot
            className="h-full min-h-80"
            icon="team"
            label="Место для подтверждённой фотографии команды"
          />
        </Reveal>
        <Reveal className="rounded-xl border border-slate-200 bg-white p-6 sm:p-8">
          <SectionHeading
            description="Подбираем специалиста под оборудование и район, согласовываем стоимость и остаёмся единым контактом для клиента."
            eyebrow="О компании"
            title="Команда мастеров с техническим контролем каждой заявки"
          />
          <ul className="mt-7 grid gap-3 text-base leading-6 text-slate-600 sm:grid-cols-2">
            {serviceModelItems.map((item) => (
              <li
                className="flex min-h-14 items-start gap-3 rounded-lg bg-surface p-4"
                key={item}
              >
                <span
                  aria-hidden="true"
                  className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary"
                />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </Reveal>
      </Container>
    </section>
  );
}

export function MaintenanceSection() {
  return (
    <section
      className="bg-white py-14 sm:py-16 lg:py-24"
      data-section="maintenance"
      id="maintenance"
    >
      <Container>
        <Reveal>
          <SectionHeading
            description="Регулярная диагностика и согласованный график помогают планировать обслуживание оборудования без неожиданных действий со стороны сервиса."
            title="Плановое обслуживание коммерческого холода"
          />
        </Reveal>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {maintenanceItems.map((item) => (
            <article
              className="rounded-xl border border-slate-200 bg-white p-5 shadow-[0_8px_26px_rgba(15,23,42,0.04)]"
              key={item.title}
            >
              <span className="grid h-11 w-11 place-items-center rounded-md bg-primary/7 text-primary-ink">
                <LineIcon className="h-5 w-5" name={item.icon} />
              </span>
              <h3 className="mt-4 text-xl font-semibold text-navy">
                {item.title}
              </h3>
              <ul className="mt-3 space-y-2 text-base leading-6 text-slate-600">
                {item.points.map((point) => (
                  <li className="flex gap-3" key={point}>
                    <span
                      aria-hidden="true"
                      className="mt-2.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary"
                    />
                    {point}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
        <div className="mt-8 flex justify-center">
          <LeadCta
            className="inline-flex min-h-12 items-center justify-center rounded-md bg-primary px-6 py-3 text-base font-semibold text-white shadow-[0_10px_24px_rgba(23,107,255,0.2)] transition-colors duration-150 hover:bg-blue-700"
            intent="maintenance"
            sourceSection="maintenance"
          >
            Запросить обслуживание
          </LeadCta>
        </div>
      </Container>
    </section>
  );
}

export function ReviewsSection() {
  return (
    <span
      aria-hidden="true"
      className="block"
      data-section-anchor="reviews"
      id="reviews"
    />
  );
}

export function ContactSection() {
  return (
    <section
      className="relative bg-white py-14 sm:py-16 lg:py-24"
      data-section="request"
      id="request"
    >
      <span aria-hidden="true" className="absolute top-0" id="contact" />
      <Container className="grid gap-8 lg:grid-cols-[0.86fr_1.14fr] lg:items-stretch lg:gap-10">
        <div className="flex flex-col justify-center" id="contacts">
          <Reveal className="flex flex-col justify-center">
            <PlaceholderBadge>Каналы связи на проверке</PlaceholderBadge>
            <h2
              className="mt-4 text-[1.75rem] font-bold leading-tight tracking-[-0.035em] text-navy sm:text-4xl"
              data-lead-heading
              id="request-heading"
              tabIndex={-1}
            >
              Опишите неисправность — уточним задачу и доступность мастера
            </h2>
            <p className="mt-5 max-w-xl text-base leading-6 text-slate-600">
              Укажите оборудование, симптомы и район выезда. Тип обращения
              подставится автоматически из выбранного действия.
            </p>
            <div
              aria-label="Способы связи"
              className="mt-7 grid gap-3 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3"
              role="list"
            >
              {unavailableContactChannels.map((channel) => (
                <div
                  aria-label={`${channel.label}: канал ожидает подтверждения`}
                  className="flex min-h-16 items-center gap-3 rounded-lg border border-slate-200 bg-surface p-3"
                  key={channel.label}
                  role="listitem"
                >
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-white text-primary-ink shadow-sm">
                    <LineIcon className="h-5 w-5" name={channel.icon} />
                  </span>
                  <span className="min-w-0">
                    <strong className="block text-sm font-semibold text-navy">
                      {channel.label}
                    </strong>
                    <span className="mt-1 block text-[0.8125rem] leading-[1.125rem] text-slate-500">
                      Не опубликован
                    </span>
                  </span>
                </div>
              ))}
            </div>
          </Reveal>
        </div>

        <Reveal className="rounded-xl bg-navy p-5 text-white shadow-[0_18px_50px_rgba(11,18,32,0.18)] sm:p-7">
          <LeadForm />
        </Reveal>
      </Container>
    </section>
  );
}

export function LandingFooter() {
  return (
    <footer className="bg-navy py-10 text-slate-300" id="contacts-footer">
      <Container className="grid gap-8 border-b border-white/10 pb-8 md:grid-cols-[1.2fr_0.8fr_1fr]">
        <div>
          <div className="flex items-center gap-3 text-white">
            <span className="grid h-11 w-11 place-items-center rounded-md bg-primary/15 text-blue-200">
              <LineIcon name="snowflake" />
            </span>
            <div>
              <p className="text-base font-semibold">
                Сервис холодильного оборудования
              </p>
              <p className="mt-1 text-[0.8125rem] leading-[1.125rem] text-slate-400">
                Предпубликационный интерфейс
              </p>
            </div>
          </div>
          <p className="mt-4 max-w-sm text-sm leading-5 text-slate-400">
            Структура B2B-сайта готова к заполнению подтверждёнными контактами,
            ценами и юридическими документами.
          </p>
        </div>
        <nav aria-label="Навигация в подвале">
          <h2 className="text-sm font-semibold uppercase tracking-[0.12em] text-white">
            Навигация
          </h2>
          <ul className="mt-4 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            {navigationItems.map((item) => (
              <li key={item.href}>
                <a
                  className="inline-flex min-h-11 min-w-11 items-center transition-colors duration-150 hover:text-white"
                  href={item.href}
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-[0.12em] text-white">
            Документы и настройки
          </h2>
          <ul className="mt-4 space-y-1 text-sm text-slate-400">
            {unavailableLegalDocuments.map((document) => (
              <li id={document.id} key={document.id}>
                <span
                  aria-disabled="true"
                  className="inline-flex min-h-11 items-center"
                >
                  {document.label}
                  <span className="sr-only"> — документ не опубликован</span>
                </span>
              </li>
            ))}
            <li>
              <CookieSettingsButton />
            </li>
          </ul>
        </div>
      </Container>
      <Container className="flex flex-col gap-2 pt-6 text-[0.8125rem] leading-[1.125rem] text-slate-400 sm:flex-row sm:items-center sm:justify-between">
        <p>Предпубликационная версия. Не является публичной офертой.</p>
        <p>Реальные заявки принимает только same-origin production API.</p>
      </Container>
    </footer>
  );
}
