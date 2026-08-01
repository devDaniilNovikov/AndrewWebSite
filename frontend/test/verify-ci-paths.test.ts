import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const scriptPath = resolve('../.github/scripts/verify-ci-paths.sh');

describe('CI path filter policy', () => {
  it('correctly identifies frontend-relevant changes (full-run paths)', () => {
    const testCases = [
      ['frontend/package.json'],
      ['frontend/app/page.tsx', 'pom.xml'],
      ['.github/workflows/ci.yml'],
      ['.github/scripts/verify-ci-paths.sh'],
      ['frontend-tasks/F1A-frontend-ci-gates.md'],
    ];

    for (const testCase of testCases) {
      const input = testCase.join('\0') + '\0';
      const result = spawnSync(scriptPath, [], { input, encoding: 'utf8' });
      expect(result.status).toBe(0);
      expect(result.stdout.trim()).toBe('relevant');
    }
  });

  it('correctly identifies non-frontend-relevant changes (skip paths)', () => {
    const testCases = [
      ['src/main/java/ru/andrew/website/AndrewWebsiteApplication.java'],
      ['pom.xml'],
      ['docs/SPEC.md', 'README.md'],
      [
        'src/test/java/ru/andrew/website/web/ProblemResponseTest.java',
        'CHANGELOG.md',
      ],
    ];

    for (const testCase of testCases) {
      const input = testCase.join('\0') + '\0';
      const result = spawnSync(scriptPath, [], { input, encoding: 'utf8' });
      expect(result.status).toBe(0);
      expect(result.stdout.trim()).toBe('skip');
    }
  });

  it('handles empty input gracefully by returning skip', () => {
    const result = spawnSync(scriptPath, [], { input: '', encoding: 'utf8' });
    expect(result.status).toBe(0);
    expect(result.stdout.trim()).toBe('skip');
  });

  it('handles empty line in input gracefully and checks other lines', () => {
    const input = ['', 'frontend/package.json', ''].join('\0') + '\0';
    const result = spawnSync(scriptPath, [], { input, encoding: 'utf8' });
    expect(result.status).toBe(0);
    expect(result.stdout.trim()).toBe('relevant');
  });
});
