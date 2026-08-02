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

export function PreviewShell() {
  return (
    <PreviewSiteFrame>
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
    </PreviewSiteFrame>
  );
}
