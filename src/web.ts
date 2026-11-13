import { WebPlugin } from '@capacitor/core';

import type {
  AuthorizationOptions,
  AuthorizationStatus,
  AvailabilityResult,
  DeleteSamplesOptions,
  DeleteSamplesResult,
  HealthPlugin,
  QueryAggregatedOptions,
  QueryAggregatedResult,
  QueryOptions,
  QueryWorkoutsOptions,
  QueryWorkoutsResult,
  ReadSamplesResult,
  SaveWorkoutOptions,
  SaveWorkoutResult,
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

  async deleteSamples(_options: DeleteSamplesOptions): Promise<DeleteSamplesResult> {
    throw this.unimplemented('Deleting health data is only available on native platforms.');
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

  async saveWorkout(_options: SaveWorkoutOptions): Promise<SaveWorkoutResult> {
    throw this.unimplemented('Saving workouts is only available on native platforms.');
  }

  async queryAggregated(_options: QueryAggregatedOptions): Promise<QueryAggregatedResult> {
    throw this.unimplemented('Querying aggregated data is only available on native platforms.');
  }
}
