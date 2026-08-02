import {
  assertPinnedNodeVersion,
  PINNED_NODE_VERSION,
} from './lib/toolchain.mjs';

assertPinnedNodeVersion();
console.log(`Pinned Node runtime verified: ${PINNED_NODE_VERSION.slice(1)}`);
