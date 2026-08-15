import {
  BenefitStrip,
  EquipmentSection,
  HeroSection,
  ServicesSection,
} from './landing/HeroEquipmentSections';
import { PreviewSiteFrame } from './PreviewSiteFrame';
import {
  AboutSection,
  ContactSection,
  MaintenanceSection,
  ReviewsSection,
} from './landing/TrustContactSections';
import {
  PricingSection,
  ProcessSection,
  RepairCallout,
  WorksSection,
} from './landing/WorkPricingSections';
import { SectionViewAnalytics } from './analytics/SectionViewAnalytics';
import { DeepLinkController } from './landing/DeepLinkController';
import { MobileStickyCta } from './landing/MobileStickyCta';

export function PreviewShell() {
  return (
    <PreviewSiteFrame>
      <SectionViewAnalytics />
      <DeepLinkController />
      <main id="main-content" tabIndex={-1}>
        <HeroSection />
        <BenefitStrip />
        <EquipmentSection />
        <ServicesSection />
        <WorksSection />
        <RepairCallout />
        <PricingSection />
        <ProcessSection />
        <AboutSection />
        <MaintenanceSection />
        <ReviewsSection />
        <ContactSection />
      </main>
      <MobileStickyCta />
    </PreviewSiteFrame>
  );
}
