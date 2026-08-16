import {
  benefitItems,
  equipmentItems,
  serviceItems,
} from '../../content/preview-content';
import {
  Container,
  LineIcon,
  MediaSlot,
  PlaceholderBadge,
  SectionHeading,
} from './PreviewPrimitives';
import { EquipmentCard } from './EquipmentCard';
import { LeadCta } from './LeadCta';
import { Reveal } from './Reveal';

export function HeroSection() {
  return (
    <section
      className="overflow-hidden border-b border-slate-200 bg-white"
      data-section="hero"
    >
      <Container className="grid gap-7 py-8 lg:grid-cols-2 lg:items-stretch lg:gap-0 lg:py-0">
        <div className="flex flex-col justify-center lg:py-8 lg:pr-8 xl:pr-10">
          <p className="text-sm font-semibold uppercase leading-5 tracking-[0.16em] text-primary-ink">
            Сервис для организаций
          </p>
          <h1 className="mt-4 max-w-2xl text-[2rem] font-semibold leading-[1.08] tracking-[-0.052em] text-navy sm:text-[2.625rem] lg:text-[3.25rem]">
            Ремонт коммерческого холодильного оборудования
          </h1>
          <p className="mt-5 max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
            Диагностика, ремонт и плановое обслуживание торгового и
            профессионального холодильного оборудования.
          </p>
          <p className="mt-3 max-w-xl text-base leading-6 text-slate-500">
            Опишите симптомы — подберём формат обращения и уточним следующие
            шаги после проверки доступности мастера.
          </p>
          <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:items-center">
            <LeadCta
              className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white shadow-[0_10px_24px_rgba(23,107,255,0.2)] transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
              sourceSection="hero"
            >
              Оставить заявку
            </LeadCta>
            <a
              className="inline-flex min-h-11 items-center justify-center rounded-md border border-slate-300 bg-white px-5 py-3 text-base font-semibold text-navy transition-colors duration-150 hover:border-primary hover:text-primary-ink"
              href="#equipment"
            >
              Выбрать оборудование
            </a>
          </div>
        </div>

        <div className="relative min-h-[22rem] overflow-hidden rounded-lg border border-slate-200 bg-slate-100 lg:min-h-[31rem] lg:rounded-none lg:border-y-0 lg:border-r-0">
          <MediaSlot
            className="h-full min-h-[22rem] lg:min-h-[31rem]"
            icon="snowflake"
            label="Коммерческое холодильное оборудование"
          />
          <div className="absolute inset-x-5 bottom-5 rounded-lg border border-white/70 bg-white/90 p-4 shadow-lg backdrop-blur-sm">
            <PlaceholderBadge>Сервисная модель</PlaceholderBadge>
            <p className="mt-2 text-base font-semibold leading-6 text-navy">
              Диагностика задачи, подбор специалиста и контроль результата.
            </p>
          </div>
        </div>
      </Container>
    </section>
  );
}

export function BenefitStrip() {
  return (
    <section
      aria-label="Преимущества"
      className="border-b border-slate-200 bg-white"
      data-section="benefits"
    >
      <Container>
        <ul className="grid sm:grid-cols-2 lg:grid-cols-4">
          {benefitItems.map((item) => (
            <li
              className="flex gap-3 border-b border-slate-200 py-4 sm:px-4 sm:[&:nth-child(odd)]:border-r lg:border-b-0 lg:border-r lg:first:pl-0 lg:last:border-r-0 lg:last:pr-0"
              key={item.title}
            >
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-primary/7 text-primary-ink">
                <LineIcon className="h-5 w-5" name={item.icon} />
              </span>
              <span>
                <span className="block text-base font-semibold text-navy">
                  {item.title}
                </span>
                <span className="mt-1 block text-sm leading-5 text-slate-500">
                  {item.text}
                </span>
              </span>
            </li>
          ))}
        </ul>
      </Container>
    </section>
  );
}

export function EquipmentSection() {
  return (
    <section
      className="bg-white py-14 sm:py-16 lg:py-20"
      data-section="equipment"
      id="equipment"
    >
      <Container>
        <Reveal>
          <SectionHeading
            description="Выберите категорию, чтобы посмотреть частые симптомы и примеры диагностических работ."
            title="Оборудование, с которым мы работаем"
          />
        </Reveal>
        <div
          className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
          data-grid="equipment"
        >
          {equipmentItems.map((item, index) => (
            <div className="h-full" data-grid-item key={item.id}>
              <Reveal className="h-full" delay={index * 0.035}>
                <EquipmentCard item={item} />
              </Reveal>
            </div>
          ))}
        </div>
        <div className="mt-8 flex flex-col gap-4 rounded-xl border border-primary/15 bg-primary/5 p-5 sm:flex-row sm:items-center sm:justify-between lg:p-6">
          <p className="max-w-2xl text-base leading-6 text-slate-600">
            Не нашли свой тип оборудования? Опишите симптомы — состав работ
            определим после диагностики.
          </p>
          <LeadCta
            className="inline-flex min-h-11 shrink-0 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection="equipment"
          >
            Описать неисправность
          </LeadCta>
        </div>
      </Container>
    </section>
  );
}

export function ServicesSection() {
  return (
    <section
      className="border-y border-slate-200 bg-surface py-14 sm:py-16 lg:py-20"
      data-section="services"
      id="services"
    >
      <Container>
        <Reveal>
          <SectionHeading title="Услуги" />
        </Reveal>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          {serviceItems.map((item) => (
            <article
              className="rounded-lg border border-slate-200 bg-white p-5 shadow-[0_8px_28px_rgba(15,23,42,0.04)]"
              key={item.title}
            >
              <span className="grid h-9 w-9 place-items-center rounded-lg border border-primary/15 bg-primary/5 text-primary-ink">
                <LineIcon className="h-5 w-5" name={item.icon} />
              </span>
              <h3 className="mt-4 text-lg font-semibold text-navy">
                {item.title}
              </h3>
              <p className="mt-2 text-base leading-6 text-slate-600">
                {item.text}
              </p>
            </article>
          ))}
        </div>
        <div className="mt-8 flex justify-center">
          <LeadCta
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection="services"
          >
            Описать неисправность
          </LeadCta>
        </div>
      </Container>
    </section>
  );
}
