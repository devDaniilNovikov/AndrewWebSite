import type { Metadata } from 'next';
import { ProvisionalProductPage } from '../../components/product-pages/ProvisionalProductPage';
import {
  createProductPageMetadata,
  getProductPage,
} from '../../content/product-pages';

const page = getProductPage('/remont-ledogeneratorov');

export const metadata: Metadata = createProductPageMetadata(page);

export default function IceMakerRepairPage() {
  return <ProvisionalProductPage page={page} />;
}
