export type PreviewHref = `#${string}` | `/${string}`;

export type NavigationItem = Readonly<{
  href: PreviewHref;
  label: string;
}>;

export const navigationItems = [
  { href: '/#equipment', label: 'Оборудование' },
  { href: '/#services', label: 'Услуги' },
  { href: '/#works', label: 'Работы' },
  { href: '/#pricing', label: 'Цены' },
  { href: '/#about', label: 'О компании' },
  { href: '/#request', label: 'Контакты' },
] as const satisfies readonly NavigationItem[];

export const benefitItems = [
  {
    icon: 'building',
    title: 'Работа с организациями',
    text: 'Коммерческое холодильное оборудование.',
  },
  {
    icon: 'clipboard',
    title: 'Согласование работ',
    text: 'До начала основных работ.',
  },
  {
    icon: 'team',
    title: 'Команда под задачу',
    text: 'Под конкретную неисправность.',
  },
  {
    icon: 'shield',
    title: 'Технический контроль',
    text: 'Единая коммуникация по заявке.',
  },
] as const;

export const heroPhoto = {
  alt: 'Производственный цех с коммерческим холодильным оборудованием: шкафы, витрины, холодильная камера и льдогенератор',
  height: 1086,
  src: '/media/verified/hero-commercial-refrigeration.webp',
  width: 1448,
} as const;

export const aboutTeamPhoto = {
  alt: 'Три специалиста на площадке монтажа холодильной камеры',
  height: 1447,
  src: '/media/verified/about-team-installation.webp',
  width: 1087,
} as const;

export const equipmentItems = [
  {
    id: 'equipment-refrigerated-cabinets',
    icon: 'snowflake',
    title: 'Холодильные камеры',
    photo: {
      alt: 'Промышленная холодильная камера',
      height: 941,
      src: '/media/verified/equipment-cold-rooms.jpg',
      width: 1672,
    },
    text: 'Промышленные холодильные камеры для хранения продуктов.',
    examples: ['Не охлаждает', 'Шумит', 'Обмерзает'],
  },
  {
    id: 'equipment-display-cases',
    icon: 'store',
    title: 'Витрины и горки',
    photo: {
      alt: 'Холодильная витрина для выкладки товаров',
      height: 676,
      src: '/media/verified/equipment-display-cases.webp',
      width: 1200,
    },
    text: 'Торговое холодильное оборудование для залов и выкладки.',
    examples: ['Теплеет', 'Течёт', 'Покрывается наледью'],
  },
  {
    id: 'equipment-ice-makers',
    icon: 'ice',
    title: 'Льдогенераторы',
    photo: {
      alt: 'Профессиональный льдогенератор',
      height: 676,
      src: '/media/verified/equipment-ice-makers.webp',
      width: 1200,
    },
    text: 'Профессиональное оборудование для заведений и производств.',
    examples: ['Не делает лёд', 'Медленный цикл', 'Протекает'],
  },
  {
    id: 'equipment-chest-freezers',
    icon: 'box',
    title: 'Морозильные лари',
    photo: {
      alt: 'Профессиональный морозильный ларь',
      height: 941,
      src: '/media/verified/equipment-chest-freezers.jpg',
      width: 1672,
    },
    text: 'Морозильное оборудование для торговых объектов.',
    examples: ['Не морозит', 'Обмерзает', 'Не включается'],
  },
  {
    id: 'equipment-refrigeration-systems',
    icon: 'fan',
    title: 'Холодильные системы',
    photo: {
      alt: 'Промышленная холодильная система',
      height: 941,
      src: '/media/verified/equipment-refrigeration-systems.jpg',
      width: 1672,
    },
    text: 'Холодильные контуры, автоматика и связанные узлы.',
    examples: ['Не запускается', 'Шумит', 'Теряет температуру'],
  },
  {
    id: 'equipment-cabinets-and-tables',
    icon: 'cabinet',
    title: 'Шкафы и столы',
    photo: {
      alt: 'Профессиональные холодильные шкафы и столы',
      height: 941,
      src: '/media/verified/equipment-cabinets-and-tables.jpg',
      width: 1672,
    },
    text: 'Профессиональное оборудование кухонь и торговых объектов.',
    examples: ['Не охлаждает', 'Течёт', 'Работает нестабильно'],
  },
] as const;

export const serviceItems = [
  {
    icon: 'search',
    title: 'Диагностика',
    text: 'Определение причины неисправности и подготовка плана работ.',
  },
  {
    icon: 'wrench',
    title: 'Ремонт',
    text: 'Выполнение согласованного объёма работ после диагностики.',
  },
  {
    icon: 'gear',
    title: 'Замена узлов',
    text: 'Состав работ и комплектующих согласовывается отдельно.',
  },
  {
    icon: 'temperature',
    title: 'Температурный режим',
    text: 'Диагностика причин отклонения и подбор решения.',
  },
  {
    icon: 'calendar',
    title: 'Плановое обслуживание',
    text: 'Формат для регулярных задач организаций.',
  },
  {
    icon: 'headset',
    title: 'Техническое сопровождение',
    text: 'Единый контакт по заявке и результату выполненных работ.',
  },
] as const;

export const workItems = [
  {
    id: 'work-retail-site',
    label: 'Кейс торгового объекта',
    icon: 'store',
    photo: {
      alt: 'Испаритель внутри промышленного холодильного шкафа',
      height: 1086,
      src: '/media/verified/work-retail-site.jpg',
      width: 1448,
    },
    title: 'Реставрация испарителя холодильного шкафа/стола',
    problem: 'Утечка фреона в системе.',
    result:
      'Реставрация испарителя путем замены трубок и уголков. Утечки нет, пожизненная гарантия.',
  },
  {
    id: 'work-refrigerated-cabinet',
    label: 'Кейс холодильного шкафа',
    icon: 'cabinet',
    photo: {
      alt: 'Холодильная установка с ресиверами и медными трубопроводами',
      height: 1086,
      src: '/media/verified/work-refrigerated-cabinet.jpg',
      width: 1448,
    },
    title: 'Поиск утечки промышленного агрегата',
    problem: 'Утечка фреона в централи холодильной системы.',
    result:
      'Система была опрессована, была найдена утечка в нескольких местах и была устранена.',
  },
  {
    id: 'work-ice-maker',
    label: 'Кейс ледогенератора',
    icon: 'ice',
    photo: {
      alt: 'Внутренний узел льдогенератора с компрессором и теплообменником',
      height: 1086,
      src: '/media/verified/work-ice-maker.jpg',
      width: 1448,
    },
    title: 'Переход ледогенератора на воздушное охлаждение',
    problem: 'Чрезмерное потребление воды и утечка фреона.',
    result: 'Ледогенератор стабильно работает и меньше потребляет воды.',
  },
] as const;

export const pricingItems = [
  {
    icon: 'search',
    title: 'Выезд и диагностика',
    text: 'Оценивается после уточнения объекта и задачи.',
  },
  {
    icon: 'wrench',
    title: 'Ремонт торгового оборудования',
    text: 'Зависит от неисправности, деталей и объёма работ.',
  },
  {
    icon: 'cabinet',
    title: 'Ремонт холодильных шкафов',
    text: 'Рассчитывается по результатам диагностики.',
  },
  {
    icon: 'calendar',
    title: 'Плановое обслуживание',
    text: 'Зависит от состава оборудования и периодичности.',
  },
] as const;

export const processSteps = [
  {
    title: 'Принимаем заявку',
    text: 'Фиксируем контакт, оборудование, симптомы и район выезда.',
  },
  {
    title: 'Назначаем мастера',
    text: 'Уточняем задачу и подбираем специалиста.',
  },
  {
    title: 'Согласовываем стоимость',
    text: 'Объём и стоимость основных работ согласуются заранее.',
  },
  {
    title: 'Контролируем результат',
    text: 'Остаёмся единым контактом по выполненной работе и гарантийному обращению.',
  },
] as const;

export const maintenanceItems = [
  {
    icon: 'building',
    title: 'Кому подходит',
    points: ['Кафе и ресторанам', 'Магазинам', 'Пищевым производствам'],
  },
  {
    icon: 'clipboard',
    title: 'Что входит',
    points: [
      'Диагностика состояния',
      'Проверка доступных узлов',
      'Рекомендации по следующим работам',
    ],
  },
  {
    icon: 'shield',
    title: 'Цель обслуживания',
    points: [
      'Планировать работы',
      'Следить за состоянием техники',
      'Обсуждать приоритеты',
    ],
  },
  {
    icon: 'message',
    title: 'Как отправить запрос',
    points: [
      'Описать объект',
      'Указать тип оборудования',
      'Перейти к общей форме заявки',
    ],
  },
] as const;

export const reviewPlaceholders = [
  'Отзыв управляющего объектом',
  'Отзыв представителя магазина',
  'Отзыв производственной площадки',
] as const;

export type PreviewIcon =
  | (typeof benefitItems)[number]['icon']
  | (typeof equipmentItems)[number]['icon']
  | (typeof serviceItems)[number]['icon']
  | (typeof workItems)[number]['icon']
  | (typeof pricingItems)[number]['icon']
  | (typeof maintenanceItems)[number]['icon']
  | 'menu'
  | 'close'
  | 'phone'
  | 'arrow'
  | 'image'
  | 'message';
