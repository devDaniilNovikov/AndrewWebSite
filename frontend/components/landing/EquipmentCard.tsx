'use client';

import { useState } from 'react';
import type { equipmentItems } from '../../content/preview-content';
import { LeadCta } from './LeadCta';
import { LineIcon, MediaSlot } from './PreviewPrimitives';

type EquipmentItem = (typeof equipmentItems)[number];

type EquipmentCardProps = Readonly<{
  item: EquipmentItem;
}>;

export function EquipmentCard({ item }: EquipmentCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const headingId = `${item.id}-title`;
  const detailsId = `${item.id}-details`;

  const toggleDetails = () => {
    setIsExpanded((currentState) => {
      const nextState = !currentState;
      if (nextState) {
        window.dispatchEvent(
          new CustomEvent('andrew:analytics-request', {
            detail: {
              name: 'expand_equipment',
              sourceSection: 'equipment',
            },
          }),
        );
      }
      return nextState;
    });
  };

  return (
    <article
      aria-labelledby={headingId}
      className="flex h-full flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-[0_10px_35px_rgba(15,23,42,0.05)]"
      id={item.id}
    >
      <MediaSlot
        className="min-h-40 border-b border-slate-200"
        icon={item.icon}
        label={item.title}
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

        <div
          aria-hidden={!isExpanded}
          className={`grid transition-[grid-template-rows,opacity] duration-200 ease-out motion-reduce:transition-none ${
            isExpanded
              ? 'grid-rows-[1fr] opacity-100'
              : 'grid-rows-[0fr] opacity-0'
          }`}
          data-expanded={isExpanded}
          id={detailsId}
        >
          <div className="min-h-0 overflow-hidden">
            <div className="mt-5 space-y-5 border-t border-slate-200 pt-5">
              <div>
                <h4 className="text-base font-semibold text-navy">
                  Типичные симптомы
                </h4>
                <ul className="mt-2 list-disc space-y-1.5 pl-5 text-base leading-6 text-slate-600">
                  {item.symptoms.map((symptom) => (
                    <li key={symptom}>{symptom}</li>
                  ))}
                </ul>
              </div>
              <div>
                <h4 className="text-base font-semibold text-navy">
                  Примеры работ
                </h4>
                <ul className="mt-2 list-disc space-y-1.5 pl-5 text-base leading-6 text-slate-600">
                  {item.workExamples.map((workExample) => (
                    <li key={workExample}>{workExample}</li>
                  ))}
                </ul>
              </div>
              <p className="rounded-lg bg-surface p-3 text-sm leading-5 text-slate-600">
                Точная причина определяется после диагностики
              </p>
            </div>
          </div>
        </div>

        <div className="mt-auto flex flex-col gap-2 border-t border-slate-100 pt-4 sm:flex-row">
          <button
            aria-controls={detailsId}
            aria-expanded={isExpanded}
            className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-md border border-primary/25 px-4 py-2.5 text-base font-semibold text-primary-ink transition-colors duration-150 hover:bg-primary/5 motion-reduce:transition-none"
            onClick={toggleDetails}
            type="button"
          >
            {isExpanded ? 'Свернуть' : 'Частые неисправности'}
            <LineIcon
              className={`h-4 w-4 transition-transform duration-200 motion-reduce:transition-none ${isExpanded ? '-rotate-90' : 'rotate-90'}`}
              name="arrow"
            />
          </button>
          <LeadCta
            aria-label={`Оставить заявку: ${item.title}`}
            className="inline-flex min-h-11 flex-1 items-center justify-center rounded-md bg-primary px-4 py-2.5 text-base font-semibold text-white transition-colors duration-150 hover:bg-blue-700 motion-reduce:transition-none"
            sourceSection={item.id}
          >
            Оставить заявку
          </LeadCta>
        </div>
      </div>
    </article>
  );
}
