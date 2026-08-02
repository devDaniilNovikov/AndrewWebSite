import {
  pricingItems,
  processSteps,
  workPlaceholders,
} from '../../content/preview-content';
import {
  Container,
  LineIcon,
  MediaSlot,
  PlaceholderBadge,
  SectionHeading,
} from './PreviewPrimitives';
import { Reveal } from './Reveal';

export function WorksSection() {
  return (
    <section
      className="scroll-mt-24 bg-white py-10 sm:py-12"
      data-section="works"
      id="works"
    >
      <Container>
        <Reveal>
          <SectionHeading
            description="Карточки сохраняют плотность правильного макета, но не изображают вымышленные объекты, результаты или суммы."
            title="Выполненные работы"
          />
        </Reveal>
        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          {workPlaceholders.map((item) => (
            <article
              className="grid overflow-hidden rounded-lg border border-slate-200 bg-white shadow-[0_10px_34px_rgba(15,23,42,0.05)] sm:grid-cols-[0.45fr_0.55fr]"
              key={item.label}
            >
              <MediaSlot
                className="min-h-48 border-b border-slate-200 sm:min-h-56 sm:border-b-0 sm:border-r"
                icon={item.icon}
              />
              <div className="flex flex-col p-4">
                <PlaceholderBadge>Кейс — плейсхолдер</PlaceholderBadge>
                <h3 className="mt-2.5 text-base font-semibold text-navy">
                  {item.label}
                </h3>
                <dl className="mt-3 space-y-2 text-xs">
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-[0.08em] text-slate-600">
                      Неисправность
                    </dt>
                    <dd className="mt-1 text-slate-600">
                      Описание уточняется.
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-[0.08em] text-slate-600">
                      Результат
                    </dt>
                    <dd className="mt-1 text-slate-600">
                      Результат уточняется.
                    </dd>
                  </div>
                </dl>
                <div className="mt-3 flex items-end justify-between gap-3 border-t border-slate-100 pt-3">
                  <span>
                    <span className="block text-xs text-slate-600">
                      Стоимость работ
                    </span>
                    <strong className="mt-1 block text-sm text-navy">
                      Цена уточняется
                    </strong>
                  </span>
                  <a
                    className="text-xs font-semibold text-primary-ink"
                    href="#contact"
                  >
                    Оставить заявку
                  </a>
                </div>
              </div>
            </article>
          ))}
        </div>
      </Container>
    </section>
  );
}

export function RepairCallout() {
  return (
    <section
      className="bg-white pb-10 sm:pb-12"
      aria-label="Связаться по другой неисправности"
      data-section="repair"
    >
      <Container>
        <Reveal className="flex flex-col gap-6 rounded-xl bg-navy px-6 py-7 text-white shadow-[0_18px_50px_rgba(11,18,32,0.2)] md:flex-row md:items-center md:justify-between lg:px-9">
          <div className="flex gap-4">
            <span className="grid h-12 w-12 shrink-0 place-items-center rounded-full border border-blue-300/30 bg-white/5 text-blue-200">
              <LineIcon name="wrench" />
            </span>
            <div>
              <h2 className="text-xl font-semibold tracking-[-0.025em]">
                Не нашли похожую неисправность?
              </h2>
              <p className="mt-2 max-w-xl text-sm leading-6 text-slate-300">
                Опишите проблему в форме. В опубликованном preview отправка
                отключена, а локально допустимы только синтетические данные.
              </p>
            </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <a
              className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500"
              href="#contact"
            >
              Оставить заявку
            </a>
            <span className="inline-flex min-h-11 items-center justify-center gap-2 rounded-md border border-white/25 px-5 py-3 text-sm font-semibold text-slate-200">
              <LineIcon className="h-5 w-5" name="phone" />
              Телефон будет добавлен
            </span>
          </div>
        </Reveal>
      </Container>
    </section>
  );
}

export function PricingSection() {
  return (
    <section
      className="scroll-mt-24 bg-white pb-10 text-white sm:pb-12"
      data-section="pricing"
      id="pricing"
    >
      <Container>
        <div className="grid gap-8 rounded-xl bg-navy px-6 py-6 shadow-[0_18px_50px_rgba(11,18,32,0.18)] sm:px-8 lg:grid-cols-[0.72fr_1.28fr] lg:gap-12">
          <Reveal>
            <SectionHeading
              description="Фактические суммы не опубликованы. Финальная стоимость зависит от оборудования, неисправности, деталей и объёма работ."
              eyebrow="Прайс требует подтверждения"
              inverse
              title="Ориентиры по стоимости"
            />
            <div className="mt-5 flex items-start gap-3 rounded-lg border border-white/12 bg-white/[0.04] p-4 text-sm text-slate-300">
              <LineIcon className="shrink-0 text-blue-200" name="shield" />
              <p>
                Условия гарантии и цены будут добавлены после проверки
                владельцем.
              </p>
            </div>
          </Reveal>
          <div className="divide-y divide-white/10 border-y border-white/10">
            {pricingItems.map((item) => (
              <article
                className="grid gap-3 py-3 sm:grid-cols-[auto_1fr_auto] sm:items-center"
                key={item.title}
              >
                <span className="grid h-10 w-10 place-items-center rounded-md border border-white/15 bg-white/[0.04] text-blue-200">
                  <LineIcon className="h-5 w-5" name={item.icon} />
                </span>
                <div>
                  <h3 className="text-sm font-semibold text-white">
                    {item.title}
                  </h3>
                  <p className="mt-1 text-xs leading-5 text-slate-400">
                    {item.text}
                  </p>
                </div>
                <strong className="text-sm font-semibold text-blue-200 sm:text-right">
                  Цена уточняется
                </strong>
              </article>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
}

export function ProcessSection() {
  return (
    <section className="bg-white py-8 sm:py-10" data-section="process">
      <Container>
        <Reveal>
          <SectionHeading center title="Как проходит заявка" />
        </Reveal>
        <ol className="mt-7 grid gap-7 md:grid-cols-2 lg:grid-cols-4">
          {processSteps.map((step, index) => (
            <li
              className="relative border-t border-slate-200 pt-6"
              key={step.title}
            >
              <span className="absolute -top-4 left-0 grid h-8 w-8 place-items-center rounded-full bg-navy text-xs font-semibold text-white ring-4 ring-white">
                {index + 1}
              </span>
              <h3 className="text-sm font-semibold text-navy">{step.title}</h3>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                {step.text}
              </p>
            </li>
          ))}
        </ol>
        <div className="mt-6 flex justify-center">
          <a
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
            href="#contact"
          >
            Оставить заявку
          </a>
        </div>
      </Container>
    </section>
  );
}
