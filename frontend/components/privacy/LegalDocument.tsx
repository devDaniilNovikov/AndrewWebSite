import type { ReactNode } from 'react';
import Link from 'next/link';
import { PreviewSiteFrame } from '../PreviewSiteFrame';
import { Container } from '../landing/PreviewPrimitives';

export function LegalDocument({
  title,
  subtitle,
  edition,
  children,
}: Readonly<{
  title: string;
  subtitle: string;
  edition: string;
  children: ReactNode;
}>) {
  return (
    <PreviewSiteFrame>
      <main className="bg-white py-10 sm:py-16" id="main-content" tabIndex={-1}>
        <Container>
          <article className="mx-auto max-w-4xl break-words text-base leading-7 text-slate-700 [&_a]:text-primary-ink [&_a]:underline [&_aside]:my-6 [&_aside]:rounded-lg [&_aside]:border [&_aside]:border-slate-200 [&_aside]:bg-surface [&_aside]:p-4 [&_h2]:mb-4 [&_h2]:mt-10 [&_h2]:text-xl [&_h2]:font-bold [&_h2]:text-navy [&_li]:pl-1 [&_p]:my-4 [&_td]:border [&_td]:border-slate-200 [&_td]:p-3 [&_td]:align-top [&_th]:border [&_th]:border-slate-200 [&_th]:bg-surface [&_th]:p-3 [&_th]:text-left [&_ul]:my-4 [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-6">
            <Link className="inline-flex min-h-11 items-center" href="/">
              На главную
            </Link>
            <h1 className="mt-6 text-3xl font-bold leading-tight tracking-tight text-navy sm:text-4xl">
              {title}
            </h1>
            <p>{subtitle}</p>
            <p className="text-sm text-slate-600">{edition}</p>
            {children}
          </article>
        </Container>
      </main>
    </PreviewSiteFrame>
  );
}
