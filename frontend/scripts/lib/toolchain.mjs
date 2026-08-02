export const PINNED_NODE_VERSION = 'v24.14.0';

export function assertPinnedNodeVersion(version = process.version) {
  if (version !== PINNED_NODE_VERSION) {
    throw new Error(
      `Node ${PINNED_NODE_VERSION.slice(1)} is required; received ${version}.`,
    );
  }
}
