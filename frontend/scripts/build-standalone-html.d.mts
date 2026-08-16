export type StandaloneBuildOptions = Readonly<{
  inputPath?: string;
  outputPath?: string;
}>;

export type StandaloneBuildResult = Readonly<{
  bytes: number;
  outputPath: string;
}>;

export function buildStandaloneHtml(
  options?: StandaloneBuildOptions,
): Promise<StandaloneBuildResult>;
