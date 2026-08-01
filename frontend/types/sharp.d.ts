/**
 * Next.js references its optional image optimizer from a public declaration.
 * The static export removes that runtime dependency and disables optimization,
 * so any direct use in this frontend must remain a type error.
 */
declare module 'sharp' {
  const unavailableSharp: never;

  export = unavailableSharp;
}
