import { navigationItems } from '../../content/preview-content';
import { MobileNavigation } from './MobileNavigation';
import { LineIcon } from './PreviewPrimitives';

export function LandingHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 shadow-[0_1px_0_rgba(15,23,42,0.02)] backdrop-blur-md lg:static">
      <div className="mx-auto flex min-h-18 w-full max-w-[72rem] items-center gap-3 px-5 sm:gap-4 sm:px-7 lg:px-8">
        <a
          aria-label="На главную"
          className="flex min-w-0 items-center gap-3 text-navy"
          href="#main-content"
        >
          <span className="grid size-10 shrink-0 place-items-center rounded-md bg-primary/10 text-primary-ink">
            <LineIcon name="snowflake" />
          </span>
          <span className="min-w-0">
            <span className="block truncate text-sm font-semibold leading-5">
              Название компании уточняется
            </span>
            <span className="hidden text-[0.65rem] text-slate-500 sm:block">
              Ремонт холодильного оборудования
            </span>
          </span>
        </a>

        <nav
          aria-label="Основная навигация"
          className="ml-auto hidden lg:block"
        >
          <ul className="flex items-center gap-6 text-xs font-semibold text-slate-600">
            {navigationItems.map((item) => (
              <li key={item.href}>
                <a
                  className="inline-flex min-h-11 items-center transition-colors hover:text-primary-ink"
                  href={item.href}
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="ml-auto hidden shrink-0 items-center gap-3 lg:flex">
          <span className="text-right">
            <span className="block text-xs font-semibold text-navy">
              Телефон будет добавлен
            </span>
            <span className="mt-1 block text-[0.65rem] text-slate-500">
              Часы работы уточняются
            </span>
          </span>
          <a
            className="inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-blue-700"
            href="#contact"
          >
            Оставить заявку
          </a>
        </div>

        <a
          aria-label="Оставить заявку — мобильная версия"
          className="ml-auto inline-flex min-h-11 shrink-0 items-center justify-center rounded-md bg-primary px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-blue-700 lg:hidden"
          href="#contact"
        >
          Заявка
        </a>

        <div className="lg:hidden">
          <MobileNavigation items={navigationItems} />
        </div>
      </div>
    </header>
  );
}
