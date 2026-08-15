import Image from 'next/image';
import type { PreviewIcon } from '../../content/preview-content';

export type VerifiedLocalPhoto = Readonly<{
  alt: string;
  src: `/media/verified/${string}`;
}>;

export function Container({
  children,
  className = '',
}: Readonly<{ children: React.ReactNode; className?: string }>) {
  return (
    <div
      className={`mx-auto w-full max-w-[72rem] px-5 sm:px-7 lg:px-8 ${className}`}
    >
      {children}
    </div>
  );
}

export function PlaceholderBadge({
  children,
  inverse = false,
}: Readonly<{ children: React.ReactNode; inverse?: boolean }>) {
  return (
    <span
      className={`inline-flex w-fit items-center rounded-full border px-2.5 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.1em] ${
        inverse
          ? 'border-blue-200/30 bg-blue-200/10 text-blue-100'
          : 'border-primary/20 bg-primary/5 text-primary-ink'
      }`}
    >
      {children}
    </span>
  );
}

export function SectionHeading({
  title,
  description,
  eyebrow,
  inverse = false,
  center = false,
}: Readonly<{
  title: string;
  description?: string;
  eyebrow?: string;
  inverse?: boolean;
  center?: boolean;
}>) {
  return (
    <div className={`max-w-3xl ${center ? 'mx-auto text-center' : ''}`}>
      {eyebrow ? (
        <p
          className={`text-[0.8125rem] font-semibold uppercase leading-[1.125rem] tracking-[0.14em] ${inverse ? 'text-blue-200' : 'text-primary-ink'}`}
        >
          {eyebrow}
        </p>
      ) : null}
      <h2
        className={`mt-2 text-[1.75rem] font-bold leading-tight tracking-[-0.035em] sm:text-3xl lg:text-4xl ${inverse ? 'text-white' : 'text-navy'}`}
      >
        {title}
      </h2>
      {description ? (
        <p
          className={`mt-4 max-w-2xl text-base leading-6 ${center ? 'mx-auto' : ''} ${inverse ? 'text-slate-300' : 'text-slate-600'}`}
        >
          {description}
        </p>
      ) : null}
    </div>
  );
}

export function LineIcon({
  name,
  className = '',
}: Readonly<{ name: PreviewIcon; className?: string }>) {
  const commonProps = {
    'aria-hidden': true,
    className: `h-6 w-6 ${className}`,
    fill: 'none',
    viewBox: '0 0 24 24',
  } as const;

  const paths: Partial<Record<PreviewIcon, React.ReactNode>> = {
    arrow: (
      <path
        d="m8 5 7 7-7 7"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    ),
    building: (
      <path
        d="M4 20V7l8-3v16M4 10h8m-5 3h2m-2 3h2m6-6h5v10h-8m4-7h1m-1 3h1"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    calendar: (
      <path
        d="M5 5h14v15H5zM8 3v4m8-4v4M5 9h14m-10 4 2 2 4-4"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    clipboard: (
      <path
        d="M8 5h8m-7-2h6v4H9zM6 5H4v16h16V5h-2M8 11h8m-8 4h8"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    close: (
      <path
        d="m6 6 12 12M18 6 6 18"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
    ),
    fan: (
      <path
        d="M12 12c-5-1-6-4-4-6 2-2 4 0 4 6Zm0 0c1-5 4-6 6-4 2 2 0 4-6 4Zm0 0c5 1 6 4 4 6-2 2-4 0-4-6Zm0 0c-1 5-4 6-6 4-2-2 0-4 6-4Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.4"
      />
    ),
    gear: (
      <path
        d="M9.5 4.5 10 2h4l.5 2.5 2 .8 2.1-1.4 2.8 2.8L20 8.8l.8 2 2.2.5v4l-2.2.5-.8 2 1.4 2.1-2.8 2.8-2.1-1.4-2 .8L14 24h-4l-.5-2.2-2-.8-2.1 1.4-2.8-2.8L4 17.5l-.8-2L1 15v-4l2.2-.5.8-2-1.4-2.1 2.8-2.8L7.5 5l2-.5ZM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.2"
      />
    ),
    headset: (
      <path
        d="M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H5a1 1 0 0 1-1-1Zm16 0h-3v6h2a1 1 0 0 0 1-1Zm-3 6c0 1-1 2-3 2"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    image: (
      <path
        d="M3 5h18v14H3zM7 9h.01M3 16l5-5 4 4 2-2 7 6"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    menu: (
      <path
        d="M4 7h16M4 12h16M4 17h16"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
    ),
    message: (
      <path
        d="M4 5h16v12H9l-5 4Zm4 4h8m-8 4h5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    phone: (
      <path
        d="M7 3 4 5c0 8 7 15 15 15l2-3-5-3-2 2c-3-1-5-3-6-6l2-2Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    search: (
      <path
        d="m20 20-4.5-4.5m2.5-5A7.5 7.5 0 1 1 3 10.5a7.5 7.5 0 0 1 15 0Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.6"
      />
    ),
    shield: (
      <path
        d="M12 3 4.5 6v5.5c0 4.5 3 7.7 7.5 9.5 4.5-1.8 7.5-5 7.5-9.5V6Zm-3 9 2 2 4-5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    snowflake: (
      <path
        d="M12 2v20M4 7l16 10M4 17 20 7M8 4l4 3 4-3M8 20l4-3 4 3M3 11l5 1-1 5m14-4-5-1 1-5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.35"
      />
    ),
    team: (
      <path
        d="M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm8-1a3 3 0 1 0 0-6M2 21v-2a6 6 0 0 1 12 0v2m1-6a5 5 0 0 1 7 4.6V21"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    temperature: (
      <path
        d="M10 14V5a3 3 0 1 1 6 0v9a5 5 0 1 1-6 0Zm3-7v9"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
    wrench: (
      <path
        d="M14 6a5 5 0 0 0-6.5 6.5L3 17v4h4l4.5-4.5A5 5 0 0 0 18 10l-3 3-4-4Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.6"
      />
    ),
  };

  return <svg {...commonProps}>{paths[name] ?? paths.image}</svg>;
}

export function MediaPlaceholder({
  label = 'Иллюстрация раздела',
  icon = 'image',
  className = '',
}: Readonly<{ label?: string; icon?: PreviewIcon; className?: string }>) {
  return (
    <div
      aria-label={`Графический блок: ${label}`}
      className={`placeholder-media flex min-h-40 flex-col items-center justify-center gap-3 overflow-hidden bg-slate-100 px-5 text-center text-slate-500 ${className}`}
      data-media-slot="placeholder"
      role="img"
    >
      <span className="grid h-12 w-12 place-items-center rounded-full border border-slate-300 bg-white/80 text-primary-ink">
        <LineIcon name={icon} />
      </span>
      <span className="text-xs font-semibold uppercase tracking-[0.1em]">
        {label}
      </span>
    </div>
  );
}

export function MediaSlot({
  photo,
  label = 'Иллюстрация раздела',
  icon = 'image',
  className = '',
  sizes = '(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw',
}: Readonly<{
  photo?: VerifiedLocalPhoto;
  label?: string;
  icon?: PreviewIcon;
  className?: string;
  sizes?: string;
}>) {
  if (!photo) {
    return <MediaPlaceholder className={className} icon={icon} label={label} />;
  }

  return (
    <div
      className={`relative min-h-40 overflow-hidden ${className}`}
      data-media-slot="verified"
    >
      <Image
        alt={photo.alt}
        className="object-cover"
        fill
        sizes={sizes}
        src={photo.src}
        unoptimized
      />
    </div>
  );
}
