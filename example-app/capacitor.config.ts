import type { CapacitorConfig } from '@capacitor/cli';

import pkg from './package.json' with { type: 'json' };

const maestroE2e = process.env.CAPGO_MAESTRO_E2E === '1';

const config: CapacitorConfig = {
  appId: 'app.capgo.health',
  appName: '@capgo/capacitor-health',
  webDir: 'dist',
  plugins: {
    SplashScreen: {
      launchAutoHide: false,
    },
    CapacitorUpdater: {
      appId: 'app.capgo.health',
      autoUpdate: !maestroE2e,
      autoSplashscreen: !maestroE2e,
      directUpdate: maestroE2e ? 'never' : 'always',
      version: pkg.version,
    },
  },
};

export default config;
