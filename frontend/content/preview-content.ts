export type PreviewHref = `#${string}` | `/${string}`;

export type NavigationItem = Readonly<{
  href: PreviewHref;
  label: string;
}>;

export const navigationItems = [
  { href: '/#equipment', label: 'Оборудование' },
  { href: '/uslugi', label: 'Услуги' },
  { href: '/raboty', label: 'Работы' },
  { href: '/tseny', label: 'Цены' },
  { href: '/o-kompanii', label: 'О компании' },
  { href: '/kontakty', label: 'Контакты' },
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
    title: 'Условия будут проверены',
    text: 'До публикации на сайте.',
  },
] as const;

export const equipmentItems = [
  {
    icon: 'snowflake',
    title: 'Холодильные шкафы',
    text: 'Коммерческое оборудование для хранения продуктов.',
    href: '/remont-torgovogo-holodilnogo-oborudovaniya',
  },
  {
    icon: 'store',
    title: 'Витрины и горки',
    text: 'Торговое холодильное оборудование для залов и выкладки.',
    href: '/remont-torgovogo-holodilnogo-oborudovaniya',
  },
  {
    icon: 'ice',
    title: 'Льдогенераторы',
    text: 'Профессиональное оборудование для заведений и производств.',
    href: '/remont-ledogeneratorov',
  },
  {
    icon: 'box',
    title: 'Морозильные лари',
    text: 'Категория показана как демонстрационная структура каталога.',
    href: '/remont-torgovogo-holodilnogo-oborudovaniya',
  },
  {
    icon: 'fan',
    title: 'Холодильные системы',
    text: 'Точный перечень обслуживаемых систем ожидает подтверждения.',
    href: '/uslugi',
  },
  {
    icon: 'cabinet',
    title: 'Шкафы и столы',
    text: 'Профессиональное оборудование кухонь и торговых объектов.',
    href: '/remont-torgovogo-holodilnogo-oborudovaniya',
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
    text: 'Структура услуги без обещаний по срокам и результату.',
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
    text: 'Условия сопровождения будут сформированы после проверки.',
  },
] as const;

export const workPlaceholders = [
  { label: 'Кейс торгового объекта', icon: 'store' },
  { label: 'Кейс холодильного шкафа', icon: 'cabinet' },
  { label: 'Кейс ледогенератора', icon: 'ice' },
] as const;

export const pricingItems = [
  {
    icon: 'search',
    title: 'Выезд и диагностика',
    text: 'Публикуется после подтверждения условий.',
  },
  {
    icon: 'wrench',
    title: 'Ремонт торгового оборудования',
    text: 'Зависит от неисправности, деталей и объёма работ.',
  },
  {
    icon: 'cabinet',
    title: 'Ремонт холодильных шкафов',
    text: 'Стоимость появится после проверки прайса.',
  },
  {
    icon: 'calendar',
    title: 'Плановое обслуживание',
    text: 'Формат и состав обслуживания согласовываются отдельно.',
  },
] as const;

export const processSteps = [
  {
    title: 'Принимаем заявку',
    text: 'После подключения формы или публикации телефона.',
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
    title: 'Проверяем результат',
    text: 'После работ обсуждаем дальнейшее обслуживание.',
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
      'Перечень работ уточняется',
      'Периодичность согласовывается',
      'Условия фиксируются отдельно',
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
  | (typeof workPlaceholders)[number]['icon']
  | (typeof pricingItems)[number]['icon']
  | (typeof maintenanceItems)[number]['icon']
  | 'menu'
  | 'close'
  | 'phone'
  | 'arrow'
  | 'image'
  | 'message';
