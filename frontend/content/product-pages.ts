import type { Metadata } from 'next';
import type { PreviewHref, PreviewIcon } from './preview-content';

export const productPagePaths = [
  '/uslugi',
  '/remont-torgovogo-holodilnogo-oborudovaniya',
  '/remont-ledogeneratorov',
  '/o-kompanii',
  '/raboty',
  '/tseny',
  '/kontakty',
] as const;

export type ProductPagePath = (typeof productPagePaths)[number];

type ContentStatus = 'confirmed' | 'placeholder';
type SectionTone = 'light' | 'surface' | 'dark';

export type ProductPageItem = Readonly<{
  icon: PreviewIcon;
  title: string;
  text: string;
  status: ContentStatus;
  href?: PreviewHref;
  linkLabel?: string;
  intent?: 'maintenance' | 'repair';
  sourceSection?: string;
}>;

export type ProductPageSection = Readonly<{
  title: string;
  description: string;
  tone: SectionTone;
  items: readonly ProductPageItem[];
}>;

export type ProductPageDefinition = Readonly<{
  path: ProductPagePath;
  title: string;
  description: string;
  eyebrow: string;
  mediaIcon: PreviewIcon;
  mediaLabel: string;
  sections: readonly ProductPageSection[];
}>;

export const productPageRoutes = [
  {
    path: '/uslugi',
    title: 'Услуги',
    description:
      'Предварительная структура направлений ремонта и обслуживания коммерческого холодильного оборудования.',
    eyebrow: 'Регион обслуживания не опубликован',
    mediaIcon: 'wrench',
    mediaLabel: 'Фото услуги будет добавлено',
    sections: [
      {
        title: 'Основные направления',
        description:
          'Показаны только направления, подтверждённые в техническом задании.',
        tone: 'light',
        items: [
          {
            icon: 'store',
            title: 'Торговое холодильное оборудование',
            text: 'Холодильные шкафы, витрины, столы и лари.',
            status: 'confirmed',
            href: '/remont-torgovogo-holodilnogo-oborudovaniya',
            linkLabel: 'Открыть страницу',
          },
          {
            icon: 'ice',
            title: 'Льдогенераторы',
            text: 'Отдельное направление ремонта профессионального оборудования.',
            status: 'confirmed',
            href: '/remont-ledogeneratorov',
            linkLabel: 'Открыть страницу',
          },
          {
            icon: 'calendar',
            title: 'Плановое обслуживание',
            text: 'Обсуждение регулярного обслуживания объектов через общую форму.',
            status: 'confirmed',
            href: '#request',
            linkLabel: 'Обсудить обслуживание',
            intent: 'maintenance',
            sourceSection: 'product-services-maintenance',
          },
        ],
      },
      {
        title: 'Состав обращения',
        description:
          'Фактический перечень операций и коммерческие условия фиксируются после проверки материалов.',
        tone: 'dark',
        items: [
          {
            icon: 'search',
            title: 'Диагностика',
            text: 'Определение причины и подготовка следующего шага.',
            status: 'confirmed',
          },
          {
            icon: 'clipboard',
            title: 'Согласование работ',
            text: 'Основные работы и стоимость согласуются заранее.',
            status: 'confirmed',
          },
          {
            icon: 'shield',
            title: 'Гарантийные условия',
            text: 'Срок и состав гарантии будут добавлены после подтверждения.',
            status: 'placeholder',
          },
        ],
      },
    ],
  },
  {
    path: '/remont-torgovogo-holodilnogo-oborudovaniya',
    title: 'Ремонт торгового холодильного оборудования',
    description:
      'Предварительная страница ремонта шкафов, витрин, холодильных столов и ларей без неподтверждённых обещаний.',
    eyebrow: 'Основное коммерческое направление',
    mediaIcon: 'store',
    mediaLabel: 'Фото оборудования будет добавлено',
    sections: [
      {
        title: 'Оборудование',
        description:
          'Категории отражают подтверждённый объём первой версии сайта.',
        tone: 'light',
        items: [
          {
            icon: 'cabinet',
            title: 'Холодильные шкафы',
            text: 'Коммерческое оборудование для хранения продуктов.',
            status: 'confirmed',
          },
          {
            icon: 'store',
            title: 'Витрины и холодильные столы',
            text: 'Оборудование торговых залов и профессиональных кухонь.',
            status: 'confirmed',
          },
          {
            icon: 'box',
            title: 'Лари и другое оборудование',
            text: 'Точный перечень фактически обслуживаемых моделей уточняется.',
            status: 'placeholder',
          },
        ],
      },
      {
        title: 'Условия ремонта',
        description:
          'Страница сохраняет структуру коммерческого предложения без вымышленных цен и гарантий.',
        tone: 'dark',
        items: [
          {
            icon: 'search',
            title: 'Диагностика',
            text: 'Причина неисправности определяется до согласования основных работ.',
            status: 'confirmed',
          },
          {
            icon: 'clipboard',
            title: 'Стоимость',
            text: 'Цены и условия диагностики будут добавлены после проверки прайса.',
            status: 'placeholder',
            href: '/tseny',
            linkLabel: 'Посмотреть структуру цен',
          },
          {
            icon: 'shield',
            title: 'Гарантия',
            text: 'Подтверждённые гарантийные условия ещё не предоставлены.',
            status: 'placeholder',
          },
        ],
      },
    ],
  },
  {
    path: '/remont-ledogeneratorov',
    title: 'Ремонт ледогенераторов',
    description:
      'Предварительная страница ремонта профессиональных ледогенераторов с безопасными контентными плейсхолдерами.',
    eyebrow: 'Отдельное направление услуг',
    mediaIcon: 'ice',
    mediaLabel: 'Фото ледогенератора будет добавлено',
    sections: [
      {
        title: 'Что будет раскрыто на странице',
        description:
          'Структура готова к наполнению после проверки технических материалов.',
        tone: 'light',
        items: [
          {
            icon: 'ice',
            title: 'Виды оборудования',
            text: 'Перечень обслуживаемых типов и моделей уточняется.',
            status: 'placeholder',
          },
          {
            icon: 'gear',
            title: 'Типовые неисправности',
            text: 'Список неисправностей будет добавлен на основе реального опыта.',
            status: 'placeholder',
          },
          {
            icon: 'wrench',
            title: 'Подход к ремонту',
            text: 'Диагностика и согласование основных работ до их выполнения.',
            status: 'confirmed',
          },
        ],
      },
      {
        title: 'Коммерческие сведения',
        description:
          'Неопубликованные данные остаются заметными блокерами, а не рекламными обещаниями.',
        tone: 'dark',
        items: [
          {
            icon: 'search',
            title: 'Диагностика',
            text: 'Порядок и стоимость диагностики уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'shield',
            title: 'Гарантийный подход',
            text: 'Условия гарантии будут опубликованы после подтверждения.',
            status: 'placeholder',
          },
          {
            icon: 'message',
            title: 'Примеры работ',
            text: 'Реальные кейсы и фотографии будут добавлены в F4.',
            status: 'placeholder',
            href: '/raboty',
            linkLabel: 'Открыть раздел работ',
          },
        ],
      },
    ],
  },
  {
    path: '/o-kompanii',
    title: 'О компании',
    description:
      'Предварительная страница команды мастеров без вымышленных имён, стажа, реквизитов или фотографий.',
    eyebrow: 'Команда будет добавлена',
    mediaIcon: 'team',
    mediaLabel: 'Фото команды будет добавлено',
    sections: [
      {
        title: 'Позиционирование',
        description:
          'Фактическое описание команды будет добавлено после подтверждения владельцем.',
        tone: 'light',
        items: [
          {
            icon: 'team',
            title: 'Команда под задачу',
            text: 'Состав команды и распределение задач уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'building',
            title: 'Работа с организациями',
            text: 'Фактические форматы работы с организациями будут добавлены.',
            status: 'placeholder',
          },
          {
            icon: 'clipboard',
            title: 'Принципы работы',
            text: 'Подробные процессы будут уточнены по фактической практике.',
            status: 'placeholder',
          },
        ],
      },
      {
        title: 'Данные компании',
        description:
          'Персональные и юридические сведения не публикуются до проверки владельцем.',
        tone: 'dark',
        items: [
          {
            icon: 'team',
            title: 'Специалисты',
            text: 'Имена, роли, опыт и фотографии будут добавлены позднее.',
            status: 'placeholder',
          },
          {
            icon: 'building',
            title: 'Реквизиты ИП',
            text: 'Юридические реквизиты ещё не подтверждены.',
            status: 'placeholder',
          },
          {
            icon: 'shield',
            title: 'Документы и гарантия',
            text: 'Финальные формулировки ожидают юридической проверки.',
            status: 'placeholder',
          },
        ],
      },
    ],
  },
  {
    path: '/raboty',
    title: 'Выполненные работы',
    description:
      'Каркас раздела будущих реальных кейсов без вымышленных объектов, результатов, клиентов, сумм или фотографий.',
    eyebrow: 'Материалы готовятся',
    mediaIcon: 'message',
    mediaLabel: 'Фото выполненной работы будет добавлено',
    sections: [
      {
        title: 'Будущие карточки кейсов',
        description:
          'Каждая карточка будет заполнена только после подтверждения фактов и прав на медиа.',
        tone: 'light',
        items: [
          {
            icon: 'store',
            title: 'Торговое оборудование',
            text: 'Неисправность, проведённые работы и результат уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'cabinet',
            title: 'Холодильный шкаф',
            text: 'Описание объекта, результат и стоимость уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'ice',
            title: 'Ледогенератор',
            text: 'Фактический кейс и лицензированные фотографии будут добавлены в F4.',
            status: 'placeholder',
          },
        ],
      },
      {
        title: 'Правила публикации',
        description:
          'Раздел не имитирует доказательства опыта до получения проверяемых материалов.',
        tone: 'dark',
        items: [
          {
            icon: 'clipboard',
            title: 'Проверяемые факты',
            text: 'Требования к подтверждению фактов будут добавлены вместе с реальными кейсами.',
            status: 'placeholder',
          },
          {
            icon: 'image',
            title: 'Права на фотографии',
            text: 'Происхождение, разрешение и alt-текст должны быть подтверждены.',
            status: 'placeholder',
          },
          {
            icon: 'message',
            title: 'Отзывы',
            text: 'Тексты и источники отзывов ещё не предоставлены.',
            status: 'placeholder',
          },
        ],
      },
    ],
  },
  {
    path: '/tseny',
    title: 'Цены',
    description:
      'Предварительная структура прайса без неподтверждённых сумм, условий диагностики или гарантий.',
    eyebrow: 'Прайс требует подтверждения',
    mediaIcon: 'clipboard',
    mediaLabel: 'Иллюстрация прайса будет добавлена',
    sections: [
      {
        title: 'Ориентиры по стоимости',
        description:
          'Числовые значения не публикуются до получения и проверки прайса.',
        tone: 'dark',
        items: [
          {
            icon: 'search',
            title: 'Выезд и диагностика',
            text: 'Цена и условия уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'store',
            title: 'Ремонт торгового оборудования',
            text: 'Стоимость определяется после диагностики и проверки прайса.',
            status: 'placeholder',
          },
          {
            icon: 'ice',
            title: 'Ремонт ледогенераторов',
            text: 'Стоимость определяется после подтверждения условий.',
            status: 'placeholder',
          },
          {
            icon: 'calendar',
            title: 'Плановое обслуживание',
            text: 'Формат и стоимость обслуживания уточняются.',
            status: 'placeholder',
          },
        ],
      },
      {
        title: 'Как формируется стоимость',
        description:
          'Опубликованы только безопасные общие принципы без числовых обещаний.',
        tone: 'surface',
        items: [
          {
            icon: 'gear',
            title: 'Состав неисправности',
            text: 'Факторы, влияющие на стоимость, уточняются.',
            status: 'placeholder',
          },
          {
            icon: 'clipboard',
            title: 'Согласование',
            text: 'Порядок согласования стоимости будет добавлен после подтверждения.',
            status: 'placeholder',
          },
          {
            icon: 'shield',
            title: 'Гарантийные условия',
            text: 'Будут добавлены после подтверждения владельцем.',
            status: 'placeholder',
          },
        ],
      },
    ],
  },
  {
    path: '/kontakty',
    title: 'Контакты',
    description:
      'Предварительная контактная страница: регион, телефон, часы и реквизиты остаются плейсхолдерами.',
    eyebrow: 'Связь с командой',
    mediaIcon: 'phone',
    mediaLabel: 'Контактное изображение не опубликовано',
    sections: [
      {
        title: 'Контактные сведения',
        description:
          'До подтверждения данных страница не создаёт ложных каналов связи.',
        tone: 'light',
        items: [
          {
            icon: 'phone',
            title: 'Телефон',
            text: '+7 (903) 237-58-61',
            status: 'confirmed',
      
          },
          {
            icon: 'building',
            title: 'Регион',
            text: 'Регион выезда не опубликован.',
            status: 'placeholder',
          },
          {
            icon: 'calendar',
            title: 'График',
            text: 'Часы приёма обращений уточняются.',
            status: 'placeholder',
          },
        ],
      },
      {
        title: 'Перед публикацией',
        description:
          'Открытые факты остаются видимыми требованиями к production-контенту.',
        tone: 'dark',
        items: [
          {
            icon: 'building',
            title: 'Реквизиты',
            text: 'Подтверждённые реквизиты ИП будут добавлены позднее.',
            status: 'placeholder',
          },
          {
            icon: 'shield',
            title: 'Политика данных',
            text: 'Юридический текст и ссылка ещё не подтверждены.',
            status: 'placeholder',
          },
          {
            icon: 'message',
            title: 'Форма заявки',
            text: 'Форма принимает заявки после подключения backend; локально допустимы только синтетические данные.',
            status: 'confirmed',
            href: '#request',
            linkLabel: 'Перейти к форме',
            intent: 'repair',
            sourceSection: 'product-contacts-form',
          },
        ],
      },
    ],
  },
] as const satisfies readonly ProductPageDefinition[];

export function getProductPage(path: ProductPagePath): ProductPageDefinition {
  const page = productPageRoutes.find((candidate) => candidate.path === path);

  if (!page) {
    throw new Error(`Unknown product page path: ${path}`);
  }

  return page;
}

export function createProductPageMetadata(
  page: ProductPageDefinition,
): Metadata {
  return {
    title: `${page.title} — предварительная версия`,
    description: page.description,
    robots: {
      index: false,
      follow: false,
    },
  };
}
