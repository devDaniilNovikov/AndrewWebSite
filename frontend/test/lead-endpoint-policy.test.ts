import { describe, expect, it } from 'vitest';
import {
  isAllowedLeadEndpoint,
  resolveLeadEndpoint,
} from '../lib/leads/endpoint-policy';

describe('lead endpoint policy', () => {
  it('uses only the relative same-origin endpoint in production', () => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'production',
        pageOrigin: 'https://example.invalid',
        previewApiOrigin: 'http://127.0.0.1:8080',
      }),
    ).toEqual({ enabled: true, endpoint: '/api/leads', mode: 'production' });
  });

  it.each([
    'http://127.0.0.1:8080',
    'http://localhost:8080',
    'http://[::1]:8080',
  ])('enables a strict loopback API origin in local preview: %s', (origin) => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'preview',
        pageOrigin: 'http://127.0.0.1:3000',
        previewApiOrigin: origin,
      }),
    ).toEqual({
      enabled: true,
      endpoint: `${origin}/api/leads`,
      mode: 'preview',
    });
  });

  it('keeps hosted preview disabled before parsing the configured API', () => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'preview',
        pageOrigin: 'https://preview.example.invalid',
        previewApiOrigin: 'http://127.0.0.1:8080',
      }),
    ).toEqual({ enabled: false, reason: 'page_not_loopback' });
  });

  it('is disabled by default when preview API origin is absent', () => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'preview',
        pageOrigin: 'http://127.0.0.1:3000',
      }),
    ).toEqual({ enabled: false, reason: 'preview_api_missing' });
  });

  it.each([
    'https://127.0.0.1:8080',
    'http://192.168.1.10:8080',
    'http://example.invalid',
    'http://127.0.0.1:8080/',
    'http://127.0.0.1:8080/path',
    'http://user@127.0.0.1:8080',
    'not-an-origin',
  ])('rejects unsafe preview API origin %j', (previewApiOrigin) => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'preview',
        pageOrigin: 'http://localhost:3000',
        previewApiOrigin,
      }),
    ).toEqual({ enabled: false, reason: 'preview_api_invalid' });
  });

  it('fails closed for an unknown build mode', () => {
    expect(
      resolveLeadEndpoint({
        buildMode: 'unexpected',
        pageOrigin: 'http://localhost:3000',
        previewApiOrigin: 'http://localhost:8080',
      }),
    ).toEqual({ enabled: false, reason: 'unknown_build_mode' });
  });

  it.each([
    '/api/leads',
    'http://127.0.0.1:8080/api/leads',
    'http://localhost/api/leads',
    'http://[::1]:8080/api/leads',
  ])('allows the exact lead endpoint %s', (endpoint) => {
    expect(isAllowedLeadEndpoint(endpoint)).toBe(true);
  });

  it.each([
    '/api/leads/',
    '/api/other',
    'https://127.0.0.1:8080/api/leads',
    'http://example.invalid/api/leads',
    'http://user@127.0.0.1:8080/api/leads',
    'http://127.0.0.1:8080/api/leads?query',
    'http://127.0.0.1:8080/api/leads#fragment',
    'http://127.0.0.1:8080/other',
    'not-an-endpoint',
  ])('rejects any other endpoint %j', (endpoint) => {
    expect(isAllowedLeadEndpoint(endpoint)).toBe(false);
  });
});
