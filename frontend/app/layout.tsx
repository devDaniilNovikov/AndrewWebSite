import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Демонстрационная версия — сайт готовится',
  description: 'Техническая демонстрация будущего сайта без бизнес-контента.',
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
