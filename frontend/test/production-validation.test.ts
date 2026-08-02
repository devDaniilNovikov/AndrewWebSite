import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  blockerIds as productionBlockerIds,
  findMissingBlockerIds,
  loadProductionReadiness,
} from '../scripts/lib/production-readiness.mjs';

const blockerIds = [
  'company_name',
  'phone',
  'consent_text',
  'personal_data_policy',
  'prices',
  'warranty_terms',
  'cases',
  'testimonial',
  'licensed_photographs',
] as const;

describe('production content validation', () => {
  it('loads the canonical manifest with every blocker missing', async () => {
    const readiness = await loadProductionReadiness();

    expect(productionBlockerIds).toEqual(blockerIds);
    expect(findMissingBlockerIds(readiness)).toEqual(blockerIds);
    expect(Object.values(readiness)).toEqual(blockerIds.map(() => 'missing'));
  });

  it('accepts only canonical IDs and readiness states', async () => {
    const temporaryDirectory = await mkdtemp(
      resolve(tmpdir(), 'andrew-readiness-'),
    );
    const manifestPath = resolve(temporaryDirectory, 'readiness.json');

    try {
      const verifiedReadiness = Object.fromEntries(
        blockerIds.map((id) => [id, 'verified']),
      );
      await writeFile(manifestPath, JSON.stringify(verifiedReadiness));
      expect(
        findMissingBlockerIds(
          await loadProductionReadiness(pathToFileURL(manifestPath)),
        ),
      ).toEqual([]);

      await writeFile(manifestPath, JSON.stringify([]));
      await expect(
        loadProductionReadiness(pathToFileURL(manifestPath)),
      ).rejects.toThrow('must be an object');

      await writeFile(manifestPath, JSON.stringify({ unexpected: 'missing' }));
      await expect(
        loadProductionReadiness(pathToFileURL(manifestPath)),
      ).rejects.toThrow('IDs do not match');

      await writeFile(
        manifestPath,
        JSON.stringify({ ...verifiedReadiness, phone: 'invented' }),
      );
      await expect(
        loadProductionReadiness(pathToFileURL(manifestPath)),
      ).rejects.toThrow('invalid state');
    } finally {
      await rm(temporaryDirectory, { recursive: true, force: true });
    }
  });

  it('fails closed with every canonical blocker ID and no content values', () => {
    const result = spawnSync(
      process.execPath,
      ['scripts/validate-production-content.mjs'],
      {
        encoding: 'utf8',
      },
    );

    expect(result.status).toBe(1);
    for (const blockerId of blockerIds) {
      expect(result.stderr).toContain(blockerId);
    }
    expect(result.stderr).not.toContain('AndrewWebSite');
    expect(result.stderr.trim().split('\n')).toEqual(blockerIds);
  });

  it('removes a stale preview export before the production gate fails', () => {
    const outputDirectory = resolve('out');
    mkdirSync(outputDirectory, { recursive: true });
    writeFileSync(resolve(outputDirectory, 'stale-preview.txt'), 'preview');

    const result = spawnSync(
      process.execPath,
      ['scripts/build.mjs', 'production'],
      {
        encoding: 'utf8',
        env: {
          ...process.env,
          NEXT_PUBLIC_PREVIEW_API_ORIGIN: 'http://127.0.0.1:4174',
        },
      },
    );

    expect(result.status).toBe(1);
    expect(existsSync(outputDirectory)).toBe(false);
    expect(result.stderr.trim().split('\n')).toEqual(blockerIds);
  });
});
