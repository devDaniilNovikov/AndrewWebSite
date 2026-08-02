import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['test/**/*.test.{ts,tsx}'],
    setupFiles: './test/setup.ts',
    coverage: {
      provider: 'v8',
      include: [
        'components/**/*.tsx',
        'lib/leads/**/*.ts',
        'scripts/lib/preview-api-origin.mjs',
        'scripts/lib/production-readiness.mjs',
        'scripts/lib/static-boundary.mjs',
      ],
      reporter: ['text', 'json-summary', 'lcov'],
      thresholds: {
        branches: 80,
        functions: 80,
        lines: 80,
        statements: 80,
      },
    },
  },
});
