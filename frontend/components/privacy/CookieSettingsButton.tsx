'use client';

export function CookieSettingsButton() {
  return (
    <button
      className="min-h-11 text-left text-sm font-medium text-slate-300 underline decoration-white/25 underline-offset-4 transition-colors duration-150 hover:text-white hover:decoration-white"
      onClick={() => window.dispatchEvent(new Event('andrew:cookie-settings'))}
      type="button"
    >
      Настройки cookies
    </button>
  );
}
