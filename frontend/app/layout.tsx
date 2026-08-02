import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Ремонт коммерческого холодильного оборудования — демонстрация',
  description:
    'Демонстрационная структура сайта по ремонту коммерческого холодильного оборудования без неподтверждённых бизнес-данных.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru">
      <body className="bg-surface text-navy antialiased">{children}</body>
    </html>
  );
}
