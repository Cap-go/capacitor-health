/**
 * Resolves @capacitor/docgen without relying on pnpm's .bin shim. In this monorepo, the
 * {@code @capacitor/docgen} symlink under {@code node_modules} can point at a stale pnpm path
 * while the real package still exists at the workspace root.
 */
const { spawnSync } = require('child_process');
const { pluginRoot, resolveFromWorkspace } = require('./resolve-workspace-module.cjs');

const docgenBin = resolveFromWorkspace('@capacitor/docgen/bin/docgen');

const r = spawnSync(
  process.execPath,
  [docgenBin, '--api', 'HealthPlugin', '--output-readme', 'README.md', '--output-json', 'dist/docs.json'],
  { stdio: 'inherit', cwd: pluginRoot, env: process.env },
);
process.exit(r.status === null ? 1 : r.status);
