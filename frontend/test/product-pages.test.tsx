import { render, within } from '@testing-library/react';
import { axe } from 'jest-axe';
import { describe, expect, it } from 'vitest';
import { ProvisionalProductPage } from '../components/product-pages/ProvisionalProductPage';
import {
  createProductPageMetadata,
  getProductPage,
  productPageRoutes,
} from '../content/product-pages';

const EXPECTED_PATHS = [
  '/uslugi',
  '/remont-torgovogo-holodilnogo-oborudovaniya',
  '/remont-ledogeneratorov',
  '/o-kompanii',
  '/raboty',
  '/tseny',
  '/kontakty',
] as const;

type ExpectedPath = (typeof EXPECTED_PATHS)[number];
type ProductPageRegistryEntry = Readonly<{
  description: string;
  path: ExpectedPath;
  title: string;
}>;

const typedProductPageRoutes =
  productPageRoutes satisfies readonly ProductPageRegistryEntry[];

describe('provisional product page registry', () => {
  it('exports exactly the approved routes with unique metadata', () => {
    expect(typedProductPageRoutes.map(({ path }) => path)).toEqual(
      EXPECTED_PATHS,
    );

    const titles = typedProductPageRoutes.map(({ title }) => title.trim());
    const descriptions = typedProductPageRoutes.map(({ description }) =>
      description.trim(),
    );

    expect(titles.every(Boolean)).toBe(true);
    expect(descriptions.every(Boolean)).toBe(true);
    expect(new Set(titles).size).toBe(EXPECTED_PATHS.length);
    expect(new Set(descriptions).size).toBe(EXPECTED_PATHS.length);
  });

  it('keeps route metadata out of search indexes', () => {
    for (const page of typedProductPageRoutes) {
      expect(createProductPageMetadata(page)).toMatchObject({
        description: page.description,
        robots: { follow: false, index: false },
        title: `${page.title} — предварительная версия`,
      });
    }
  });

  it('contains no published business contacts, prices, or verified media', () => {
    const serializedRegistry = JSON.stringify(typedProductPageRoutes);

    expect(serializedRegistry).not.toMatch(
      /(?:tel:|https?:\/\/|\/media\/verified\/)/iu,
    );
    expect(serializedRegistry).not.toMatch(/\d[\d\s.,]*(?:₽|руб(?:\.|лей)?)/iu);
    expect(serializedRegistry).not.toContain('Москва');
    expect(serializedRegistry).not.toMatch(
      /(?:Телефон будет добавлен|Регион(?: выезда)? уточняется|Цена уточняется|Отправка отключена)/iu,
    );
    expect(serializedRegistry).toContain('уточняется');
  });

  it('routes product-page request links through the shared lead context contract', () => {
    const page = getProductPage('/uslugi');
    const { container } = render(<ProvisionalProductPage page={page} />);
    const view = within(container);

    expect(
      container.querySelector('[data-lead-source="product-uslugi-hero"]'),
    ).toHaveAttribute('data-lead-intent', 'repair');

    const maintenanceCta = view.getByRole('link', {
      name: 'Обсудить обслуживание',
    });
    expect(maintenanceCta).toHaveAttribute(
      'data-lead-source',
      'product-services-maintenance',
    );
    expect(maintenanceCta).toHaveAttribute('data-lead-intent', 'maintenance');
  });

  it('marks works, prices, and company details as provisional', () => {
    for (const path of ['/raboty', '/tseny', '/o-kompanii'] as const) {
      const items = getProductPage(path).sections.flatMap(
        (section) => section.items,
      );

      expect(items.length, path).toBeGreaterThan(0);
      expect(
        items.every((item) => item.status === 'placeholder'),
        path,
      ).toBe(true);
    }
  });
});

describe('ProvisionalProductPage', () => {
  for (const page of typedProductPageRoutes) {
    it(`renders ${page.path} through the safe placeholder-only shell`, () => {
      const { container } = render(<ProvisionalProductPage page={page} />);
      const view = within(container);

      expect(container.querySelectorAll('h1')).toHaveLength(1);
      expect(view.getByRole('heading', { level: 1 })).toHaveTextContent(
        page.title,
      );
      expect(
        view.getAllByText(/Предпубликационная версия/iu).length,
      ).toBeGreaterThan(0);
      expect(view.getAllByText(page.mediaLabel).length).toBeGreaterThan(0);
      expect(
        view.getAllByText('Телефон не опубликован').length,
      ).toBeGreaterThan(0);
      expect(
        view.queryByText('Телефон будет добавлен'),
      ).not.toBeInTheDocument();
      expect(
        view.queryByText('Название компании уточняется'),
      ).not.toBeInTheDocument();
      expect(
        container.querySelectorAll('[data-media-slot="placeholder"]'),
      ).not.toHaveLength(0);
      expect(
        container.querySelector('[data-media-slot="verified"]'),
      ).not.toBeInTheDocument();
      expect(container.querySelector('img')).not.toBeInTheDocument();
      expect(
        container.querySelector('a[href^="tel:"]'),
      ).not.toBeInTheDocument();
      expect(
        container.querySelector('a[href^="http"]'),
      ).not.toBeInTheDocument();

      for (const link of container.querySelectorAll<HTMLAnchorElement>(
        'a[href]',
      )) {
        expect(link.getAttribute('href')).toMatch(/^(?:#|\/(?!\/))/u);
      }
    });
  }

  it('has no WCAG A or AA violations in the shared product-page renderer', async () => {
    const { container } = render(
      <ProvisionalProductPage page={typedProductPageRoutes[0]} />,
    );

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  });
});
