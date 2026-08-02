import type { Metadata } from 'next';
import { ProvisionalProductPage } from '../../components/product-pages/ProvisionalProductPage';
import {
  createProductPageMetadata,
  getProductPage,
} from '../../content/product-pages';

const page = getProductPage('/remont-torgovogo-holodilnogo-oborudovaniya');

export const metadata: Metadata = createProductPageMetadata(page);

export default function CommercialEquipmentRepairPage() {
  return <ProvisionalProductPage page={page} />;
}
