import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = dirname(fileURLToPath(import.meta.url));

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  images: {
    unoptimized: true,
  },
  poweredByHeader: false,
  turbopack: {
    root: frontendRoot,
  },
  generateBuildId: async () =>
    process.env.ANDREW_BUILD_ID ?? 'andrew-static-preview',
};

export default nextConfig;
