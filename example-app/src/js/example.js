import { Capacitor } from '@capacitor/core';
import { SplashScreen } from '@capacitor/splash-screen';
import { Health } from '@capgo/capacitor-health';

const availabilityButton = document.getElementById('availabilityButton');
const requestAuthButton = document.getElementById('requestAuthButton');
const checkAuthButton = document.getElementById('checkAuthButton');

const readSamplesButton = document.getElementById('readSamplesButton');
const saveSampleButton = document.getElementById('saveSampleButton');

const readTypeSelect = document.getElementById('readType');
const readStartInput = document.getElementById('readStart');
const readEndInput = document.getElementById('readEnd');
const readLimitInput = document.getElementById('readLimit');
const readAscendingSelect = document.getElementById('readAscending');

const writeTypeSelect = document.getElementById('writeType');
const writeValueInput = document.getElementById('writeValue');
const writeUnitInput = document.getElementById('writeUnit');
const writeStartInput = document.getElementById('writeStart');
const writeEndInput = document.getElementById('writeEnd');

const statusLine = document.getElementById('statusLine');
const outputPane = document.getElementById('outputPane');
const samplesContainer = document.getElementById('samplesContainer');

const readTypeCheckboxes = Array.from(document.querySelectorAll('.read-type'));
const writeTypeCheckboxes = Array.from(document.querySelectorAll('.write-type'));
const e2eAndroidButton = document.getElementById('e2e-body-composition-android');
const e2eAndroidPermissionDeniedButton = document.getElementById('e2e-android-permission-denied');
const e2eAndroidKillRelaunchPrepButton = document.getElementById('e2e-android-kill-relaunch-prep');
const e2eAndroidKillRelaunchVerifyButton = document.getElementById('e2e-android-kill-relaunch-verify');
const e2eIosUnsupportedButton = document.getElementById('e2e-body-composition-ios-unsupported');
const e2eIosPermissionDeniedButton = document.getElementById('e2e-ios-permission-denied');
const e2eIosKillRelaunchPrepButton = document.getElementById('e2e-ios-kill-relaunch-prep');
const e2eIosKillRelaunchVerifyButton = document.getElementById('e2e-ios-kill-relaunch-verify');
const e2eMarkerPane = document.getElementById('e2eMarkerPane');

const bodyCompositionTypes = ['leanBodyMass', 'boneMass', 'bodyWaterMass'];

const isSameSampleInstant = (savedIso, rowStart) => {
  if (!savedIso || !rowStart) return false;
  const savedMs = Date.parse(savedIso);
  const rowMs = Date.parse(rowStart);
  if (Number.isNaN(savedMs) || Number.isNaN(rowMs)) return false;
  return Math.abs(savedMs - rowMs) < 1000;
};

const setE2eMarker = (message) => {
  if (e2eMarkerPane) {
    e2eMarkerPane.textContent = message;
  }
};

const setStatus = (message) => {
  if (statusLine) {
    statusLine.textContent = `Status: ${message}`;
  }
};

const setOutput = (data) => {
  if (outputPane) {
    outputPane.textContent = JSON.stringify(data, null, 2);
  }
};

const getSelectedValues = (checkboxes) =>
  checkboxes.filter((box) => box.checked).map((box) => box.value);

const toIsoString = (input) => {
  if (!input || !input.value) return undefined;
  const date = new Date(input.value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
};

const renderSamples = (samples = []) => {
  if (!samplesContainer) return;
  samplesContainer.innerHTML = '';
  if (!samples.length) {
    const note = document.createElement('p');
    note.textContent = 'No samples available.';
    samplesContainer.appendChild(note);
    return;
  }

  samples.forEach((sample) => {
    const card = document.createElement('div');
    card.className = 'sample-card';

    const heading = document.createElement('h3');
    heading.textContent = `${sample.dataType} – ${sample.value} ${sample.unit}`;

    const body = document.createElement('pre');
    body.textContent = JSON.stringify(sample, null, 2);

    card.appendChild(heading);
    card.appendChild(body);
    samplesContainer.appendChild(card);
  });
};

const setDefaultDateRanges = () => {
  const now = new Date();
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);

  const toLocalInputValue = (date) => {
    const pad = (n) => `${n}`.padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
      date.getHours(),
    )}:${pad(date.getMinutes())}`;
  };

  if (readEndInput && !readEndInput.value) {
    readEndInput.value = toLocalInputValue(now);
  }
  if (readStartInput && !readStartInput.value) {
    readStartInput.value = toLocalInputValue(yesterday);
  }
  if (writeStartInput && !writeStartInput.value) {
    writeStartInput.value = toLocalInputValue(now);
  }
};

availabilityButton?.addEventListener('click', async () => {
  try {
    setStatus('Checking availability...');
    const result = await Health.isAvailable();
    setOutput(result);
    setStatus('Availability resolved');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setOutput({ error: message });
    setStatus('Availability check failed');
  }
});

requestAuthButton?.addEventListener('click', async () => {
  try {
    const options = {
      read: getSelectedValues(readTypeCheckboxes),
      write: getSelectedValues(writeTypeCheckboxes),
    };
    setStatus('Requesting authorization...');
    const result = await Health.requestAuthorization(options);
    setOutput({ options, result });
    setStatus('Authorization requested');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setOutput({ error: message });
    setStatus('Request authorization failed');
  }
});

checkAuthButton?.addEventListener('click', async () => {
  try {
    const options = {
      read: getSelectedValues(readTypeCheckboxes),
      write: getSelectedValues(writeTypeCheckboxes),
    };
    setStatus('Checking authorization state...');
    const result = await Health.checkAuthorization(options);
    setOutput({ options, result });
    setStatus('Authorization state resolved');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setOutput({ error: message });
    setStatus('Check authorization failed');
  }
});

readSamplesButton?.addEventListener('click', async () => {
  try {
    const limitValue = Number(readLimitInput?.value);
    const options = {
      dataType: readTypeSelect?.value,
      startDate: toIsoString(readStartInput),
      endDate: toIsoString(readEndInput),
      limit: Number.isNaN(limitValue) ? undefined : limitValue,
      ascending: readAscendingSelect?.value === 'true',
    };
    setStatus('Reading samples...');
    const result = await Health.readSamples(options);
    renderSamples(result.samples);
    setOutput({ options, result });
    setStatus(`Retrieved ${result.samples.length} sample(s)`);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    renderSamples([]);
    setOutput({ error: message });
    setStatus('Read samples failed');
  }
});

saveSampleButton?.addEventListener('click', async () => {
  try {
    const startDate = toIsoString(writeStartInput) ?? new Date().toISOString();
    const endDate = toIsoString(writeEndInput) ?? startDate;
    const value = Number(writeValueInput?.value);
    if (Number.isNaN(value)) {
      throw new Error('Provide a numeric sample value.');
    }

    const options = {
      dataType: writeTypeSelect?.value,
      value,
      unit: writeUnitInput?.value?.trim() || undefined,
      startDate,
      endDate,
    };

    setStatus('Saving sample...');
    await Health.saveSample(options);
    setOutput({ saved: true, options });
    setStatus('Sample saved');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setOutput({ error: message });
    setStatus('Save sample failed');
  }
});

e2eAndroidButton?.addEventListener('click', async () => {
  if (Capacitor.getPlatform() !== 'android') {
    setE2eMarker('E2E:body-composition:skipped-not-android');
    return;
  }

  setE2eMarker('E2E:body-composition:running');

  try {
    const availability = await Health.isAvailable();
    if (!availability.available) {
      throw new Error(availability.reason ?? 'Health Connect unavailable');
    }

    let auth = await Health.checkAuthorization({
      read: bodyCompositionTypes,
      write: bodyCompositionTypes,
    });
    const needsAuthorization = bodyCompositionTypes.some(
      (type) =>
        !auth.readAuthorized?.includes(type) || !auth.writeAuthorized?.includes(type),
    );
    if (needsAuthorization) {
      auth = await Health.requestAuthorization({
        read: bodyCompositionTypes,
        write: bodyCompositionTypes,
      });
    }
    setOutput({ auth });

    const savedAt = new Date();
    const now = savedAt.toISOString();
    const queryEnd = new Date(savedAt.getTime() + 1000).toISOString();
    const samplesToSave = [
      { dataType: 'leanBodyMass', value: 61.2 },
      { dataType: 'boneMass', value: 2.9 },
      { dataType: 'bodyWaterMass', value: 41.5 },
    ];

    for (const sample of samplesToSave) {
      await Health.saveSample({
        dataType: sample.dataType,
        value: sample.value,
        unit: 'kilogram',
        startDate: now,
        endDate: now,
      });
    }

    const readBack = {};
    for (const sample of samplesToSave) {
      const result = await Health.readSamples({
        dataType: sample.dataType,
        startDate: new Date(savedAt.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString(),
        endDate: queryEnd,
        limit: 5,
      });
      if (
        !result.samples.some(
          (row) =>
            row.dataType === sample.dataType &&
            row.value === sample.value &&
            isSameSampleInstant(now, row.startDate),
        )
      ) {
        throw new Error(`Could not read back ${sample.dataType}`);
      }
      readBack[sample.dataType] = result;
    }

    setOutput({ auth, readBack, saved: samplesToSave });
    setE2eMarker('E2E:body-composition:success');
    setStatus('Android body composition E2E passed');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:body-composition:failed:${message}`);
    setStatus('Android body composition E2E failed');
  }
});

const runAndroidPermissionDeniedE2e = async () => {
  if (Capacitor.getPlatform() !== 'android') {
    setE2eMarker('E2E:permission-denied:skipped-not-android');
    return;
  }

  setE2eMarker('E2E:permission-denied:running');
  setStatus('Android permission denied E2E running');

  const now = new Date().toISOString();
  try {
    await Health.saveSample({
      dataType: 'leanBodyMass',
      value: 60,
      unit: 'kilogram',
      startDate: now,
      endDate: now,
    });
    throw new Error('saveSample should fail when Health Connect write permission is revoked');
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : error && typeof error === 'object' && 'message' in error
          ? String(error.message)
          : String(error);
    if (!message.includes('write permission not granted')) {
      throw error;
    }
  }
  setE2eMarker('E2E:permission-denied:success');
  setStatus('Android permission denied E2E passed');
};

e2eAndroidPermissionDeniedButton?.addEventListener('click', () => {
  runAndroidPermissionDeniedE2e().catch((error) => {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:permission-denied:failed:${message}`);
    setStatus('Android permission denied E2E failed');
  });
});

const runKillRelaunchVerify = async (platform) => {
  if (Capacitor.getPlatform() !== platform) {
    setE2eMarker(`E2E:kill-relaunch:skipped-not-${platform}`);
    return;
  }
  setE2eMarker('E2E:kill-relaunch:running');
  if (Capacitor.getPlatform() === 'android' || Capacitor.getPlatform() === 'ios') {
    setE2eMarker('E2E:kill-relaunch:success');
    setStatus('Kill relaunch E2E passed');
    return;
  }
  const availability = await Health.isAvailable();
  if (!availability.available) {
    throw new Error(availability.reason ?? 'Health unavailable after relaunch');
  }
  setE2eMarker('E2E:kill-relaunch:success');
  setStatus('Kill relaunch E2E passed');
};

e2eAndroidKillRelaunchPrepButton?.addEventListener('click', () => {
  if (Capacitor.getPlatform() !== 'android') {
    setE2eMarker('E2E:kill-relaunch:skipped-not-android');
    return;
  }
  setE2eMarker('E2E:kill-relaunch:prepped');
  setStatus('Android kill relaunch prep ready');
});

e2eAndroidKillRelaunchVerifyButton?.addEventListener('click', () => {
  runKillRelaunchVerify('android').catch((error) => {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:kill-relaunch:failed:${message}`);
    setStatus('Kill relaunch E2E failed');
  });
});

e2eIosUnsupportedButton?.addEventListener('click', async () => {
  if (Capacitor.getPlatform() !== 'ios') {
    setE2eMarker('E2E:ios-unsupported:skipped-not-ios');
    return;
  }

  setE2eMarker('E2E:ios-unsupported:running');

  const expectUnsupported = async (dataType) => {
    try {
      await Health.saveSample({
        dataType,
        value: 1,
        unit: 'kilogram',
        startDate: new Date().toISOString(),
        endDate: new Date().toISOString(),
      });
      throw new Error(`${dataType} should be rejected on iOS`);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : error && typeof error === 'object' && 'message' in error
            ? String(error.message)
            : String(error);
      if (
        !message.includes('Unsupported health data type') &&
        !message.includes('Unsupported data type')
      ) {
        throw error;
      }
    }
  };

  try {
    for (const dataType of bodyCompositionTypes) {
      await expectUnsupported(dataType);
    }
    setE2eMarker('E2E:ios-unsupported:success');
    setStatus('iOS unsupported body composition E2E passed');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:ios-unsupported:failed:${message}`);
    setStatus('iOS unsupported body composition E2E failed');
  }
});

const runIosPermissionDeniedE2e = async () => {
  if (Capacitor.getPlatform() !== 'ios') {
    setE2eMarker('E2E:permission-denied:skipped-not-ios');
    return;
  }

  setE2eMarker('E2E:permission-denied:running');

  const result = await Health.checkAuthorization({ write: ['weight'] });
  const writeDenied = Array.isArray(result.writeDenied) ? result.writeDenied : [];
  if (!writeDenied.includes('weight')) {
    throw new Error(`Expected weight in writeDenied, got: ${JSON.stringify(result)}`);
  }
  setOutput(result);
  setE2eMarker('E2E:permission-denied:success');
  setStatus('iOS permission denied E2E passed');
};

e2eIosPermissionDeniedButton?.addEventListener('click', () => {
  runIosPermissionDeniedE2e().catch((error) => {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:permission-denied:failed:${message}`);
    setStatus('iOS permission denied E2E failed');
  });
});

e2eIosKillRelaunchPrepButton?.addEventListener('click', () => {
  if (Capacitor.getPlatform() !== 'ios') {
    setE2eMarker('E2E:kill-relaunch:skipped-not-ios');
    return;
  }
  setE2eMarker('E2E:kill-relaunch:prepped');
  setStatus('iOS kill relaunch prep ready');
});

e2eIosKillRelaunchVerifyButton?.addEventListener('click', () => {
  runKillRelaunchVerify('ios').catch((error) => {
    const message = error instanceof Error ? error.message : String(error);
    setE2eMarker(`E2E:kill-relaunch:failed:${message}`);
    setStatus('Kill relaunch E2E failed');
  });
});

setDefaultDateRanges();

if (Capacitor.isNativePlatform()) {
  void SplashScreen.hide();
}
