(function () {
  const ACTIVE_TAB_STORAGE_KEY = 'dashboard.activeTab';
  const OUTPUT_DOCK_PREFS_KEY = 'dashboard.outputDock.prefs';
  const OUTPUT_CONVEYOR_HISTORY_STORAGE_KEY = 'dashboard.outputDock.conveyorHistory';
  const WATCH_LIST_API = '/api/dashboard/watch';
  const WATCH_CANCEL_API = '/api/dashboard/watch/cancel';
  const WATCH_HISTORY_LIMIT_API = '/api/dashboard/watch/history-limit';
  const WATCH_WS_PATH = '/ws/watch';

  const OUTPUT_MIN_HEIGHT = 170;
  const OUTPUT_DEFAULT_HEIGHT = 260;
  const OUTPUT_MIN_FONT_SIZE = 8;
  const OUTPUT_MAX_FONT_SIZE = 24;
  const OUTPUT_DEFAULT_FONT_SIZE = 12;
  const WATCH_HISTORY_MIN_LIMIT = 1;
  const WATCH_HISTORY_DEFAULT_LIMIT = 100;

  function initTabs() {
    const buttons = Array.from(document.querySelectorAll('.tab-button[data-tab-target]'));
    const panels = Array.from(document.querySelectorAll('.tab-panel'));
    if (!buttons.length || !panels.length) {
      return;
    }

    function findButton(tabId) {
      return buttons.find(function (button) {
        return button.getAttribute('data-tab-target') === tabId;
      });
    }

    function activateTab(tabId) {
      const targetButton = findButton(tabId) || buttons[0];
      const targetId = targetButton.getAttribute('data-tab-target');
      buttons.forEach(function (button) {
        const active = button === targetButton;
        button.classList.toggle('active', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
      });
      panels.forEach(function (panel) {
        const active = panel.id === targetId;
        panel.classList.toggle('active', active);
        panel.hidden = !active;
      });
      window.sessionStorage.setItem(ACTIVE_TAB_STORAGE_KEY, targetId);
    }

    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        activateTab(button.getAttribute('data-tab-target'));
      });
    });

    const url = new URL(window.location.href);
    const requestedTab = url.searchParams.get('tab');
    const storedTab = window.sessionStorage.getItem(ACTIVE_TAB_STORAGE_KEY);
    const initialTab = requestedTab || storedTab || buttons[0].getAttribute('data-tab-target');
    activateTab(initialTab);

    document.querySelectorAll('form.tab-preserve-form').forEach(function (form) {
      form.addEventListener('submit', function () {
        const activeButton = document.querySelector('.tab-button.active[data-tab-target]');
        if (activeButton) {
          window.sessionStorage.setItem(ACTIVE_TAB_STORAGE_KEY, activeButton.getAttribute('data-tab-target'));
        }
      });
    });
  }

  function parseEmbeddedTree() {
    const node = document.getElementById('tree-data');
    if (!node) return {};
    try {
      return JSON.parse(node.textContent || '{}');
    } catch (e) {
      console.error('Failed to parse embedded conveyor tree', e);
      return {};
    }
  }

  function parseEmbeddedOutputEvent() {
    const node = document.getElementById('dashboard-output-event');
    if (!node) {
      return null;
    }
    try {
      const text = (node.textContent || '').trim();
      if (!text) {
        return null;
      }
      return JSON.parse(text);
    } catch (e) {
      console.error('Failed to parse embedded dashboard output event', e);
      return null;
    }
  }

  function selectedName() {
    const url = new URL(window.location.href);
    return url.searchParams.get('name');
  }

  function selectedTab() {
    const url = new URL(window.location.href);
    return url.searchParams.get('tab');
  }

  function nodeMeta(subTree) {
    if (!subTree || typeof subTree !== 'object') {
      return null;
    }
    const meta = subTree.__meta__;
    return meta && typeof meta === 'object' ? meta : null;
  }

  function nodeState(meta) {
    if (!meta) {
      return 'stopped';
    }
    if (meta.running === true && meta.suspended === true) {
      return 'suspended';
    }
    if (meta.running === true) {
      return 'running';
    }
    return 'stopped';
  }

  function nodeTitle(meta) {
    const state = nodeState(meta);
    if (state === 'running') {
      return 'Running';
    }
    if (state === 'suspended') {
      return 'Running (suspended)';
    }
    return 'Stopped';
  }

  function createNode(name, subTree, selected, tab) {
    const li = document.createElement('li');
    li.className = 'tree-node';

    const link = document.createElement('a');
    const query = new URLSearchParams();
    query.set('name', name);
    if (tab) {
      query.set('tab', tab);
    }
    link.href = '/dashboard?' + query.toString();
    link.textContent = name;
    const meta = nodeMeta(subTree);
    link.classList.add('tree-state-' + nodeState(meta));
    link.title = nodeTitle(meta);
    if (name === selected) {
      link.classList.add('active');
    }
    li.appendChild(link);

    const children = Object.keys(subTree || {}).filter(function (child) {
      return child !== '__meta__';
    });
    if (children.length > 0) {
      const ul = document.createElement('ul');
      children.sort().forEach(function (child) {
        ul.appendChild(createNode(child, subTree[child], selected, tab));
      });
      li.appendChild(ul);
    }
    return li;
  }

  function createPropertyRow(keyName, valueName, key, value) {
    const row = document.createElement('div');
    row.className = 'property-row';

    const keyInput = document.createElement('input');
    keyInput.type = 'text';
    keyInput.name = keyName;
    keyInput.placeholder = 'Property key';
    keyInput.value = key || '';

    const valueInput = document.createElement('input');
    valueInput.type = 'text';
    valueInput.name = valueName;
    valueInput.placeholder = 'Property value';
    valueInput.value = value || '';

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.className = 'property-remove';
    removeButton.title = 'Remove property';
    removeButton.textContent = 'X';

    row.appendChild(keyInput);
    row.appendChild(valueInput);
    row.appendChild(removeButton);
    return row;
  }

  function prettyJson(value) {
    try {
      return JSON.stringify(value, null, 2);
    } catch (e) {
      return String(value);
    }
  }

  function highlightCodeElement(block) {
    if (!block) {
      return;
    }
    if (window.Prism && typeof window.Prism.highlightElement === 'function') {
      window.Prism.highlightElement(block);
    }
  }

  function highlightJsonBlocks() {
    const jsonBlocks = document.querySelectorAll('pre.json-result code');
    if (!jsonBlocks.length) {
      return;
    }
    jsonBlocks.forEach(highlightCodeElement);
  }

  function initRequestBodyPreview(bodyId, contentTypeId, previewId) {
    const bodyInput = document.getElementById(bodyId);
    const contentTypeInput = document.getElementById(contentTypeId);
    const preview = document.getElementById(previewId);
    if (!bodyInput || !contentTypeInput || !preview) {
      return;
    }

    function renderPreview() {
      const contentType = contentTypeInput.value || '';
      const raw = bodyInput.value || '';
      let text = raw;
      let languageClass = 'language-plaintext';

      if (contentType === 'application/json') {
        languageClass = 'language-json';
        try {
          const parsed = JSON.parse(raw);
          text = JSON.stringify(parsed, null, 2);
        } catch (e) {
          text = raw;
        }
      }

      preview.className = languageClass;
      preview.textContent = text;
      highlightCodeElement(preview);
    }

    bodyInput.addEventListener('input', renderPreview);
    contentTypeInput.addEventListener('change', renderPreview);
    renderPreview();
  }

  function initPropertyEditor(listId, addButtonId, keyName, valueName) {
    const list = document.getElementById(listId);
    const addButton = document.getElementById(addButtonId);
    if (!list || !addButton) {
      return;
    }

    function addRow(key, value) {
      list.appendChild(createPropertyRow(keyName, valueName, key, value));
    }

    if (list.querySelectorAll('.property-row').length === 0) {
      addRow('', '');
    }

    addButton.addEventListener('click', function () {
      addRow('', '');
    });

    list.addEventListener('click', function (event) {
      const removeButton = event.target.closest('.property-remove');
      if (!removeButton) {
        return;
      }
      const row = removeButton.closest('.property-row');
      if (!row) {
        return;
      }
      const rows = list.querySelectorAll('.property-row');
      if (rows.length <= 1) {
        row.querySelectorAll('input').forEach(function (input) {
          input.value = '';
        });
        return;
      }
      row.remove();
    });
  }

  function initPartLoaderTester() {
    initPropertyEditor('extra-param-list', 'add-extra-param', 'extraParamKey', 'extraParamValue');
  }

  function initStaticPartTester() {
    initPropertyEditor('static-extra-param-list', 'add-static-extra-param', 'staticExtraParamKey', 'staticExtraParamValue');

    const deleteCheckbox = document.getElementById('static-delete');
    const bodyInput = document.getElementById('static-body');
    if (!deleteCheckbox || !bodyInput) {
      return;
    }

    function syncDeleteMode() {
      const isDelete = !!deleteCheckbox.checked;
      bodyInput.disabled = isDelete;
      bodyInput.placeholder = isDelete ? 'Delete mode enabled. Body is ignored.' : 'Static part value';
    }

    deleteCheckbox.addEventListener('change', syncDeleteMode);
    syncDeleteMode();
  }

  function initCommandTester() {
    initPropertyEditor('command-extra-param-list', 'add-command-extra-param', 'commandExtraParamKey', 'commandExtraParamValue');

    const foreachInput = document.getElementById('command-foreach');
    const idInput = document.getElementById('command-id');
    const propertiesList = document.getElementById('command-extra-param-list');
    const addPropertyButton = document.getElementById('add-command-extra-param');
    const buttons = Array.from(document.querySelectorAll('.command-op-button'));
    if (!foreachInput || !idInput || !buttons.length) {
      return;
    }

    function hasAdditionalProperties() {
      if (!propertiesList) {
        return false;
      }
      const rows = Array.from(propertiesList.querySelectorAll('.property-row'));
      return rows.some(function (row) {
        const keyInput = row.querySelector('input[name="commandExtraParamKey"]');
        return !!(keyInput && keyInput.value && keyInput.value.trim().length > 0);
      });
    }

    function syncState() {
      const forEachEnabled = !!foreachInput.checked;
      idInput.disabled = forEachEnabled;
      idInput.required = !forEachEnabled;
      idInput.placeholder = forEachEnabled ? 'Disabled in foreach mode' : 'e.g. user-42';

      const hasProperties = hasAdditionalProperties();
      buttons.forEach(function (button) {
        const op = button.getAttribute('data-op');
        let disabled = false;
        if (forEachEnabled && op === 'create') {
          disabled = true;
        }
        if (op === 'addProperties' && !hasProperties) {
          disabled = true;
        }
        button.disabled = disabled;
      });
    }

    foreachInput.addEventListener('change', syncState);
    if (propertiesList) {
      propertiesList.addEventListener('input', syncState);
      propertiesList.addEventListener('click', function () {
        window.setTimeout(syncState, 0);
      });
    }
    if (addPropertyButton) {
      addPropertyButton.addEventListener('click', function () {
        window.setTimeout(syncState, 0);
      });
    }
    syncState();
  }

  function initForeachToggle() {
    const foreachInput = document.getElementById('test-foreach');
    const idInput = document.getElementById('test-id');
    if (!foreachInput || !idInput) {
      return;
    }

    function syncForeachState() {
      const enabled = !!foreachInput.checked;
      idInput.disabled = enabled;
      idInput.required = !enabled;
      idInput.placeholder = enabled ? 'Disabled in foreach mode' : 'e.g. user-42';
    }

    foreachInput.addEventListener('change', syncForeachState);
    syncForeachState();
  }

  function initStopOperationWarning() {
    document.querySelectorAll('form[data-stop-operation="true"]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        const proceed = window.confirm(
          'This stop operation is irreversible and may cause data loss. Continue?'
        );
        if (!proceed) {
          event.preventDefault();
        }
      });
    });
  }

  function formatClockTime(value) {
    if (value === null || value === undefined || value === '') {
      return 'n/a';
    }

    function toDate(raw) {
      if (typeof raw === 'number' && Number.isFinite(raw)) {
        const millis = Math.abs(raw) < 100000000000 ? raw * 1000 : raw;
        return new Date(millis);
      }
      if (typeof raw === 'string') {
        const trimmed = raw.trim();
        if (!trimmed) {
          return null;
        }
        const numeric = Number(trimmed);
        if (Number.isFinite(numeric)) {
          const millis = Math.abs(numeric) < 100000000000 ? numeric * 1000 : numeric;
          return new Date(millis);
        }
        const parsed = Date.parse(trimmed);
        if (!Number.isNaN(parsed)) {
          return new Date(parsed);
        }
      }
      return null;
    }

    const date = toDate(value);
    if (!date || Number.isNaN(date.getTime())) {
      return String(value);
    }

    return new Intl.DateTimeFormat(undefined, {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date);
  }

  function toScalar(value) {
    if (value === null || value === undefined) {
      return 'null';
    }
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return String(value);
    }
    return prettyJson(value);
  }

  function buildStatusLine(status) {
    if (!status || typeof status !== 'object') {
      return 'No status details';
    }
    if (status.summaryLine) {
      return String(status.summaryLine);
    }

    const parts = [];
    if (status.httpStatus !== null && status.httpStatus !== undefined && status.httpStatus !== '') {
      parts.push('HTTP ' + status.httpStatus);
    }
    if (Object.prototype.hasOwnProperty.call(status, 'result')) {
      parts.push('result=' + toScalar(status.result));
    }
    if (status.status) {
      parts.push('status=' + status.status);
    }
    if (status.errorCode) {
      parts.push('errorCode=' + status.errorCode);
    }
    if (status.errorMessage) {
      parts.push('errorMessage=' + status.errorMessage);
    }
    if (status.responseTime) {
      parts.push('response=' + status.responseTime);
    }
    return parts.join(' | ');
  }

  function watchEventKey(watch, payload) {
    const props = payload && payload.properties ? payload.properties : {};
    return [
      props.watchId || (watch && watch.watchId) || '',
      props.eventType || '',
      payload && payload.timestamp ? payload.timestamp : '',
      payload && payload.correlationId ? payload.correlationId : '',
      payload && payload.status ? payload.status : '',
      payload && payload.errorCode ? payload.errorCode : '',
      payload && payload.errorMessage ? payload.errorMessage : ''
    ].join('|');
  }

  function tabEventKey(entry) {
    if (entry && entry.dedupeKey) {
      return String(entry.dedupeKey);
    }
    const meta = entry && entry.meta ? entry.meta : {};
    const keyPayload = {
      t: entry.timestamp || '',
      sl: entry.statusLine || '',
      et: meta.eventType || '',
      cid: meta.correlationId || '',
      p: entry.payload
    };
    return JSON.stringify(keyPayload);
  }

  function initOutputDock() {
    const dock = document.getElementById('output-dock');
    const openButton = document.getElementById('output-open-button');
    const closeButton = document.getElementById('output-close-button');
    const resizeHandle = document.getElementById('output-resize-handle');
    const tabList = document.getElementById('output-tab-list');
    const emptyPanel = document.getElementById('output-empty');
    const content = document.getElementById('output-content');
    const timelineList = document.getElementById('output-timeline-list');
    const seeAllCheckbox = document.getElementById('output-see-all');
    const prevButton = document.getElementById('output-prev-button');
    const nextButton = document.getElementById('output-next-button');
    const clearButton = document.getElementById('output-clear-button');
    const statusLine = document.getElementById('output-status-line');
    const jsonCode = document.getElementById('output-json');
    const fontDecreaseButton = document.getElementById('output-font-decrease');
    const fontIncreaseButton = document.getElementById('output-font-increase');
    const fontSizeLabel = document.getElementById('output-font-size-label');
    const watchLimitInput = document.getElementById('output-watch-limit');
    const conveyorLimitInput = document.getElementById('output-conveyor-limit');
    const jsonPathInput = document.getElementById('output-jsonpath');
    const watchLimitHiddenInputs = Array.from(document.querySelectorAll('input.watch-limit-hidden'));
    const configuredDefaultWatchHistory = dock && dock.dataset ? Number(dock.dataset.defaultWatchHistoryLimit) : NaN;
    const configuredDefaultConveyorHistory = dock && dock.dataset ? Number(dock.dataset.defaultConveyorHistoryLimit) : NaN;

    function clampWatchHistoryLimit(limit) {
      const numeric = Number(limit);
      if (!Number.isFinite(numeric)) {
        return WATCH_HISTORY_DEFAULT_LIMIT;
      }
      return Math.max(Math.round(numeric), WATCH_HISTORY_MIN_LIMIT);
    }

    const defaultWatchHistoryLimit = clampWatchHistoryLimit(
      Number.isFinite(configuredDefaultWatchHistory) ? configuredDefaultWatchHistory : WATCH_HISTORY_DEFAULT_LIMIT
    );
    const defaultConveyorHistoryLimit = clampWatchHistoryLimit(
      Number.isFinite(configuredDefaultConveyorHistory) ? configuredDefaultConveyorHistory : WATCH_HISTORY_DEFAULT_LIMIT
    );

    if (!dock || !openButton || !closeButton || !resizeHandle || !tabList || !emptyPanel || !content || !timelineList || !seeAllCheckbox || !prevButton || !nextButton || !clearButton || !statusLine || !jsonCode || !fontDecreaseButton || !fontIncreaseButton || !fontSizeLabel || !watchLimitInput || !conveyorLimitInput || !jsonPathInput) {
      return {
        pushConveyorEvent: function () {},
        pushWatchEvent: function () {},
        focusWatchTab: function () {},
        open: function () {},
        getWatchHistoryLimit: function () { return defaultWatchHistoryLimit; }
      };
    }

    const state = {
      tabs: new Map(),
      selectedTabId: null,
      open: true,
      height: OUTPUT_DEFAULT_HEIGHT,
      fontSize: OUTPUT_DEFAULT_FONT_SIZE,
      watchHistoryLimit: defaultWatchHistoryLimit,
      conveyorHistoryLimit: defaultConveyorHistoryLimit,
      jsonPath: '$.payload'
    };
    let watchHistoryLimitListener = null;

    function maxDockHeight() {
      return Math.max(OUTPUT_MIN_HEIGHT, Math.floor(window.innerHeight * 0.75));
    }

    function clampHeight(height) {
      const numeric = Number(height);
      if (!Number.isFinite(numeric)) {
        return OUTPUT_DEFAULT_HEIGHT;
      }
      return Math.min(Math.max(Math.round(numeric), OUTPUT_MIN_HEIGHT), maxDockHeight());
    }

    function clampFontSize(size) {
      const numeric = Number(size);
      if (!Number.isFinite(numeric)) {
        return OUTPUT_DEFAULT_FONT_SIZE;
      }
      return Math.min(Math.max(Math.round(numeric), OUTPUT_MIN_FONT_SIZE), OUTPUT_MAX_FONT_SIZE);
    }

    function normalizeJsonPath(path) {
      const trimmed = typeof path === 'string' ? path.trim() : '';
      if (!trimmed) {
        return '$';
      }
      if (trimmed === '$') {
        return '$';
      }
      if (trimmed.startsWith('$')) {
        return trimmed;
      }
      if (trimmed.startsWith('.') || trimmed.startsWith('[')) {
        return '$' + trimmed;
      }
      return '$.' + trimmed;
    }

    function parseJsonPathTokens(path) {
      if (!path.startsWith('$')) {
        throw new Error('JSONPath must start with "$"');
      }
      const tokens = [];
      let index = 1;
      while (index < path.length) {
        const ch = path[index];
        if (ch === '.') {
          index += 1;
          if (index >= path.length) {
            throw new Error('Invalid JSONPath: trailing "."');
          }
          if (path[index] === '*') {
            tokens.push({ type: 'wildcard' });
            index += 1;
            continue;
          }
          const start = index;
          while (index < path.length && /[A-Za-z0-9_$]/.test(path[index])) {
            index += 1;
          }
          if (start === index) {
            throw new Error('Invalid JSONPath property segment');
          }
          tokens.push({ type: 'prop', value: path.slice(start, index) });
          continue;
        }
        if (ch === '[') {
          index += 1;
          if (index >= path.length) {
            throw new Error('Invalid JSONPath: unclosed "["');
          }
          const inner = path[index];
          if (inner === '*') {
            index += 1;
            if (path[index] !== ']') {
              throw new Error('Invalid JSONPath wildcard segment');
            }
            index += 1;
            tokens.push({ type: 'wildcard' });
            continue;
          }
          if (inner === '"' || inner === '\'') {
            const quote = inner;
            index += 1;
            const start = index;
            while (index < path.length && path[index] !== quote) {
              if (path[index] === '\\' && index + 1 < path.length) {
                index += 2;
              } else {
                index += 1;
              }
            }
            if (index >= path.length) {
              throw new Error('Invalid JSONPath: unclosed quoted property');
            }
            const raw = path.slice(start, index).replace(/\\(['"])/g, '$1');
            index += 1;
            if (path[index] !== ']') {
              throw new Error('Invalid JSONPath: missing closing "]"');
            }
            index += 1;
            tokens.push({ type: 'prop', value: raw });
            continue;
          }
          const start = index;
          if (path[index] === '-') {
            index += 1;
          }
          while (index < path.length && /[0-9]/.test(path[index])) {
            index += 1;
          }
          const rawIndex = path.slice(start, index);
          if (!/^[-]?\d+$/.test(rawIndex)) {
            throw new Error('Invalid JSONPath array index');
          }
          if (path[index] !== ']') {
            throw new Error('Invalid JSONPath: missing closing "]"');
          }
          index += 1;
          tokens.push({ type: 'index', value: Number(rawIndex) });
          continue;
        }
        if (/\s/.test(ch)) {
          index += 1;
          continue;
        }
        throw new Error('Invalid JSONPath near "' + path.slice(index) + '"');
      }
      return tokens;
    }

    function applyJsonPathToken(values, token) {
      const next = [];
      values.forEach(function (value) {
        if (token.type === 'prop') {
          if (Array.isArray(value)) {
            value.forEach(function (item) {
              if (item && typeof item === 'object' && Object.prototype.hasOwnProperty.call(item, token.value)) {
                next.push(item[token.value]);
              }
            });
          } else if (value && typeof value === 'object' && Object.prototype.hasOwnProperty.call(value, token.value)) {
            next.push(value[token.value]);
          }
          return;
        }
        if (token.type === 'index') {
          if (!Array.isArray(value)) {
            return;
          }
          let index = token.value;
          if (index < 0) {
            index = value.length + index;
          }
          if (index >= 0 && index < value.length) {
            next.push(value[index]);
          }
          return;
        }
        if (token.type === 'wildcard') {
          if (Array.isArray(value)) {
            next.push.apply(next, value);
          } else if (value && typeof value === 'object') {
            next.push.apply(next, Object.values(value));
          }
        }
      });
      return next;
    }

    function evaluateJsonPath(source, path) {
      const normalized = normalizeJsonPath(path);
      if (normalized === '$') {
        return source;
      }
      const tokens = parseJsonPathTokens(normalized);
      let values = [source];
      tokens.forEach(function (token) {
        values = applyJsonPathToken(values, token);
      });
      if (values.length === 0) {
        return null;
      }
      return values.length === 1 ? values[0] : values;
    }

    function resolveTabHistoryLimit(tab) {
      if (!tab) {
        return state.watchHistoryLimit;
      }
      const numeric = Number(tab.historyLimit);
      if (Number.isFinite(numeric) && numeric > 0) {
        return clampWatchHistoryLimit(numeric);
      }
      return state.watchHistoryLimit;
    }

    function savePrefs() {
      const prefs = {
        open: state.open,
        height: state.height,
        fontSize: state.fontSize,
        watchHistoryLimit: state.watchHistoryLimit,
        conveyorHistoryLimit: state.conveyorHistoryLimit,
        jsonPath: state.jsonPath
      };
      window.localStorage.setItem(OUTPUT_DOCK_PREFS_KEY, JSON.stringify(prefs));
    }

    function saveConveyorHistory() {
      const payload = {
        selectedTabId: state.selectedTabId,
        tabs: Array.from(state.tabs.values())
          .filter(function (tab) { return tab.type === 'conveyor'; })
          .map(function (tab) {
            return {
              tabId: tab.tabId,
              title: tab.title,
              type: 'conveyor',
              sourceKey: tab.sourceKey,
              events: tab.events,
              selectedEventIndex: tab.selectedEventIndex,
              seeAll: tab.seeAll === true,
              followTail: tab.followTail !== false
            };
          })
      };
      window.sessionStorage.setItem(OUTPUT_CONVEYOR_HISTORY_STORAGE_KEY, JSON.stringify(payload));
    }

    function loadConveyorHistory() {
      const raw = window.sessionStorage.getItem(OUTPUT_CONVEYOR_HISTORY_STORAGE_KEY);
      if (!raw) {
        return;
      }
      try {
        const parsed = JSON.parse(raw);
        const tabs = Array.isArray(parsed.tabs) ? parsed.tabs : [];
        tabs.forEach(function (savedTab) {
          if (!savedTab || !savedTab.tabId || savedTab.type !== 'conveyor') {
            return;
          }
          const tab = ensureTab(
            savedTab.tabId,
            savedTab.title || savedTab.sourceKey || savedTab.tabId,
            'conveyor',
            savedTab.sourceKey || savedTab.tabId,
            state.conveyorHistoryLimit
          );
          tab.events = [];
          tab.eventKeys.clear();
          const events = Array.isArray(savedTab.events) ? savedTab.events : [];
          events.forEach(function (eventRecord) {
            if (!eventRecord || typeof eventRecord !== 'object') {
              return;
            }
            addEvent(tab, eventRecord);
          });
          tab.seeAll = savedTab.seeAll === true;
          tab.followTail = savedTab.followTail !== false;
          if (Number.isInteger(savedTab.selectedEventIndex)) {
            tab.selectedEventIndex = savedTab.selectedEventIndex;
          } else if (tab.events.length > 0) {
            tab.selectedEventIndex = tab.events.length - 1;
          } else {
            tab.selectedEventIndex = -1;
          }
          if (tab.events.length === 0) {
            tab.selectedEventIndex = -1;
          } else if (tab.followTail) {
            tab.selectedEventIndex = tab.events.length - 1;
          } else {
            tab.selectedEventIndex = Math.min(Math.max(tab.selectedEventIndex, 0), tab.events.length - 1);
          }
        });
        if (typeof parsed.selectedTabId === 'string' && state.tabs.has(parsed.selectedTabId)) {
          state.selectedTabId = parsed.selectedTabId;
        }
      } catch (error) {
        console.error('Failed to load saved conveyor output history', error);
      }
    }

    function loadPrefs() {
      const raw = window.localStorage.getItem(OUTPUT_DOCK_PREFS_KEY);
      if (!raw) {
        return;
      }
      try {
        const prefs = JSON.parse(raw);
        if (typeof prefs.open === 'boolean') {
          state.open = prefs.open;
        }
        state.height = clampHeight(prefs.height);
        state.fontSize = clampFontSize(prefs.fontSize);
        state.watchHistoryLimit = clampWatchHistoryLimit(prefs.watchHistoryLimit);
        state.conveyorHistoryLimit = clampWatchHistoryLimit(prefs.conveyorHistoryLimit);
        state.jsonPath = normalizeJsonPath(prefs.jsonPath || '$.payload');
      } catch (e) {
        state.open = true;
        state.height = OUTPUT_DEFAULT_HEIGHT;
        state.fontSize = OUTPUT_DEFAULT_FONT_SIZE;
        state.watchHistoryLimit = defaultWatchHistoryLimit;
        state.conveyorHistoryLimit = defaultConveyorHistoryLimit;
        state.jsonPath = '$.payload';
      }
    }

    function syncWatchLimitHiddenInputs() {
      const currentLimit = String(state.watchHistoryLimit);
      watchLimitHiddenInputs.forEach(function (input) {
        input.value = currentLimit;
      });
    }

    function updateControlUi() {
      fontSizeLabel.textContent = state.fontSize + 'px';
      watchLimitInput.value = String(state.watchHistoryLimit);
      conveyorLimitInput.value = String(state.conveyorHistoryLimit);
      jsonPathInput.value = state.jsonPath;
      syncWatchLimitHiddenInputs();
    }

    function applyDockState() {
      dock.hidden = !state.open;
      openButton.hidden = state.open;
      if (state.open) {
        dock.style.height = state.height + 'px';
      }
      dock.style.setProperty('--output-font-size', state.fontSize + 'px');
      updateControlUi();
      savePrefs();
    }

    function ensureTab(tabId, title, type, sourceKey, historyLimit) {
      let tab = state.tabs.get(tabId);
      if (!tab) {
        tab = {
          tabId: tabId,
          title: title,
          type: type,
          sourceKey: sourceKey,
          events: [],
          eventKeys: new Set(),
          historyLimit: clampWatchHistoryLimit(historyLimit),
          selectedEventIndex: -1,
          seeAll: false,
          followTail: true
        };
        state.tabs.set(tabId, tab);
      } else {
        tab.title = title || tab.title;
        tab.type = type || tab.type;
        tab.sourceKey = sourceKey || tab.sourceKey;
        if (Number.isFinite(Number(historyLimit)) && Number(historyLimit) > 0) {
          tab.historyLimit = clampWatchHistoryLimit(historyLimit);
        }
        if (!Number.isInteger(tab.selectedEventIndex)) {
          tab.selectedEventIndex = -1;
        }
        if (typeof tab.seeAll !== 'boolean') {
          tab.seeAll = false;
        }
        if (typeof tab.followTail !== 'boolean') {
          tab.followTail = true;
        }
      }
      return tab;
    }

    function trimTab(tab) {
      if (!tab) {
        return 0;
      }
      const limit = resolveTabHistoryLimit(tab);
      let removedCount = 0;
      while (tab.events.length > limit) {
        const removed = tab.events.shift();
        if (removed) {
          tab.eventKeys.delete(tabEventKey(removed));
        }
        removedCount += 1;
      }
      if (removedCount > 0 && Number.isInteger(tab.selectedEventIndex)) {
        tab.selectedEventIndex -= removedCount;
      }
      if (tab.events.length === 0) {
        tab.selectedEventIndex = -1;
        tab.followTail = true;
      } else if (Number.isInteger(tab.selectedEventIndex)) {
        tab.selectedEventIndex = Math.min(Math.max(tab.selectedEventIndex, 0), tab.events.length - 1);
      }
      return removedCount;
    }

    function trimAllTabs() {
      state.tabs.forEach(trimTab);
    }

    function closeTab(tabId) {
      state.tabs.delete(tabId);
      if (state.selectedTabId === tabId) {
        state.selectedTabId = null;
      }
      render();
    }

    function selectTab(tabId) {
      if (!state.tabs.has(tabId)) {
        return;
      }
      state.selectedTabId = tabId;
      render();
    }

    function addEvent(tab, eventRecord) {
      const key = tabEventKey(eventRecord);
      if (tab.eventKeys.has(key)) {
        return;
      }
      const wasFollowingTail = tab.followTail === true;
      const wasAtTail = tab.events.length === 0 || tab.selectedEventIndex >= tab.events.length - 1;
      tab.eventKeys.add(key);
      tab.events.push(eventRecord);
      trimTab(tab);
      if (tab.events.length === 0) {
        tab.selectedEventIndex = -1;
        tab.followTail = true;
        return;
      }
      if (wasFollowingTail || wasAtTail) {
        tab.selectedEventIndex = tab.events.length - 1;
        tab.followTail = true;
      } else if (!Number.isInteger(tab.selectedEventIndex)) {
        tab.selectedEventIndex = tab.events.length - 1;
        tab.followTail = true;
      } else {
        tab.selectedEventIndex = Math.min(Math.max(tab.selectedEventIndex, 0), tab.events.length - 1);
        tab.followTail = tab.selectedEventIndex === tab.events.length - 1;
      }
    }

    function sortTabs(tabs) {
      return tabs.sort(function (a, b) {
        const at = a.title || a.tabId;
        const bt = b.title || b.tabId;
        return at.localeCompare(bt);
      });
    }

    function renderTabs() {
      const tabs = sortTabs(Array.from(state.tabs.values()));
      tabList.innerHTML = '';

      tabs.forEach(function (tab) {
        const wrapper = document.createElement('div');
        wrapper.className = 'output-tab-item';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'output-tab-button';
        if (tab.tabId === state.selectedTabId) {
          button.classList.add('active');
          button.setAttribute('aria-selected', 'true');
        } else {
          button.setAttribute('aria-selected', 'false');
        }
        button.setAttribute('role', 'tab');
        button.textContent = tab.title;
        button.title = tab.sourceKey || tab.title;
        button.addEventListener('click', function () {
          selectTab(tab.tabId);
        });

        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'output-tab-close';
        close.textContent = 'X';
        close.title = 'Close output tab';
        close.addEventListener('click', function (event) {
          event.stopPropagation();
          closeTab(tab.tabId);
        });

        wrapper.appendChild(button);
        wrapper.appendChild(close);
        tabList.appendChild(wrapper);
      });
    }

    function selectedTab() {
      if (state.selectedTabId && state.tabs.has(state.selectedTabId)) {
        return state.tabs.get(state.selectedTabId);
      }
      const first = sortTabs(Array.from(state.tabs.values()))[0];
      if (!first) {
        return null;
      }
      state.selectedTabId = first.tabId;
      return first;
    }

    function ensureSelectedEvent(tab) {
      if (!tab || tab.events.length === 0) {
        if (tab) {
          tab.selectedEventIndex = -1;
          tab.followTail = true;
        }
        return -1;
      }
      if (!Number.isInteger(tab.selectedEventIndex) || tab.selectedEventIndex < 0) {
        tab.selectedEventIndex = tab.events.length - 1;
      }
      if (tab.selectedEventIndex >= tab.events.length) {
        tab.selectedEventIndex = tab.events.length - 1;
      }
      tab.followTail = tab.selectedEventIndex === tab.events.length - 1;
      return tab.selectedEventIndex;
    }

    function eventStatusClass(entry) {
      if (!entry || !entry.status) {
        return 'output-status-neutral';
      }
      const rawCode = Number(entry.status.httpStatus);
      if (Number.isFinite(rawCode)) {
        if (rawCode >= 200 && rawCode < 300) {
          return 'output-status-2xx';
        }
        if (rawCode >= 300 && rawCode < 400) {
          return 'output-status-3xx';
        }
        if (rawCode >= 400 && rawCode < 500) {
          return 'output-status-4xx';
        }
        if (rawCode >= 500) {
          return 'output-status-5xx';
        }
      }
      if (entry.status.errorCode || entry.status.errorMessage) {
        return 'output-status-5xx';
      }
      if (entry.status.result === true) {
        return 'output-status-2xx';
      }
      if (entry.status.result === false) {
        return 'output-status-4xx';
      }
      return 'output-status-neutral';
    }

    function renderTimeline(tab) {
      timelineList.innerHTML = '';
      if (!tab || tab.events.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'output-timeline-empty';
        empty.textContent = 'No cached events.';
        timelineList.appendChild(empty);
        prevButton.disabled = true;
        nextButton.disabled = true;
        clearButton.disabled = true;
        seeAllCheckbox.checked = tab ? tab.seeAll === true : false;
        seeAllCheckbox.disabled = true;
        return;
      }

      const selectedIndex = ensureSelectedEvent(tab);
      const seeAllEnabled = tab.seeAll === true;

      tab.events.forEach(function (entry, index) {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'output-timeline-item ' + eventStatusClass(entry);
        item.textContent = formatClockTime(entry.timestamp || new Date().toISOString());
        item.title = entry.statusLine || 'output event';
        if (!seeAllEnabled && index === selectedIndex) {
          item.classList.add('active');
        }
        item.disabled = seeAllEnabled;
        item.addEventListener('click', function () {
          if (tab.seeAll === true) {
            return;
          }
          tab.selectedEventIndex = index;
          tab.followTail = index === tab.events.length - 1;
          render();
        });
        timelineList.appendChild(item);
      });

      const atStart = selectedIndex <= 0;
      const atEnd = selectedIndex >= tab.events.length - 1;
      prevButton.disabled = seeAllEnabled || atStart;
      nextButton.disabled = seeAllEnabled || atEnd;
      clearButton.disabled = false;
      seeAllCheckbox.checked = seeAllEnabled;
      seeAllCheckbox.disabled = false;
    }

    function renderContent() {
      const tab = selectedTab();
      if (!tab) {
        emptyPanel.hidden = false;
        content.hidden = true;
        timelineList.innerHTML = '';
        prevButton.disabled = true;
        nextButton.disabled = true;
        clearButton.disabled = true;
        seeAllCheckbox.checked = false;
        seeAllCheckbox.disabled = true;
        return;
      }

      emptyPanel.hidden = true;
      content.hidden = false;
      renderTimeline(tab);

      if (tab.events.length === 0) {
        statusLine.textContent = 'No output events yet.';
        jsonCode.textContent = prettyJson([]);
        highlightCodeElement(jsonCode);
        return;
      }

      let payloadForView;
      if (tab.seeAll === true) {
        const latest = tab.events[tab.events.length - 1];
        statusLine.textContent = 'Showing all events (' + tab.events.length + '). Latest: ' + (latest ? latest.statusLine : 'n/a');
        payloadForView = tab.events.map(function (entry) {
          return {
            timestamp: entry.timestamp,
            status: entry.status,
            meta: entry.meta,
            payload: entry.payload
          };
        });
      } else {
        const selectedIndex = ensureSelectedEvent(tab);
        const selectedEvent = tab.events[selectedIndex];
        statusLine.textContent = selectedEvent ? selectedEvent.statusLine : 'No output events yet.';
        payloadForView = selectedEvent ? {
          timestamp: selectedEvent.timestamp,
          status: selectedEvent.status,
          meta: selectedEvent.meta,
          payload: selectedEvent.payload
        } : [];
      }
      let payloadToDisplay = payloadForView;
      try {
        payloadToDisplay = evaluateJsonPath(payloadForView, state.jsonPath);
      } catch (error) {
        payloadToDisplay = {
          jsonPathError: error && error.message ? error.message : String(error),
          path: state.jsonPath,
          source: payloadForView
        };
      }
      const payloadText = prettyJson(payloadToDisplay);
      jsonCode.textContent = payloadText;
      highlightCodeElement(jsonCode);
    }

    function render() {
      renderTabs();
      renderContent();
      applyDockState();
      saveConveyorHistory();
    }

    function pushConveyorEvent(event) {
      if (!event || !event.sourceKey) {
        return;
      }
      const tabId = 'conveyor:' + event.sourceKey;
      const tab = ensureTab(tabId, event.title || event.sourceKey, 'conveyor', event.sourceKey, state.conveyorHistoryLimit);
      const status = event.status || {};
      addEvent(tab, {
        timestamp: new Date().toISOString(),
        statusLine: buildStatusLine(status),
        status: status,
        meta: {
          sourceType: 'conveyor',
          sourceKey: event.sourceKey
        },
        payload: event.payload
      });
      state.selectedTabId = tabId;
      state.open = true;
      render();
    }

    function pushWatchEvent(watch, payload) {
      if (!watch || !watch.watchId || !payload) {
        return;
      }
      const tabId = 'watch:' + watch.watchId;
      const tab = ensureTab(
        tabId,
        watch.displayName || watch.watchId,
        'watch',
        watch.watchId,
        watch.foreach ? watch.historyLimit : 1
      );

      const props = payload.properties || {};
      const eventType = props.eventType || 'EVENT';
      const status = {
        httpStatus: null,
        result: payload.result,
        status: payload.status || eventType,
        errorCode: payload.errorCode || null,
        errorMessage: payload.errorMessage || null,
        responseTime: null,
        summaryLine: [
          'event=' + eventType,
          'time=' + formatClockTime(payload.timestamp),
          payload.status ? 'status=' + payload.status : null,
          payload.correlationId ? 'id=' + payload.correlationId : null,
          payload.errorCode ? 'errorCode=' + payload.errorCode : null,
          payload.errorMessage ? 'errorMessage=' + payload.errorMessage : null
        ].filter(Boolean).join(' | ')
      };

      addEvent(tab, {
        dedupeKey: watchEventKey(watch, payload),
        timestamp: payload.timestamp || new Date().toISOString(),
        statusLine: status.summaryLine,
        status: status,
        meta: {
          sourceType: 'watch',
          sourceKey: watch.watchId,
          eventType: eventType,
          correlationId: payload.correlationId || null
        },
        payload: payload
      });

      if (!state.selectedTabId) {
        state.selectedTabId = tabId;
      }
      render();
    }

    function focusWatchTab(watchId, displayName) {
      if (!watchId) {
        return;
      }
      const tabId = 'watch:' + watchId;
      ensureTab(tabId, displayName || watchId, 'watch', watchId, 1);
      state.selectedTabId = tabId;
      state.open = true;
      render();
    }

    function openDock() {
      state.open = true;
      render();
    }

    function setFontSize(value) {
      state.fontSize = clampFontSize(value);
      applyDockState();
    }

    function setWatchHistoryLimit(value) {
      state.watchHistoryLimit = clampWatchHistoryLimit(value);
      state.tabs.forEach(function (tab) {
        if (tab.type === 'watch') {
          tab.historyLimit = tab.sourceKey && tab.sourceKey.endsWith('|*') ? state.watchHistoryLimit : 1;
        }
      });
      trimAllTabs();
      render();
      if (typeof watchHistoryLimitListener === 'function') {
        watchHistoryLimitListener(state.watchHistoryLimit);
      }
    }

    function setConveyorHistoryLimit(value) {
      state.conveyorHistoryLimit = clampWatchHistoryLimit(value);
      state.tabs.forEach(function (tab) {
        if (tab.type === 'conveyor') {
          tab.historyLimit = state.conveyorHistoryLimit;
        }
      });
      trimAllTabs();
      render();
    }

    function setJsonPath(value) {
      state.jsonPath = normalizeJsonPath(value);
      render();
    }

    function navigateSelectedTab(offset) {
      const tab = selectedTab();
      if (!tab || tab.events.length === 0 || tab.seeAll === true) {
        return;
      }
      const current = ensureSelectedEvent(tab);
      const next = Math.min(Math.max(current + offset, 0), tab.events.length - 1);
      tab.selectedEventIndex = next;
      tab.followTail = next === tab.events.length - 1;
      render();
    }

    function clearSelectedTabEvents() {
      const tab = selectedTab();
      if (!tab) {
        return;
      }
      tab.events = [];
      tab.eventKeys.clear();
      tab.selectedEventIndex = -1;
      tab.followTail = true;
      render();
    }

    closeButton.addEventListener('click', function () {
      state.open = false;
      render();
    });

    openButton.addEventListener('click', function () {
      openDock();
    });

    fontDecreaseButton.addEventListener('click', function () {
      setFontSize(state.fontSize - 1);
    });

    fontIncreaseButton.addEventListener('click', function () {
      setFontSize(state.fontSize + 1);
    });

    seeAllCheckbox.addEventListener('change', function () {
      const tab = selectedTab();
      if (!tab) {
        seeAllCheckbox.checked = false;
        return;
      }
      tab.seeAll = seeAllCheckbox.checked === true;
      if (!tab.seeAll && tab.events.length > 0) {
        ensureSelectedEvent(tab);
      }
      render();
    });

    prevButton.addEventListener('click', function () {
      navigateSelectedTab(-1);
    });

    nextButton.addEventListener('click', function () {
      navigateSelectedTab(1);
    });

    clearButton.addEventListener('click', function () {
      clearSelectedTabEvents();
    });

    watchLimitInput.addEventListener('change', function () {
      setWatchHistoryLimit(watchLimitInput.value);
    });

    watchLimitInput.addEventListener('input', function () {
      setWatchHistoryLimit(watchLimitInput.value);
    });

    watchLimitInput.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        setWatchHistoryLimit(watchLimitInput.value);
      }
    });

    watchLimitInput.addEventListener('blur', function () {
      setWatchHistoryLimit(watchLimitInput.value);
    });

    conveyorLimitInput.addEventListener('change', function () {
      setConveyorHistoryLimit(conveyorLimitInput.value);
    });

    conveyorLimitInput.addEventListener('input', function () {
      setConveyorHistoryLimit(conveyorLimitInput.value);
    });

    conveyorLimitInput.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        setConveyorHistoryLimit(conveyorLimitInput.value);
      }
    });

    conveyorLimitInput.addEventListener('blur', function () {
      setConveyorHistoryLimit(conveyorLimitInput.value);
    });

    jsonPathInput.addEventListener('change', function () {
      setJsonPath(jsonPathInput.value);
    });

    jsonPathInput.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        setJsonPath(jsonPathInput.value);
      }
    });

    jsonPathInput.addEventListener('blur', function () {
      setJsonPath(jsonPathInput.value);
    });

    resizeHandle.addEventListener('mousedown', function (event) {
      event.preventDefault();
      const startY = event.clientY;
      const startHeight = state.height;

      function onMove(moveEvent) {
        const nextHeight = startHeight + (startY - moveEvent.clientY);
        state.height = clampHeight(nextHeight);
        dock.style.height = state.height + 'px';
      }

      function onUp() {
        window.removeEventListener('mousemove', onMove);
        window.removeEventListener('mouseup', onUp);
        savePrefs();
      }

      window.addEventListener('mousemove', onMove);
      window.addEventListener('mouseup', onUp);
    });

    window.addEventListener('resize', function () {
      state.height = clampHeight(state.height);
      if (!dock.hidden) {
        dock.style.height = state.height + 'px';
      }
      savePrefs();
    });

    loadPrefs();
    state.height = clampHeight(state.height);
    state.fontSize = clampFontSize(state.fontSize);
    state.watchHistoryLimit = clampWatchHistoryLimit(state.watchHistoryLimit);
    state.conveyorHistoryLimit = clampWatchHistoryLimit(state.conveyorHistoryLimit);
    loadConveyorHistory();
    trimAllTabs();
    render();

    return {
      pushConveyorEvent: pushConveyorEvent,
      pushWatchEvent: pushWatchEvent,
      focusWatchTab: focusWatchTab,
      open: openDock,
      getWatchHistoryLimit: function () { return state.watchHistoryLimit; },
      onWatchHistoryLimitChange: function (listener) {
        watchHistoryLimitListener = typeof listener === 'function' ? listener : null;
      }
    };
  }

  function watchHasData(watch) {
    return watch.events.some(function (event) {
      return event && event.properties && (event.properties.eventType === 'RESULT' || event.properties.eventType === 'SCRAP');
    });
  }

  function latestDataEvent(watch) {
    for (let i = watch.events.length - 1; i >= 0; i -= 1) {
      const event = watch.events[i];
      if (!event || !event.properties) {
        continue;
      }
      const type = event.properties.eventType;
      if (type === 'RESULT' || type === 'SCRAP') {
        return event;
      }
    }
    return null;
  }

  function initWatchPanel(outputDock) {
    const tagsRoot = document.getElementById('watch-tags');
    const refreshButton = document.getElementById('watch-refresh');

    if (!tagsRoot || !refreshButton) {
      return {
        cancelWatchById: function () { return Promise.resolve(); }
      };
    }

    const state = {
      watches: new Map(),
      socket: null
    };

    function clampWatchHistoryLimit(limit) {
      const numeric = Number(limit);
      if (!Number.isFinite(numeric)) {
        return outputDock.getWatchHistoryLimit();
      }
      return Math.max(Math.round(numeric), WATCH_HISTORY_MIN_LIMIT);
    }

    function parseTime(value) {
      if (!value) {
        return null;
      }
      const parsed = Date.parse(value);
      if (Number.isNaN(parsed)) {
        return null;
      }
      return new Date(parsed);
    }

    function formatElapsedMillis(millis) {
      const safe = Math.max(0, Math.floor(Number(millis) || 0));
      const totalSeconds = Math.floor(safe / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      if (hours > 0) {
        return String(hours) + ':' + String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
      }
      return String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
    }

    function watchElapsedLabel(watch) {
      const pingWait = watch && watch.lastPing && watch.lastPing.result
        ? Number(watch.lastPing.result.waitMillis)
        : NaN;
      if (Number.isFinite(pingWait) && pingWait >= 0) {
        return formatElapsedMillis(pingWait);
      }
      const createdAt = parseTime(watch ? watch.createdAt : null);
      if (!createdAt) {
        return '00:00';
      }
      return formatElapsedMillis(Date.now() - createdAt.getTime());
    }

    function normalizeWatch(raw) {
      const currentDefaultLimit = outputDock.getWatchHistoryLimit();
      const watch = {
        watchId: raw.watchId,
        displayName: raw.displayName || raw.watchId,
        conveyor: raw.conveyor || '',
        correlationId: raw.correlationId || null,
        foreach: !!raw.foreach,
        active: raw.active !== false,
        historyLimit: raw.foreach ? currentDefaultLimit : 1,
        createdAt: raw.createdAt || null,
        lastDataAt: raw.lastDataAt || null,
        events: Array.isArray(raw.events) ? raw.events.slice() : [],
        eventKeys: new Set(),
        lastPing: null
      };
      if (watch.events.length > watch.historyLimit) {
        watch.events = watch.events.slice(watch.events.length - watch.historyLimit);
      }
      watch.events.forEach(function (event) {
        watch.eventKeys.add(watchEventKey(watch, event));
      });
      return watch;
    }

    function syncOutputHistory(watch) {
      watch.events.forEach(function (event) {
        outputDock.pushWatchEvent(watch, event);
      });
    }

    function upsertWatch(raw) {
      if (!raw || !raw.watchId) {
        return;
      }
      const existing = state.watches.get(raw.watchId);
      if (!existing) {
        const normalized = normalizeWatch(raw);
        state.watches.set(raw.watchId, normalized);
        syncOutputHistory(normalized);
        return;
      }
      const normalized = normalizeWatch(raw);
      existing.displayName = normalized.displayName;
      existing.conveyor = normalized.conveyor;
      existing.correlationId = normalized.correlationId;
      existing.foreach = normalized.foreach;
      existing.active = normalized.active;
      existing.historyLimit = normalized.historyLimit;
      existing.createdAt = normalized.createdAt;
      existing.lastDataAt = normalized.lastDataAt;
      existing.events = normalized.events;
      existing.eventKeys = normalized.eventKeys;
      syncOutputHistory(existing);
    }

    function renderTags() {
      const watches = Array.from(state.watches.values()).sort(function (a, b) {
        return a.displayName.localeCompare(b.displayName);
      });

      tagsRoot.innerHTML = '';
      if (!watches.length) {
        const empty = document.createElement('p');
        empty.className = 'meta';
        empty.textContent = 'No active watches.';
        tagsRoot.appendChild(empty);
        return;
      }

      watches.forEach(function (watch) {
        const item = document.createElement('div');
        item.className = 'watch-tag-item';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'watch-tag';

        const latest = latestDataEvent(watch);
        if (!watchHasData(watch)) {
          item.classList.add('watch-tag-waiting');
        } else if (latest && latest.properties && latest.properties.eventType === 'SCRAP') {
          item.classList.add('watch-tag-error');
        } else {
          item.classList.add('watch-tag-ready');
        }

        button.textContent = watch.displayName + ' · ' + watchElapsedLabel(watch);
        button.title = watch.watchId;
        button.addEventListener('click', function () {
          outputDock.focusWatchTab(watch.watchId, watch.displayName);
          outputDock.open();
        });

        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'watch-tag-close';
        close.textContent = 'X';
        close.title = 'Cancel watch';
        close.addEventListener('click', function (event) {
          event.stopPropagation();
          cancelWatchById(watch.watchId).catch(function (error) {
            console.error('Failed to cancel watch', error);
          });
        });

        item.appendChild(button);
        item.appendChild(close);
        tagsRoot.appendChild(item);
      });
    }

    function appendEvent(watch, payload) {
      const eventType = payload && payload.properties ? payload.properties.eventType : null;
      if (eventType === 'PING') {
        watch.lastPing = payload;
        return;
      }
      const key = watchEventKey(watch, payload);
      if (watch.eventKeys.has(key)) {
        return;
      }
      watch.eventKeys.add(key);
      watch.events.push(payload);
      while (watch.events.length > watch.historyLimit) {
        const removed = watch.events.shift();
        watch.eventKeys.delete(watchEventKey(watch, removed));
      }
      watch.lastDataAt = payload.timestamp || watch.lastDataAt;
    }

    function applyLocalHistoryLimit(limit) {
      const normalized = clampWatchHistoryLimit(limit);
      state.watches.forEach(function (watch) {
        if (!watch.foreach) {
          watch.historyLimit = 1;
          while (watch.events.length > 1) {
            const removed = watch.events.shift();
            watch.eventKeys.delete(watchEventKey(watch, removed));
          }
          return;
        }
        watch.historyLimit = normalized;
        while (watch.events.length > watch.historyLimit) {
          const removed = watch.events.shift();
          watch.eventKeys.delete(watchEventKey(watch, removed));
        }
      });
      renderTags();
    }

    async function persistHistoryLimit(limit) {
      await fetch(WATCH_HISTORY_LIMIT_API, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json'
        },
        body: JSON.stringify({ historyLimit: limit })
      });
    }

    function setHistoryLimit(limit) {
      const normalized = clampWatchHistoryLimit(limit);
      applyLocalHistoryLimit(normalized);
      persistHistoryLimit(normalized).catch(function (error) {
        console.error('Failed to update watcher history limit on server', error);
      });
    }

    function ensureWatchFromEvent(payload) {
      if (!payload || !payload.properties || !payload.properties.watchId) {
        return null;
      }
      const watchId = payload.properties.watchId;
      let watch = state.watches.get(watchId);
      if (!watch) {
        watch = normalizeWatch({
          watchId: watchId,
          displayName: payload.properties.displayName || watchId,
          conveyor: payload.properties.conveyor || '',
          correlationId: payload.correlationId || null,
          foreach: !!payload.properties.foreach,
          active: payload.properties.watchActive !== false,
          historyLimit: Number(payload.properties.historyLimit || (payload.properties.foreach ? outputDock.getWatchHistoryLimit() : 1)),
          events: []
        });
        state.watches.set(watchId, watch);
      }
      return watch;
    }

    function processSocketMessage(payload) {
      const watch = ensureWatchFromEvent(payload);
      if (!watch) {
        return;
      }

      const eventType = payload && payload.properties ? payload.properties.eventType : null;
      if (eventType === 'CANCELED') {
        outputDock.pushWatchEvent(watch, payload);
        state.watches.delete(watch.watchId);
        renderTags();
        return;
      }

      appendEvent(watch, payload);
      watch.active = payload.properties.watchActive !== false;
      if (eventType !== 'PING') {
        outputDock.pushWatchEvent(watch, payload);
      }
      renderTags();
    }

    async function loadWatches() {
      const response = await fetch(WATCH_LIST_API, {
        method: 'GET',
        headers: {
          Accept: 'application/json'
        }
      });
      if (!response.ok) {
        throw new Error('Watch API returned status ' + response.status);
      }
      const payload = await response.json();
      state.watches.clear();
      (Array.isArray(payload) ? payload : []).forEach(upsertWatch);
      renderTags();
    }

    async function cancelWatchById(watchId) {
      if (!watchId) {
        return;
      }

      await fetch(WATCH_CANCEL_API, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json'
        },
        body: JSON.stringify({ watchId: watchId })
      });

      const existing = state.watches.get(watchId);
      if (existing) {
        existing.active = false;
        state.watches.delete(watchId);
      }
      renderTags();
    }

    function connectSocket() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const socketUrl = protocol + '//' + window.location.host + WATCH_WS_PATH;
      const socket = new WebSocket(socketUrl);
      state.socket = socket;

      socket.addEventListener('message', function (event) {
        try {
          const payload = JSON.parse(event.data);
          processSocketMessage(payload);
        } catch (e) {
          console.error('Failed to parse watch event payload', e);
        }
      });

      socket.addEventListener('close', function () {
        window.setTimeout(function () {
          connectSocket();
        }, 2000);
      });
    }

    refreshButton.addEventListener('click', function () {
      loadWatches().catch(function (error) {
        console.error('Failed to refresh watches', error);
      });
    });

    loadWatches()
      .catch(function (error) {
        console.error('Failed to load watch list', error);
      })
      .finally(connectSocket);

    return {
      cancelWatchById: cancelWatchById,
      setHistoryLimit: setHistoryLimit
    };
  }

  async function loadTreeData() {
    const embedded = parseEmbeddedTree();
    try {
      const response = await fetch('/api/dashboard/tree', {
        method: 'GET',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) {
        throw new Error('Tree API returned status ' + response.status);
      }
      const apiTree = await response.json();
      if (apiTree && Object.keys(apiTree).length > 0) {
        return apiTree;
      }
      return embedded;
    } catch (e) {
      console.error('Failed to load conveyor tree from API', e);
      if (embedded && Object.keys(embedded).length > 0) {
        return embedded;
      }
      throw e;
    }
  }

  async function renderTree() {
    const root = document.getElementById('tree');
    if (!root) return;

    let data = {};
    try {
      data = await loadTreeData();
    } catch (e) {
      root.innerHTML = '<p>Failed to load conveyors. Refresh the page or check server logs.</p>';
      return;
    }
    const selected = selectedName();
    const tab = selectedTab();
    const keys = Object.keys(data || {}).sort();

    if (keys.length === 0) {
      root.innerHTML = '<p>No conveyors found.</p>';
      return;
    }

    const ul = document.createElement('ul');
    keys.forEach(function (name) {
      ul.appendChild(createNode(name, data[name], selected, tab));
    });

    root.innerHTML = '';
    root.appendChild(ul);
  }

  initTabs();
  initRequestBodyPreview('test-body', 'test-content-type', 'request-body-preview');
  initRequestBodyPreview('static-body', 'static-content-type', 'static-request-body-preview');
  initForeachToggle();
  initPartLoaderTester();
  initStaticPartTester();
  initCommandTester();
  initStopOperationWarning();

  const outputDock = initOutputDock();
  const watchPanel = initWatchPanel(outputDock);
  outputDock.onWatchHistoryLimitChange(watchPanel.setHistoryLimit);

  const outputEvent = parseEmbeddedOutputEvent();
  if (outputEvent) {
    outputDock.pushConveyorEvent(outputEvent);
  }

  highlightJsonBlocks();
  renderTree();
})();
