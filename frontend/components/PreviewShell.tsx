import {
  BenefitStrip,
  EquipmentSection,
  HeroSection,
  ServicesSection,
} from './landing/HeroEquipmentSections';
import { LandingHeader } from './landing/LandingHeader';
import {
  AboutSection,
  ContactSection,
  LandingFooter,
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
    <>
      <a className="skip-link" href="#main-content">
        Перейти к содержимому
      </a>

      <div
        className="border-b border-blue-200 bg-blue-50 px-5 py-2 text-center text-xs font-semibold text-blue-950"
        role="status"
      >
        Демонстрационная версия · контакты, цены, отзывы и фотографии заменены
        плейсхолдерами
      </div>

      <LandingHeader />

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

      <LandingFooter />
    </>
  );
}
