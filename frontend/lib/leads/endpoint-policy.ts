import type { LeadEndpointPolicy } from './domain-types';

interface ResolveLeadEndpointInput {
  readonly buildMode: string | undefined;
  readonly pageOrigin: string;
  readonly previewApiOrigin?: string;
}

function isLoopbackHostname(hostname: string): boolean {
  if (hostname === 'localhost' || hostname === '[::1]' || hostname === '::1') {
    return true;
  }

  const octets = hostname.split('.');
  return (
    octets.length === 4 &&
    octets[0] === '127' &&
    octets.every((octet) => /^\d{1,3}$/u.test(octet) && Number(octet) <= 255)
  );
}

function parseBareHttpLoopbackOrigin(candidate: string): string | null {
  if (candidate.trim() !== candidate) {
    return null;
  }

  try {
    const parsed = new URL(candidate);
    if (
      parsed.protocol !== 'http:' ||
      !isLoopbackHostname(parsed.hostname) ||
      parsed.username !== '' ||
      parsed.password !== '' ||
      parsed.pathname !== '/' ||
      parsed.search !== '' ||
      parsed.hash !== '' ||
      parsed.origin !== candidate
    ) {
      return null;
    }

    return parsed.origin;
  } catch {
    return null;
  }
}

export function isAllowedLeadEndpoint(endpoint: string): boolean {
  if (endpoint === '/api/leads') {
    return true;
  }

  try {
    const parsed = new URL(endpoint);
    return (
      parsed.protocol === 'http:' &&
      isLoopbackHostname(parsed.hostname) &&
      parsed.username === '' &&
      parsed.password === '' &&
      parsed.pathname === '/api/leads' &&
      parsed.search === '' &&
      parsed.hash === '' &&
      `${parsed.origin}/api/leads` === endpoint
    );
  } catch {
    return false;
  }
}

export function resolveLeadEndpoint({
  buildMode,
  pageOrigin,
  previewApiOrigin,
}: ResolveLeadEndpointInput): LeadEndpointPolicy {
  if (buildMode === 'production') {
    return { enabled: true, endpoint: '/api/leads', mode: 'production' };
  }

  if (buildMode !== 'preview') {
    return { enabled: false, reason: 'unknown_build_mode' };
  }

  if (parseBareHttpLoopbackOrigin(pageOrigin) === null) {
    return { enabled: false, reason: 'page_not_loopback' };
  }

  if (previewApiOrigin === undefined || previewApiOrigin === '') {
    return { enabled: false, reason: 'preview_api_missing' };
  }

  const parsedPreviewOrigin = parseBareHttpLoopbackOrigin(previewApiOrigin);
  if (parsedPreviewOrigin === null) {
    return { enabled: false, reason: 'preview_api_invalid' };
  }

  return {
    enabled: true,
    endpoint: `${parsedPreviewOrigin}/api/leads`,
    mode: 'preview',
  };
}
