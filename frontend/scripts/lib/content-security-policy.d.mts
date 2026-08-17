export const OFFLINE_STANDALONE_ARTIFACT_PATH: string;

export function assertNoInlinePresentationAttributes(html: string): void;

export function hashCspSource(source: string): string;

export function buildHostedContentSecurityPolicy(
  input: Readonly<{
    html: string;
    previewApiOrigin?: string;
  }>,
): string;

export function buildStandaloneContentSecurityPolicy(html: string): string;

export function injectStandaloneContentSecurityPolicy(html: string): string;

export function writeHostedCspConfig(
  input: Readonly<{
    outputDirectory: string;
    previewApiOrigin?: string;
  }>,
): Promise<Readonly<{ documents: number; outputPath: string }>>;
