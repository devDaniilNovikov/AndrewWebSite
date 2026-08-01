export function PreviewShell() {
  return (
    <main className="grid min-h-screen place-items-center bg-surface px-6 py-16 text-navy">
      <section
        aria-labelledby="preview-title"
        className="w-full max-w-3xl rounded-3xl border border-slate-200 bg-white p-8 shadow-sm sm:p-12"
      >
        <p className="inline-flex rounded-full bg-primary/10 px-4 py-2 text-sm font-semibold text-navy ring-1 ring-primary/20">
          Демонстрационная версия
        </p>
        <h1
          className="mt-6 text-4xl font-semibold leading-tight tracking-tight sm:text-5xl"
          id="preview-title"
        >
          Сайт готовится к наполнению
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-7 text-slate-700 sm:text-lg">
          Это технический preview без бизнес-контента. Проверенные материалы
          появятся в следующих этапах работы.
        </p>
        <p className="mt-4 text-sm leading-6 text-slate-600">
          Формы и сбор данных отключены.
        </p>
      </section>
    </main>
  );
}
