import localFont from 'next/font/local';

export const interVariable = localFont({
  display: 'swap',
  fallback: ['system-ui', 'sans-serif'],
  preload: true,
  src: './InterVariable-cyrillic.woff2',
  style: 'normal',
  variable: '--font-inter-variable',
  weight: '100 900',
});
