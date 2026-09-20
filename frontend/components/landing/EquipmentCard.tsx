import type { equipmentItems } from '../../content/preview-content';
import { LeadCta } from './LeadCta';
import { MediaSlot } from './PreviewPrimitives';

type EquipmentItem = (typeof equipmentItems)[number];

type EquipmentCardProps = Readonly<{
  item: EquipmentItem;
}>;

export function EquipmentCard({ item }: EquipmentCardProps) {
  const headingId = `${item.id}-title`;

  return (
    <article
      aria-labelledby={headingId}
      className="flex h-full flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-[0_10px_35px_rgba(15,23,42,0.05)]"
      id={item.id}
    >
      <MediaSlot
        className="border-b border-slate-200"
        icon={item.icon}
        label={item.title}
        photo={'photo' in item ? item.photo : undefined}
      />
      <div className="flex flex-1 flex-col p-5">
        <h3
          className="text-lg font-semibold tracking-[-0.025em] text-navy"
          id={headingId}
        >
          {item.title}
        </h3>
        <p className="mt-2 text-base leading-6 text-slate-600">{item.text}</p>
        <ul
          aria-label={`Примеры обращений: ${item.title}`}
          className="mt-4 flex flex-wrap gap-2"
        >
          {item.examples.map((example) => (
            <li
              className="rounded-full bg-surface px-3 py-1.5 text-sm leading-5 text-slate-600"
              key={example}
            >
              {example}
            </li>
          ))}
        </ul>

        <div className="mt-auto border-t border-slate-100 pt-4">
          <LeadCta
            aria-label={`Оставить заявку: ${item.title}`}
            className="inline-flex min-h-11 w-full items-center justify-center rounded-md bg-primary px-4 py-2.5 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection={item.id}
          >
            Оставить заявку
          </LeadCta>
        </div>
      </div>
    </article>
  );
}
