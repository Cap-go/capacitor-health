const { spawnSync } = require('child_process');
const { pluginRoot, resolveFromWorkspace } = require('./resolve-workspace-module.cjs');

const rollup = resolveFromWorkspace('rollup/dist/bin/rollup');
const r = spawnSync(process.execPath, [rollup, '-c', 'rollup.config.mjs'], {
  stdio: 'inherit',
  cwd: pluginRoot,
  env: process.env,
});
process.exit(r.status === null ? 1 : r.status);
