import type { ReactNode } from 'react';
import { CookieConsent } from './privacy/CookieConsent';
import { LandingHeader } from './landing/LandingHeader';
import { LandingFooter } from './landing/TrustContactSections';

export function PreviewSiteFrame({
  children,
}: Readonly<{ children: ReactNode }>) {
  const isProductionBuild = process.env.NEXT_PUBLIC_BUILD_MODE === 'production';

  return (
    <>
      <a className="skip-link" href="#main-content">
        Перейти к содержимому
      </a>

      {isProductionBuild ? null : (
        <div
          className="border-b border-blue-200 bg-blue-50 px-5 py-2 text-center text-sm font-semibold leading-5 text-blue-950"
          role="status"
        >
          Предпубликационная версия · непроверенные контакты и коммерческие
          данные не используются
        </div>
      )}

      <LandingHeader />
      {children}
      <LandingFooter />
      <CookieConsent />
    </>
  );
}
