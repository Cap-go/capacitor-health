import { WebPlugin } from '@capacitor/core';

import type {
  AggregatedQueryOptions,
  AggregatedQueryResult,
  AuthorizationOptions,
  AuthorizationStatus,
  AvailabilityResult,
  HealthPlugin,
  QueryOptions,
  QueryWorkoutsOptions,
  QueryWorkoutsResult,
  ReadSamplesResult,
  WriteSampleOptions,
} from './definitions';

export class HealthWeb extends WebPlugin implements HealthPlugin {
  async isAvailable(): Promise<AvailabilityResult> {
    return {
      available: false,
      platform: 'web',
      reason: 'Native health APIs are not accessible in a browser environment.',
    };
  }

  async requestAuthorization(_options: AuthorizationOptions): Promise<AuthorizationStatus> {
    throw this.unimplemented('Health permissions are only available on native platforms.');
  }

  async checkAuthorization(_options: AuthorizationOptions): Promise<AuthorizationStatus> {
    throw this.unimplemented('Health permissions are only available on native platforms.');
  }

  async readSamples(_options: QueryOptions): Promise<ReadSamplesResult> {
    throw this.unimplemented('Reading health data is only available on native platforms.');
  }

  async saveSample(_options: WriteSampleOptions): Promise<void> {
    throw this.unimplemented('Writing health data is only available on native platforms.');
  }

  async getPluginVersion(): Promise<{ version: string }> {
    return { version: 'web' };
  }

  async openHealthConnectSettings(): Promise<void> {
    // No-op on web - Health Connect is Android only
  }

  async showPrivacyPolicy(): Promise<void> {
    // No-op on web - Health Connect privacy policy is Android only
  }

  async queryWorkouts(_options: QueryWorkoutsOptions): Promise<QueryWorkoutsResult> {
    throw this.unimplemented('Querying workouts is only available on native platforms.');
  }

  async queryAggregated(_options: AggregatedQueryOptions): Promise<AggregatedQueryResult> {
    throw this.unimplemented('Querying aggregated data is only available on native platforms.');
  }
}
