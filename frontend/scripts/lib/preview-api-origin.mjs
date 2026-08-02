const CONFIGURATION_NAME = 'NEXT_PUBLIC_PREVIEW_API_ORIGIN';

function isLoopbackHostname(hostname) {
  return (
    hostname === 'localhost' ||
    hostname === '[::1]' ||
    hostname === '::1' ||
    /^127(?:\.\d{1,3}){3}$/u.test(hostname)
  );
}

function configurationError(reason) {
  return new Error(`${CONFIGURATION_NAME} ${reason}`);
}

export function parsePreviewApiOrigin(mode, configuredOrigin) {
  if (configuredOrigin === undefined || configuredOrigin === '') {
    return undefined;
  }

  if (mode === 'production') {
    throw configurationError('is forbidden for production builds.');
  }

  if (configuredOrigin.trim() !== configuredOrigin) {
    throw configurationError('must not contain surrounding whitespace.');
  }

  let parsed;

  try {
    parsed = new URL(configuredOrigin);
  } catch {
    throw configurationError('must be a valid bare HTTP loopback origin.');
  }

  if (
    parsed.protocol !== 'http:' ||
    !isLoopbackHostname(parsed.hostname) ||
    parsed.username !== '' ||
    parsed.password !== '' ||
    parsed.pathname !== '/' ||
    parsed.search !== '' ||
    parsed.hash !== '' ||
    parsed.origin !== configuredOrigin
  ) {
    throw configurationError('must be a bare HTTP loopback origin.');
  }

  return parsed.origin;
}
