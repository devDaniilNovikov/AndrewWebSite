import type { Metadata } from 'next';
import '@fontsource-variable/manrope';
import { interVariable } from './fonts';
import './globals.css';

const isProductionBuild = process.env.NEXT_PUBLIC_BUILD_MODE === 'production';

export const metadata: Metadata = {
  title: 'Ремонт коммерческого холодильного оборудования для бизнеса',
  description:
    'Ремонт и плановое обслуживание коммерческого холодильного оборудования для организаций.',
  openGraph: {
    description:
      'Ремонт и плановое обслуживание коммерческого холодильного оборудования для организаций.',
    locale: 'ru_RU',
    title: 'Ремонт коммерческого холодильного оборудования для бизнеса',
    type: 'website',
  },
  robots: {
    index: isProductionBuild,
    follow: isProductionBuild,
  },
  icons: {
    icon: 'data:,',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html className={interVariable.variable} lang="ru">
      <body className="bg-surface text-navy antialiased">{children}</body>
    </html>
  );
}
