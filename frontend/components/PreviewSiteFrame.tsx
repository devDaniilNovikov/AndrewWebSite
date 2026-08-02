import type { ReactNode } from 'react';
import { LandingHeader } from './landing/LandingHeader';
import { LandingFooter } from './landing/TrustContactSections';

export function PreviewSiteFrame({
  children,
}: Readonly<{ children: ReactNode }>) {
  return (
    <>
      <a className="skip-link" href="#main-content">
        Перейти к содержимому
      </a>

      <div
        className="border-b border-blue-200 bg-blue-50 px-5 py-2 text-center text-xs font-semibold text-blue-950"
        role="status"
      >
        Демонстрационная версия · контакты, цены, отзывы и фотографии заменены
        плейсхолдерами
      </div>

      <LandingHeader />
      {children}
      <LandingFooter />
    </>
  );
}
