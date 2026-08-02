import type { Metadata } from 'next';
import { ProvisionalProductPage } from '../../components/product-pages/ProvisionalProductPage';
import {
  createProductPageMetadata,
  getProductPage,
} from '../../content/product-pages';

const page = getProductPage('/raboty');

export const metadata: Metadata = createProductPageMetadata(page);

export default function WorksPage() {
  return <ProvisionalProductPage page={page} />;
}
