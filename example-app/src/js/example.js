import { CapacitorUpdater } from '@capgo/capacitor-updater';
import { Capacitor } from '@capacitor/core';
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

setDefaultDateRanges();

if (Capacitor.isNativePlatform()) {
  CapacitorUpdater.notifyAppReady().catch((error) => {
    console.error('Capgo notifyAppReady failed', error);
  });
}
