const path = require('path');

const pluginRoot = path.join(__dirname, '..');
const searchRoots = [
  pluginRoot,
  path.join(pluginRoot, '..'),
  path.join(pluginRoot, '..', '..'),
  path.join(pluginRoot, '..', '..', '..'),
];

/**
 * @param {string} subpath e.g. {@code typescript/lib/tsc} or {@code @capacitor/docgen/bin/docgen}
 */
function resolveFromWorkspace(subpath) {
  for (const p of searchRoots) {
    try {
      return require.resolve(subpath, { paths: [p] });
    } catch {
      /* try next */
    }
  }
  throw new Error(`Could not resolve "${subpath}" (pnpm install from monorepo root?)`);
}

module.exports = { pluginRoot, searchRoots, resolveFromWorkspace };
