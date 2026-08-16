import Link from 'next/link';

export default function NotFound() {
  return (
    <main className="min-h-screen bg-surface px-6 py-16 text-navy">
      <div className="mx-auto max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.12em] text-navy">
          404
        </p>
        <h1 className="mt-4 text-4xl font-semibold">Страница не найдена</h1>
        <p className="mt-4 max-w-xl text-base text-slate-700">
          Такого маршрута нет в текущей версии сайта.
        </p>
        <Link
          className="mt-8 inline-flex rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          href="/"
        >
          Вернуться на главную
        </Link>
      </div>
    </main>
  );
}
