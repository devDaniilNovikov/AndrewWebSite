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
import { Reveal } from './Reveal';

export function HeroSection() {
  return (
    <section
      className="overflow-hidden border-b border-slate-200 bg-white"
      data-section="hero"
    >
      <Container className="grid gap-7 py-8 lg:grid-cols-2 lg:items-stretch lg:gap-0 lg:py-0">
        <div className="flex flex-col justify-center lg:py-8 lg:pr-8 xl:pr-10">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary-ink">
            Москва и ближайшее Подмосковье
          </p>
          <h1 className="mt-4 max-w-2xl text-4xl font-semibold leading-[1.02] tracking-[-0.052em] text-navy sm:text-5xl lg:text-[3rem] xl:text-[3.15rem]">
            Ремонт коммерческого холодильного оборудования
          </h1>
          <p className="mt-5 max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
            Демонстрационная структура сайта для ремонта торгового и
            профессионального холодильного оборудования.
          </p>
          <p className="mt-3 max-w-xl text-sm leading-6 text-slate-500">
            Проверенные контакты, фотографии и коммерческие условия будут
            опубликованы после согласования.
          </p>
          <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:items-center">
            <a
              className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(23,107,255,0.2)] transition hover:bg-blue-700"
              href="#contact"
            >
              Оставить заявку
            </a>
            <span className="inline-flex min-h-11 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-600">
              <LineIcon className="h-5 w-5 text-primary-ink" name="phone" />
              Телефон будет добавлен
            </span>
          </div>
        </div>

        <div className="relative min-h-[22rem] overflow-hidden rounded-lg border border-slate-200 bg-slate-100 lg:min-h-[31rem] lg:rounded-none lg:border-y-0 lg:border-r-0">
          <MediaSlot
            className="h-full min-h-[22rem] lg:min-h-[31rem]"
            icon="snowflake"
            label="Фото будет добавлено"
          />
          <div className="absolute inset-x-5 bottom-5 rounded-lg border border-white/70 bg-white/90 p-4 shadow-lg backdrop-blur-sm">
            <PlaceholderBadge>Медиа-плейсхолдер</PlaceholderBadge>
            <p className="mt-2 text-sm font-semibold text-navy">
              Здесь появится лицензированная фотография оборудования или
              команды.
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
                <span className="block text-xs font-semibold text-navy">
                  {item.title}
                </span>
                <span className="mt-1 block text-[0.68rem] leading-4 text-slate-500">
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
      className="scroll-mt-24 bg-white py-10 sm:py-12"
      data-section="equipment"
      id="equipment"
    >
      <Container>
        <Reveal>
          <SectionHeading
            description="Категории повторяют композицию утверждённого макета. Точный перечень и фотографии проходят проверку."
            title="Оборудование, с которым мы работаем"
          />
        </Reveal>
        <div
          className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
          data-grid="equipment"
        >
          {equipmentItems.map((item, index) => (
            <div className="h-full" data-grid-item key={item.title}>
              <Reveal className="h-full" delay={index * 0.035}>
                <article className="flex h-full flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-[0_10px_35px_rgba(15,23,42,0.05)]">
                  <MediaSlot
                    className="min-h-36 border-b border-slate-200"
                    icon={item.icon}
                  />
                  <div className="flex flex-1 flex-col p-3.5">
                    <h3 className="text-base font-semibold tracking-[-0.025em] text-navy">
                      {item.title}
                    </h3>
                    <p className="mt-1.5 flex-1 text-xs leading-5 text-slate-600">
                      {item.text}
                    </p>
                    <div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 border-t border-slate-100 pt-3 text-[0.68rem] font-semibold text-primary-ink">
                      <a
                        className="inline-flex items-center gap-1"
                        href="#contact"
                      >
                        Описать неисправность{' '}
                        <LineIcon className="h-4 w-4" name="arrow" />
                      </a>
                      <a
                        className="inline-flex items-center gap-1"
                        href="#contact"
                      >
                        Оставить заявку{' '}
                        <LineIcon className="h-4 w-4" name="arrow" />
                      </a>
                    </div>
                  </div>
                </article>
              </Reveal>
            </div>
          ))}
        </div>
      </Container>
    </section>
  );
}

export function ServicesSection() {
  return (
    <section
      className="border-y border-slate-200 bg-surface py-8 sm:py-10"
      data-section="services"
    >
      <Container>
        <Reveal>
          <SectionHeading title="Услуги" />
        </Reveal>
        <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          {serviceItems.map((item) => (
            <article
              className="rounded-lg border border-slate-200 bg-white p-4 shadow-[0_8px_28px_rgba(15,23,42,0.04)]"
              key={item.title}
            >
              <span className="grid h-9 w-9 place-items-center rounded-lg border border-primary/15 bg-primary/5 text-primary-ink">
                <LineIcon className="h-5 w-5" name={item.icon} />
              </span>
              <h3 className="mt-3 text-sm font-semibold text-navy">
                {item.title}
              </h3>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                {item.text}
              </p>
            </article>
          ))}
        </div>
        <div className="mt-5 flex justify-center">
          <a
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
            href="#contact"
          >
            Описать неисправность
          </a>
        </div>
      </Container>
    </section>
  );
}
