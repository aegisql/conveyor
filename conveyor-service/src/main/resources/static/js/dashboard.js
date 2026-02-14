(function () {
  const ACTIVE_TAB_STORAGE_KEY = 'dashboard.activeTab';
  const WATCH_LIST_API = '/api/dashboard/watch';
  const WATCH_CANCEL_API = '/api/dashboard/watch/cancel';
  const WATCH_WS_PATH = '/ws/watch';

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

  function highlightJsonBlocks() {
    const jsonBlocks = document.querySelectorAll('pre.json-result code');
    if (!jsonBlocks.length) {
      return;
    }
    if (window.Prism && typeof window.Prism.highlightElement === 'function') {
      jsonBlocks.forEach(function (block) {
        window.Prism.highlightElement(block);
      });
    }
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
      if (window.Prism && typeof window.Prism.highlightElement === 'function') {
        window.Prism.highlightElement(preview);
      }
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
    const watchCheckbox = document.getElementById('command-watch-results');
    const watchLimitInput = document.getElementById('command-watch-limit');
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

      if (watchLimitInput) {
        const watchEnabled = !!(watchCheckbox && watchCheckbox.checked);
        const limitEnabled = watchEnabled && forEachEnabled;
        watchLimitInput.disabled = !limitEnabled;
        watchLimitInput.placeholder = limitEnabled ? '100' : 'Only used with foreach watch';
      }

      const hasProperties = hasAdditionalProperties();
      buttons.forEach(function (button) {
        const op = button.getAttribute('data-op');
        let disabled = false;
        if (forEachEnabled && (op === 'completeExceptionally' || op === 'create')) {
          disabled = true;
        }
        if (op === 'addProperties' && !hasProperties) {
          disabled = true;
        }
        button.disabled = disabled;
      });
    }

    foreachInput.addEventListener('change', syncState);
    if (watchCheckbox) {
      watchCheckbox.addEventListener('change', syncState);
    }
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
    const watchCheckbox = document.getElementById('test-watch-results');
    const watchLimitInput = document.getElementById('test-watch-limit');
    if (!foreachInput || !idInput) {
      return;
    }

    function syncForeachState() {
      const enabled = !!foreachInput.checked;
      idInput.disabled = enabled;
      idInput.required = !enabled;
      idInput.placeholder = enabled ? 'Disabled in foreach mode' : 'e.g. user-42';

      if (watchLimitInput) {
        const watchEnabled = !!(watchCheckbox && watchCheckbox.checked);
        const limitEnabled = watchEnabled && enabled;
        watchLimitInput.disabled = !limitEnabled;
        watchLimitInput.placeholder = limitEnabled ? '100' : 'Only used with foreach watch';
      }
    }

    foreachInput.addEventListener('change', syncForeachState);
    if (watchCheckbox) {
      watchCheckbox.addEventListener('change', syncForeachState);
    }
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

  function prettyJson(value) {
    try {
      return JSON.stringify(value, null, 2);
    } catch (e) {
      return String(value);
    }
  }

  function initWatchPanel() {
    const tagsRoot = document.getElementById('watch-tags');
    const detailsRoot = document.getElementById('watch-details');
    const detailsTitle = document.getElementById('watch-details-title');
    const detailsStatus = document.getElementById('watch-details-status');
    const detailsJson = document.getElementById('watch-details-json');
    const refreshButton = document.getElementById('watch-refresh');
    const cancelButton = document.getElementById('watch-cancel');

    if (!tagsRoot || !detailsRoot || !detailsTitle || !detailsStatus || !detailsJson || !refreshButton || !cancelButton) {
      return;
    }

    const state = {
      watches: new Map(),
      selectedId: null,
      socket: null
    };

    function normalizeWatch(raw) {
      const watch = {
        watchId: raw.watchId,
        displayName: raw.displayName || raw.watchId,
        conveyor: raw.conveyor || '',
        correlationId: raw.correlationId || null,
        foreach: !!raw.foreach,
        active: raw.active !== false,
        historyLimit: Number(raw.historyLimit || (raw.foreach ? 100 : 1)) || (raw.foreach ? 100 : 1),
        createdAt: raw.createdAt || null,
        lastDataAt: raw.lastDataAt || null,
        events: Array.isArray(raw.events) ? raw.events.slice() : [],
        lastPing: null
      };
      if (watch.events.length > watch.historyLimit) {
        watch.events = watch.events.slice(watch.events.length - watch.historyLimit);
      }
      return watch;
    }

    function upsertWatch(raw) {
      if (!raw || !raw.watchId) {
        return;
      }
      const existing = state.watches.get(raw.watchId);
      if (!existing) {
        state.watches.set(raw.watchId, normalizeWatch(raw));
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
        detailsRoot.hidden = true;
        state.selectedId = null;
        return;
      }

      watches.forEach(function (watch) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'watch-tag';

        const latest = latestDataEvent(watch);
        if (!watchHasData(watch)) {
          button.classList.add('watch-tag-waiting');
        } else if (latest && latest.properties && latest.properties.eventType === 'SCRAP') {
          button.classList.add('watch-tag-error');
        } else {
          button.classList.add('watch-tag-ready');
        }

        if (state.selectedId === watch.watchId) {
          button.classList.add('active');
        }

        button.textContent = watch.displayName;
        button.title = watch.watchId;
        button.addEventListener('click', function () {
          state.selectedId = watch.watchId;
          renderTags();
          renderDetails();
        });
        tagsRoot.appendChild(button);
      });

      if (!state.selectedId || !state.watches.has(state.selectedId)) {
        state.selectedId = watches[0].watchId;
      }
      renderDetails();
    }

    function renderDetails() {
      if (!state.selectedId || !state.watches.has(state.selectedId)) {
        detailsRoot.hidden = true;
        return;
      }

      const watch = state.watches.get(state.selectedId);
      detailsRoot.hidden = false;
      detailsTitle.textContent = watch.displayName;

      const latest = latestDataEvent(watch);
      if (latest) {
        const eventType = latest.properties && latest.properties.eventType ? latest.properties.eventType : 'EVENT';
        detailsStatus.textContent = 'Latest event: ' + eventType + ' at ' + formatWatchTime(latest.timestamp);
      } else if (watch.lastPing && watch.lastPing.result) {
        const waitMillis = watch.lastPing.result.waitMillis;
        const seconds = typeof waitMillis === 'number' ? Math.floor(waitMillis / 1000) : 'n/a';
        detailsStatus.textContent = 'Waiting for result: ' + seconds + 's';
      } else {
        detailsStatus.textContent = 'Waiting for events.';
      }

      const detailsPayload = {
        watchId: watch.watchId,
        displayName: watch.displayName,
        conveyor: watch.conveyor,
        correlationId: watch.correlationId,
        foreach: watch.foreach,
        active: watch.active,
        historyLimit: watch.historyLimit,
        createdAt: watch.createdAt,
        lastDataAt: watch.lastDataAt,
        lastPing: watch.lastPing,
        events: watch.events
      };
      detailsJson.textContent = prettyJson(detailsPayload);
      if (window.Prism && typeof window.Prism.highlightElement === 'function') {
        window.Prism.highlightElement(detailsJson);
      }
    }

    function formatWatchTime(value) {
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

    function appendEvent(watch, payload) {
      const eventType = payload && payload.properties ? payload.properties.eventType : null;
      if (eventType === 'PING') {
        watch.lastPing = payload;
        return;
      }
      watch.events.push(payload);
      while (watch.events.length > watch.historyLimit) {
        watch.events.shift();
      }
      watch.lastDataAt = payload.timestamp || watch.lastDataAt;
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
          historyLimit: Number(payload.properties.historyLimit || (payload.properties.foreach ? 100 : 1)),
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
        state.watches.delete(watch.watchId);
        if (state.selectedId === watch.watchId) {
          state.selectedId = null;
        }
        renderTags();
        return;
      }

      appendEvent(watch, payload);
      watch.active = payload.properties.watchActive !== false;
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

    async function cancelCurrentWatch() {
      if (!state.selectedId || !state.watches.has(state.selectedId)) {
        return;
      }
      const watchId = state.selectedId;

      await fetch(WATCH_CANCEL_API, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json'
        },
        body: JSON.stringify({ watchId: watchId })
      });

      state.watches.delete(watchId);
      if (state.selectedId === watchId) {
        state.selectedId = null;
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

    cancelButton.addEventListener('click', function () {
      cancelCurrentWatch().catch(function (error) {
        console.error('Failed to cancel watch', error);
      });
    });

    loadWatches()
      .catch(function (error) {
        console.error('Failed to load watch list', error);
      })
      .finally(connectSocket);
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
  initWatchPanel();
  highlightJsonBlocks();
  renderTree();
})();
