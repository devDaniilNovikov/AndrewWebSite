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
import { LeadCta } from './LeadCta';
import { Reveal } from './Reveal';

export function WorksSection() {
  return (
    <section
      className="bg-white py-14 sm:py-16 lg:py-20"
      data-section="works"
      id="works"
    >
      <Container>
        <Reveal>
          <SectionHeading
            description="Формат карточек готов для подтверждённых объектов, неисправностей, результатов и стоимости."
            title="Выполненные работы"
          />
        </Reveal>
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {workPlaceholders.map((item) => (
            <article
              aria-labelledby={`${item.id}-title`}
              className="flex flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-[0_10px_34px_rgba(15,23,42,0.05)]"
              id={item.id}
              key={item.id}
            >
              <MediaSlot
                className="min-h-40 w-full border-b border-slate-200"
                icon={item.icon}
                label={item.label}
              />
              <div className="flex min-w-0 flex-1 flex-col p-5">
                <PlaceholderBadge>Структура кейса</PlaceholderBadge>
                <h3
                  className="mt-3 text-lg font-semibold leading-6 text-navy"
                  id={`${item.id}-title`}
                >
                  {item.label}
                </h3>
                <dl className="mt-4 space-y-3">
                  <div>
                    <dt className="text-sm font-semibold text-navy">
                      Неисправность
                    </dt>
                    <dd className="mt-1 text-base leading-6 text-slate-600">
                      Данные не опубликованы.
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm font-semibold text-navy">
                      Результат
                    </dt>
                    <dd className="mt-1 text-base leading-6 text-slate-600">
                      Данные не опубликованы.
                    </dd>
                  </div>
                </dl>
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
      className="bg-white pb-14 sm:pb-16 lg:pb-20"
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
              <h2 className="text-2xl font-semibold tracking-[-0.025em] sm:text-3xl">
                Не нашли похожую неисправность?
              </h2>
              <p className="mt-3 max-w-xl text-base leading-6 text-slate-300">
                Опишите оборудование и симптомы — контекст обращения
                автоматически попадёт в форму.
              </p>
            </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <LeadCta
              className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-500 motion-reduce:transition-none"
              sourceSection="works"
            >
              Оставить заявку
            </LeadCta>
            <a
              className="inline-flex min-h-11 items-center justify-center rounded-md border border-white/25 px-5 py-3 text-base font-semibold text-slate-200 transition-colors duration-150 hover:border-white/50 hover:text-white"
              href="#equipment"
            >
              Выбрать оборудование
            </a>
          </div>
        </Reveal>
      </Container>
    </section>
  );
}

export function PricingSection() {
  return (
    <section
      className="bg-white py-14 text-white sm:py-16 lg:py-20"
      data-section="pricing"
      id="pricing"
    >
      <Container>
        <div className="grid gap-8 rounded-xl bg-navy px-6 py-6 shadow-[0_18px_50px_rgba(11,18,32,0.18)] sm:px-8 lg:grid-cols-[0.72fr_1.28fr] lg:gap-12">
          <Reveal>
            <SectionHeading
              description="Финальная стоимость зависит от оборудования, неисправности, деталей и согласованного объёма работ."
              eyebrow="Расчёт после диагностики"
              inverse
              title="Ориентиры по стоимости"
            />
            <div className="mt-6 flex items-start gap-3 rounded-lg border border-white/12 bg-white/[0.04] p-4 text-base leading-6 text-slate-300">
              <LineIcon className="shrink-0 text-blue-200" name="shield" />
              <p>
                До начала основных работ согласовываем состав работ и итоговую
                смету.
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
                  <h3 className="text-lg font-semibold text-white">
                    {item.title}
                  </h3>
                  <p className="mt-1 text-base leading-6 text-slate-300">
                    {item.text}
                  </p>
                </div>
                <strong className="text-base font-semibold text-blue-200 sm:text-right">
                  После диагностики
                </strong>
              </article>
            ))}
          </div>
        </div>
        <div className="mt-6 flex flex-col gap-4 rounded-xl border border-slate-200 bg-surface p-5 text-navy sm:flex-row sm:items-center sm:justify-between lg:p-6">
          <p className="max-w-2xl text-base leading-6 text-slate-600">
            Итог зависит от оборудования, неисправности, деталей и объёма работ.
            Уточним после диагностики.
          </p>
          <LeadCta
            className="inline-flex min-h-11 shrink-0 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection="pricing"
          >
            Уточнить стоимость
          </LeadCta>
        </div>
      </Container>
    </section>
  );
}

export function ProcessSection() {
  return (
    <section
      className="bg-white py-14 sm:py-16 lg:py-20"
      data-section="process"
      id="process"
    >
      <Container>
        <Reveal>
          <SectionHeading center title="Как проходит заявка" />
        </Reveal>
        <ol className="mt-9 grid gap-8 md:grid-cols-2 lg:grid-cols-4">
          {processSteps.map((step, index) => (
            <li
              className="relative border-t border-slate-200 pt-6"
              key={step.title}
            >
              <span className="absolute -top-5 left-0 grid h-10 w-10 place-items-center rounded-full bg-navy text-sm font-semibold text-white ring-4 ring-white">
                {index + 1}
              </span>
              <h3 className="text-lg font-semibold text-navy">{step.title}</h3>
              <p className="mt-2 text-base leading-6 text-slate-600">
                {step.text}
              </p>
            </li>
          ))}
        </ol>
        <div className="mt-8 flex justify-center">
          <LeadCta
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection="process"
          >
            Оставить заявку
          </LeadCta>
        </div>
      </Container>
    </section>
  );
}
