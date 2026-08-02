import {
  maintenanceItems,
  navigationItems,
  reviewPlaceholders,
} from '../../content/preview-content';
import { LeadForm } from '../leads/LeadForm';
import {
  Container,
  LineIcon,
  MediaSlot,
  PlaceholderBadge,
  SectionHeading,
} from './PreviewPrimitives';
import { Reveal } from './Reveal';

export function AboutSection() {
  return (
    <section
      className="scroll-mt-24 border-y border-slate-200 bg-surface py-8 sm:py-10"
      data-section="about"
      id="about"
    >
      <Container className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[1.15fr_0.85fr_0.62fr_0.62fr] lg:items-stretch">
        <Reveal className="overflow-hidden rounded-xl border border-slate-200 bg-white">
          <MediaSlot
            className="h-full min-h-72"
            icon="team"
            label="Фото будет добавлено"
          />
        </Reveal>
        <Reveal className="rounded-xl border border-slate-200 bg-white p-5 sm:p-6">
          <SectionHeading eyebrow="О компании" title="Команда мастеров" />
          <p className="mt-4 text-sm leading-6 text-slate-600">
            Профессиональная команда для задач коммерческого холодильного
            оборудования.
          </p>
          <p className="mt-3 text-xs leading-5 text-slate-500">
            Имена, опыт, роли и фотографии специалистов будут опубликованы
            только после подтверждения.
          </p>
        </Reveal>
        {['Профиль специалиста', 'Профиль специалиста'].map((title, index) => (
          <article
            className="overflow-hidden rounded-xl border border-slate-200 bg-white"
            key={`${title}-${index}`}
          >
            <MediaSlot
              className="min-h-40 border-b border-slate-200"
              icon="team"
              label="Фото будет добавлено"
            />
            <div className="p-4">
              <h3 className="text-sm font-semibold text-navy">{title}</h3>
              <p className="mt-1 text-xs leading-5 text-slate-500">
                Имя и опыт уточняются
              </p>
              <div className="mt-3">
                <PlaceholderBadge>Данные сотрудника</PlaceholderBadge>
              </div>
            </div>
          </article>
        ))}
      </Container>
    </section>
  );
}

export function MaintenanceSection() {
  return (
    <section className="bg-white py-8 sm:py-10" data-section="maintenance">
      <Container>
        <Reveal>
          <SectionHeading
            description="Регулярное обслуживание остаётся частью предложения для организаций, а конкретный состав и условия ещё согласовываются."
            title="Плановое обслуживание коммерческого холода"
          />
        </Reveal>
        <div className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-[repeat(4,minmax(0,1fr))_1.15fr]">
          {maintenanceItems.map((item) => (
            <article
              className="rounded-lg border border-slate-200 bg-white p-3 shadow-[0_8px_26px_rgba(15,23,42,0.04)]"
              key={item.title}
            >
              <span className="grid h-8 w-8 place-items-center rounded-md bg-primary/7 text-primary-ink">
                <LineIcon className="h-5 w-5" name={item.icon} />
              </span>
              <h3 className="mt-2 text-sm font-semibold text-navy">
                {item.title}
              </h3>
              <ul className="mt-2 space-y-1 text-xs leading-4 text-slate-600">
                {item.points.map((point) => (
                  <li className="flex gap-2" key={point}>
                    <span
                      aria-hidden="true"
                      className="mt-2 h-1 w-1 shrink-0 rounded-full bg-primary"
                    />
                    {point}
                  </li>
                ))}
              </ul>
              {item.title === 'Как отправить запрос' ? (
                <a
                  className="mt-3 inline-flex min-h-10 w-full items-center justify-center rounded-md bg-primary px-3 py-2 text-center text-xs font-semibold text-white hover:bg-blue-700"
                  href="#contact"
                >
                  Обсудить обслуживание объекта
                </a>
              ) : null}
            </article>
          ))}
          <MediaSlot
            className="min-h-44 rounded-lg border border-slate-200"
            icon="wrench"
            label="Фото будет добавлено"
          />
        </div>
      </Container>
    </section>
  );
}

export function ReviewsSection() {
  return (
    <section
      className="border-y border-slate-200 bg-surface py-8 sm:py-10"
      data-section="reviews"
    >
      <Container>
        <Reveal>
          <SectionHeading title="Отзывы клиентов" />
        </Reveal>
        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          {reviewPlaceholders.map((label) => (
            <article
              className="rounded-lg border border-slate-200 bg-white p-5"
              key={label}
            >
              <div className="flex items-center gap-3">
                <span className="grid h-11 w-11 place-items-center rounded-full bg-slate-100 text-primary-ink">
                  <LineIcon name="message" />
                </span>
                <div>
                  <h3 className="text-sm font-semibold text-navy">{label}</h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Источник будет указан
                  </p>
                </div>
              </div>
              <p className="mt-4 border-l-2 border-primary/30 pl-4 text-sm leading-6 text-slate-600">
                Отзыв ожидает подтверждения
              </p>
              <div className="mt-4">
                <PlaceholderBadge>Текст не опубликован</PlaceholderBadge>
              </div>
            </article>
          ))}
        </div>
      </Container>
    </section>
  );
}

export function ContactSection() {
  return (
    <section
      className="scroll-mt-24 bg-white py-8 sm:py-10"
      data-section="contact"
      id="contact"
    >
      <Container className="grid gap-6 lg:grid-cols-[0.82fr_1.18fr] lg:items-stretch">
        <Reveal className="flex flex-col justify-center">
          <PlaceholderBadge>Контакты уточняются</PlaceholderBadge>
          <h2 className="mt-4 text-3xl font-semibold tracking-[-0.04em] text-navy">
            Опишите поломку — свяжемся и уточним детали
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-6 text-slate-600 sm:text-base">
            В опубликованной демонстрации данные не отправляются. Локальная
            тестовая форма принимает только синтетические данные, а проверенные
            контакты появятся после согласования.
          </p>
          <dl className="mt-5 divide-y divide-slate-200 border-y border-slate-200 text-sm">
            <div className="grid gap-1 py-3 sm:grid-cols-[8rem_1fr]">
              <dt className="font-semibold text-navy">Телефон</dt>
              <dd className="text-slate-600">Телефон будет добавлен</dd>
            </div>
            <div className="grid gap-1 py-3 sm:grid-cols-[8rem_1fr]">
              <dt className="font-semibold text-navy">Регион</dt>
              <dd className="text-slate-600">Москва и ближайшее Подмосковье</dd>
            </div>
            <div className="grid gap-1 py-3 sm:grid-cols-[8rem_1fr]">
              <dt className="font-semibold text-navy">График</dt>
              <dd className="text-slate-600">Часы работы уточняются</dd>
            </div>
          </dl>
        </Reveal>

        <Reveal className="rounded-xl bg-navy p-4 text-white shadow-[0_18px_50px_rgba(11,18,32,0.18)] sm:p-5">
          <LeadForm />
        </Reveal>
      </Container>
    </section>
  );
}

export function LandingFooter() {
  return (
    <footer className="bg-navy py-8 text-slate-300">
      <Container className="grid gap-6 border-b border-white/10 pb-6 md:grid-cols-[1.3fr_1fr_1fr]">
        <div>
          <div className="flex items-center gap-3 text-white">
            <span className="grid h-10 w-10 place-items-center rounded-md bg-primary/15 text-blue-200">
              <LineIcon name="snowflake" />
            </span>
            <div>
              <p className="text-sm font-semibold">
                Название компании уточняется
              </p>
              <p className="mt-1 text-xs text-slate-400">
                Демонстрационная версия
              </p>
            </div>
          </div>
          <p className="mt-4 max-w-sm text-xs leading-5 text-slate-400">
            Структура сайта по ремонту коммерческого холодильного оборудования.
            Фактические сведения будут опубликованы после проверки.
          </p>
        </div>
        <nav aria-label="Навигация в подвале">
          <h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-white">
            Навигация
          </h2>
          <ul className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
            {navigationItems.map((item) => (
              <li key={item.href}>
                <a className="hover:text-white" href={item.href}>
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <div>
          <h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-white">
            Контакты
          </h2>
          <dl className="mt-4 space-y-3 text-xs text-slate-400">
            <div>
              <dt className="sr-only">Телефон</dt>
              <dd>Телефон будет добавлен</dd>
            </div>
            <div>
              <dt className="sr-only">График</dt>
              <dd>Часы работы уточняются</dd>
            </div>
            <div>
              <dt className="sr-only">Юридическая информация</dt>
              <dd>Реквизиты и политика будут добавлены</dd>
            </div>
          </dl>
        </div>
      </Container>
      <Container className="flex flex-col gap-2 pt-5 text-[0.68rem] text-slate-400 sm:flex-row sm:items-center sm:justify-between">
        <p>Демонстрационный frontend-preview. Не является публичной офертой.</p>
        <p>Сбор данных, аналитика и внешние запросы отключены.</p>
      </Container>
    </footer>
  );
}
