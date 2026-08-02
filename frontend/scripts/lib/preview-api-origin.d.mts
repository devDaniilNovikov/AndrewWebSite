export type FrontendBuildMode = 'preview' | 'production';

export function parsePreviewApiOrigin(
  mode: FrontendBuildMode,
  configuredOrigin: string | undefined,
): string | undefined;
