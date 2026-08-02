import Link from 'next/link';
import type { ProductPageDefinition } from '../../content/product-pages';
import { PreviewSiteFrame } from '../PreviewSiteFrame';
import { ContactSection } from '../landing/TrustContactSections';
import {
  Container,
  LineIcon,
  MediaSlot,
  PlaceholderBadge,
  SectionHeading,
} from '../landing/PreviewPrimitives';
import { Reveal } from '../landing/Reveal';

function sectionClasses(
  tone: ProductPageDefinition['sections'][number]['tone'],
) {
  if (tone === 'surface') {
    return 'border-y border-slate-200 bg-surface py-10 sm:py-12';
  }

  return 'bg-white py-10 sm:py-12';
}

export function ProvisionalProductPage({
  page,
}: Readonly<{ page: ProductPageDefinition }>) {
  return (
    <PreviewSiteFrame>
      <main data-product-path={page.path} id="main-content" tabIndex={-1}>
        <section className="overflow-hidden border-b border-slate-200 bg-white">
          <Container className="grid gap-7 py-8 lg:grid-cols-2 lg:items-stretch lg:gap-0 lg:py-0">
            <div className="flex flex-col justify-center lg:py-10 lg:pr-10">
              <PlaceholderBadge>Демонстрационная страница</PlaceholderBadge>
              <p className="mt-5 text-xs font-semibold uppercase tracking-[0.16em] text-primary-ink">
                {page.eyebrow}
              </p>
              <h1 className="mt-3 max-w-3xl text-4xl font-semibold leading-[1.04] tracking-[-0.05em] text-navy sm:text-5xl lg:text-[3rem]">
                {page.title}
              </h1>
              <p className="mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg">
                {page.description}
              </p>
              <p className="mt-3 max-w-xl text-sm leading-6 text-slate-500">
                Неподтверждённые коммерческие сведения отмечены плейсхолдерами и
                не используются для production-сборки.
              </p>
              <div className="mt-7 flex flex-col gap-3 sm:flex-row">
                <Link
                  className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-3 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(23,107,255,0.2)] hover:bg-blue-700"
                  href="#contact"
                >
                  Оставить заявку
                </Link>
                <Link
                  className="inline-flex min-h-11 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:border-primary hover:text-primary-ink"
                  href="/"
                >
                  Вернуться на главную
                </Link>
              </div>
            </div>

            <MediaSlot
              className="min-h-[22rem] border border-slate-200 lg:min-h-[30rem] lg:border-y-0 lg:border-r-0"
              icon={page.mediaIcon}
              label={page.mediaLabel}
              sizes="(min-width: 1024px) 50vw, 100vw"
            />
          </Container>
        </section>

        {page.sections.map((section) => {
          const inverse = section.tone === 'dark';
          const content = (
            <>
              <Reveal>
                <SectionHeading
                  description={section.description}
                  inverse={inverse}
                  title={section.title}
                />
              </Reveal>
              <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {section.items.map((item, index) => (
                  <Reveal delay={index * 0.035} key={item.title}>
                    <article
                      className={`flex h-full flex-col rounded-lg border p-5 ${
                        inverse
                          ? 'border-white/12 bg-white/[0.04] text-white'
                          : 'border-slate-200 bg-white text-navy shadow-[0_10px_34px_rgba(15,23,42,0.05)]'
                      }`}
                    >
                      <span
                        className={`grid h-10 w-10 place-items-center rounded-md ${
                          inverse
                            ? 'border border-white/15 bg-white/[0.04] text-blue-200'
                            : 'bg-primary/7 text-primary-ink'
                        }`}
                      >
                        <LineIcon className="h-5 w-5" name={item.icon} />
                      </span>
                      <h3 className="mt-4 text-base font-semibold">
                        {item.title}
                      </h3>
                      <p
                        className={`mt-2 flex-1 text-sm leading-6 ${
                          inverse ? 'text-slate-300' : 'text-slate-600'
                        }`}
                      >
                        {item.text}
                      </p>
                      {item.status === 'placeholder' ? (
                        <div className="mt-4">
                          <PlaceholderBadge inverse={inverse}>
                            Данные уточняются
                          </PlaceholderBadge>
                        </div>
                      ) : null}
                      {item.href ? (
                        <Link
                          className={`mt-4 inline-flex min-h-10 items-center gap-1 text-sm font-semibold ${
                            inverse ? 'text-blue-200' : 'text-primary-ink'
                          }`}
                          href={item.href}
                        >
                          {item.linkLabel ?? 'Подробнее'}
                          <LineIcon className="h-4 w-4" name="arrow" />
                        </Link>
                      ) : null}
                    </article>
                  </Reveal>
                ))}
              </div>
            </>
          );

          if (inverse) {
            return (
              <section
                className="bg-white py-5 sm:py-6"
                data-product-section={section.title}
                key={section.title}
              >
                <Container>
                  <div className="rounded-xl bg-navy px-6 py-8 text-white shadow-[0_18px_50px_rgba(11,18,32,0.18)] sm:px-8">
                    {content}
                  </div>
                </Container>
              </section>
            );
          }

          return (
            <section
              className={sectionClasses(section.tone)}
              data-product-section={section.title}
              key={section.title}
            >
              <Container>{content}</Container>
            </section>
          );
        })}

        <ContactSection />
      </main>
    </PreviewSiteFrame>
  );
}
