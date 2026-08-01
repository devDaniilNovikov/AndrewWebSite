declare module 'jest-axe' {
  export type AxeResult = {
    violations: unknown[];
  };

  export function axe(container: Element | Document): Promise<AxeResult>;
}
