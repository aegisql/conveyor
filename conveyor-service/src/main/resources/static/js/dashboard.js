(function () {
  const ACTIVE_TAB_STORAGE_KEY = 'dashboard.activeTab';

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
    if (name === selected) {
      link.classList.add('active');
    }
    li.appendChild(link);

    const children = Object.keys(subTree || {});
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
      if (enabled) {
        idInput.placeholder = 'Disabled in foreach mode';
      } else {
        idInput.placeholder = 'e.g. user-42';
      }
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
  highlightJsonBlocks();
  renderTree();
})();
