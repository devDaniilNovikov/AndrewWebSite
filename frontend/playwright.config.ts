import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
  },
  webServer: {
    command:
      'node scripts/verify-toolchain.mjs && pnpm run build:standalone && pnpm run serve:preview',
    env: {
      NEXT_PUBLIC_PREVIEW_API_ORIGIN: 'http://127.0.0.1:4174',
    },
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: false,
    timeout: 120000,
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { height: 900, width: 1440 },
      },
    },
    {
      name: 'tablet-chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { height: 1024, width: 768 },
      },
    },
    {
      name: 'mobile-chromium',
      use: {
        ...devices['Pixel 5'],
        viewport: { height: 844, width: 390 },
      },
    },
  ],
});
