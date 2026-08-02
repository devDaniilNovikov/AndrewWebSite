import type { Metadata } from 'next';
import { ProvisionalProductPage } from '../../components/product-pages/ProvisionalProductPage';
import {
  createProductPageMetadata,
  getProductPage,
} from '../../content/product-pages';

const page = getProductPage('/tseny');

export const metadata: Metadata = createProductPageMetadata(page);

export default function PricesPage() {
  return <ProvisionalProductPage page={page} />;
}
