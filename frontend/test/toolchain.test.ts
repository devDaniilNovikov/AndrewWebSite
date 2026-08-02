import { describe, expect, it } from 'vitest';
import {
  assertPinnedNodeVersion,
  PINNED_NODE_VERSION,
} from '../scripts/lib/toolchain.mjs';

describe('pinned frontend runtime', () => {
  it('accepts the exact Node runtime contract', () => {
    expect(() => assertPinnedNodeVersion(PINNED_NODE_VERSION)).not.toThrow();
  });

  it.each(['v24.14.1', 'v26.3.0', '24.14.0', ''])('rejects %j', (version) => {
    expect(() => assertPinnedNodeVersion(version)).toThrow(
      'Node 24.14.0 is required',
    );
  });
});
