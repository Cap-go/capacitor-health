const { spawnSync } = require('child_process');
const { pluginRoot, resolveFromWorkspace } = require('./resolve-workspace-module.cjs');

const tsc = resolveFromWorkspace('typescript/lib/tsc');
const args = [tsc, ...process.argv.slice(2)];
const r = spawnSync(process.execPath, args, { stdio: 'inherit', cwd: pluginRoot, env: process.env });
process.exit(r.status === null ? 1 : r.status);
