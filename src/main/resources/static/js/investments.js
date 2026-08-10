const API_URL = '/api/investments';
const fmtMoney = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const fmtPct = new Intl.NumberFormat('en-IN', { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtDate = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });

const INDIAN_STOCKS = [
  { name: "Reliance Industries Ltd.", symbol: "RELIANCE", exchange: "NSE" },
  { name: "Tata Consultancy Services Ltd.", symbol: "TCS", exchange: "NSE" },
  { name: "Infosys Ltd.", symbol: "INFY", exchange: "NSE" },
  { name: "HDFC Bank Ltd.", symbol: "HDFCBANK", exchange: "NSE" },
  { name: "ICICI Bank Ltd.", symbol: "ICICIBANK", exchange: "NSE" },
  { name: "State Bank of India", symbol: "SBIN", exchange: "NSE" },
  { name: "ITC Ltd.", symbol: "ITC", exchange: "NSE" },
  { name: "Larsen & Toubro Ltd.", symbol: "LT", exchange: "NSE" },
  { name: "Bharti Airtel Ltd.", symbol: "BHARTIARTL", exchange: "NSE" },
  { name: "Hindustan Unilever Ltd.", symbol: "HINDUNILVR", exchange: "NSE" },
  { name: "Maruti Suzuki India Ltd.", symbol: "MARUTI", exchange: "NSE" },
  { name: "Asian Paints Ltd.", symbol: "ASIANPAINT", exchange: "NSE" },
  { name: "Sun Pharma Inds Ltd.", symbol: "SUNPHARMA", exchange: "NSE" },
  { name: "Titan Company Ltd.", symbol: "TITAN", exchange: "NSE" }
];

let currentInvestments = [];
let currentSummary = null;
let eventSource = null;
let timeAgoInterval = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  initSearchList();
  fetchInvestments();
});

function initSearchList() {
  const datalist = document.getElementById('indian-stocks-list');
  if (datalist) {
    INDIAN_STOCKS.forEach(stock => {
      const option = document.createElement('option');
      option.value = stock.name;
      datalist.appendChild(option);
    });
  }
  
  const nameInput = document.getElementById('inv-name');
  if (nameInput) {
    nameInput.addEventListener('input', (e) => {
      const match = INDIAN_STOCKS.find(s => s.name === e.target.value);
      if (match) {
        document.getElementById('inv-symbol').value = match.symbol;
        document.getElementById('inv-exchange').value = match.exchange;
        document.getElementById('inv-type').value = 'STOCKS';
        window.toggleMarketFields();
      }
    });
  }
}

// Toast System
function showToast(title, msg, isError = false) {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast ${isError ? 'toast-error' : ''}`;
  toast.innerHTML = `<div class="toast-title">${title}</div><div class="toast-msg">${msg}</div>`;
  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('visible'));
  setTimeout(() => {
    toast.classList.remove('visible');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Format numbers
function formatColor(value, isPct = false) {
  if (value > 0) {
    const text = isPct ? '+' + fmtPct.format(value / 100) : '+' + fmtMoney.format(value);
    return `<span class="color-profit">${text}</span>`;
  } else if (value < 0) {
    const text = isPct ? fmtPct.format(value / 100) : fmtMoney.format(value);
    return `<span class="color-loss">${text}</span>`;
  }
  return isPct ? '0.00%' : fmtMoney.format(0);
}

// Update text with animation
function updateNodeText(id, newText) {
  const node = document.getElementById(id);
  if (!node) return;
  if (node.innerHTML !== newText) {
    node.innerHTML = newText;
    node.classList.remove('value-flash');
    void node.offsetWidth; // trigger reflow
    node.classList.add('value-flash');
  }
}

// API Calls
async function fetchInvestments() {
  document.getElementById('inv-loading').style.display = 'block';
  document.getElementById('inv-empty').style.display = 'none';
  document.getElementById('inv-table-wrapper').style.display = 'none';
  document.getElementById('inv-metrics').style.display = 'none';
  document.getElementById('allocation-section').style.display = 'none';

  try {
    const res = await fetch(`${API_URL}/summary`, { signal: AbortSignal.timeout(5000) });
    if (!res.ok) throw new Error('Failed to fetch investments');
    
    currentSummary = await res.json();
    currentInvestments = currentSummary.recentInvestments || [];
    
    renderDashboard();
    initSSE();
  } catch (err) {
    console.error(err);
    document.getElementById('inv-loading').innerText = 'Unable to load portfolio.';
  }
}

function initSSE() {
  if (eventSource) {
    eventSource.close();
  }
  eventSource = new EventSource(`${API_URL}/stream`);
  eventSource.addEventListener('portfolioUpdate', (e) => {
    const newSummary = JSON.parse(e.data);
    currentSummary = newSummary;
    currentInvestments = newSummary.recentInvestments || [];
    applyDynamicUpdate();
  });
  
  if (timeAgoInterval) clearInterval(timeAgoInterval);
  timeAgoInterval = setInterval(() => {
    updateMarketStatus();
  }, 1000);
}

function updateMarketStatus() {
  const statusEl = document.getElementById('market-status-indicator');
  const textEl = document.getElementById('market-status-text');
  
  if (!currentSummary || !currentSummary.marketStatus) return;
  
  statusEl.style.display = 'inline-block';
  
  let timeStr = '';
  if (currentSummary.lastUpdated) {
    const updated = new Date(currentSummary.lastUpdated);
    const now = new Date();
    const diffSec = Math.max(0, Math.floor((now - updated) / 1000));
    const diffMin = Math.floor(diffSec / 60);
    const diffHr = Math.floor(diffMin / 60);
    
    if (diffSec < 10) {
      timeStr = 'Updated just now';
    } else if (diffSec < 60) {
      timeStr = `Updated ${diffSec} sec ago`;
    } else if (diffMin < 60) {
      timeStr = `Updated ${diffMin} min ago`;
    } else {
      timeStr = `Updated ${diffHr} hr ago`;
    }
  }

  statusEl.className = 'market-status'; // reset
  if (currentSummary.marketStatus === 'LIVE') {
    textEl.innerText = `MARKET DATA · LIVE · ${timeStr}`;
    statusEl.classList.add('status-live');
  } else if (currentSummary.marketStatus === 'WAITING_FOR_TICK') {
    textEl.innerText = `MARKET DATA · WAITING FOR TICK...`;
    statusEl.classList.add('status-delayed');
  } else if (currentSummary.marketStatus === 'CLOSED') {
    textEl.innerText = `MARKET CLOSED · ${timeStr}`;
    statusEl.classList.add('status-closed');
  } else if (currentSummary.marketStatus === 'DELAYED') {
    textEl.innerText = `MARKET DATA · DELAYED · ${timeStr}`;
    statusEl.classList.add('status-delayed');
  } else if (currentSummary.marketStatus === 'CONNECTION_LOST') {
    textEl.innerText = `CONNECTION LOST · Last known · ${timeStr.replace('Updated', '').trim()}`;
    statusEl.classList.add('status-unavailable');
  } else {
    textEl.innerText = 'UNAVAILABLE';
    statusEl.classList.add('status-unavailable');
  }
}

function applyDynamicUpdate() {
  // Update totals
  updateNodeText('inv-total-invested', fmtMoney.format(currentSummary.totalInvested));
  updateNodeText('inv-current-value', fmtMoney.format(currentSummary.currentValue));
  updateNodeText('inv-total-return', formatColor(currentSummary.totalProfitLoss));
  updateNodeText('inv-return-pct', formatColor(currentSummary.returnPercentage, true));

  updateNodeText('perf-total-invested', fmtMoney.format(currentSummary.totalInvested));
  updateNodeText('perf-current-value', fmtMoney.format(currentSummary.currentValue));
  updateNodeText('perf-total-return', formatColor(currentSummary.totalProfitLoss));
  updateNodeText('perf-return-pct', formatColor(currentSummary.returnPercentage, true));
  
  updateMarketStatus();

  // Update allocation
  const alloc = currentSummary.assetAllocation || {};
  for (const [type, pct] of Object.entries(alloc)) {
    const safeType = type.replace(/\s+/g, '-');
    const bar = document.getElementById(`alloc-bar-${safeType}`);
    const pctNode = document.getElementById(`alloc-pct-${safeType}`);
    if (bar && pctNode) {
      bar.style.width = pct + '%';
      updateNodeText(`alloc-pct-${safeType}`, pct.toFixed(1) + '%');
    }
  }

  // Update table rows
  currentInvestments.forEach(inv => {
    updateNodeText(`inv-${inv.id}-current`, fmtMoney.format(inv.currentValue));
    updateNodeText(`inv-${inv.id}-profit`, formatColor(inv.profitLoss));
    updateNodeText(`inv-${inv.id}-pct`, formatColor(inv.returnPercentage, true));
  });
}

// Render UI
function renderDashboard() {
  document.getElementById('inv-loading').style.display = 'none';

  if (currentInvestments.length === 0) {
    document.getElementById('inv-empty').style.display = 'block';
    return;
  }
  
  updateMarketStatus();

  // Populate Summary
  document.getElementById('inv-total-invested').innerText = fmtMoney.format(currentSummary.totalInvested);
  document.getElementById('inv-current-value').innerText = fmtMoney.format(currentSummary.currentValue);
  document.getElementById('inv-total-return').innerHTML = formatColor(currentSummary.totalProfitLoss);
  document.getElementById('inv-return-pct').innerHTML = formatColor(currentSummary.returnPercentage, true);

  document.getElementById('perf-total-invested').innerText = fmtMoney.format(currentSummary.totalInvested);
  document.getElementById('perf-current-value').innerText = fmtMoney.format(currentSummary.currentValue);
  document.getElementById('perf-total-return').innerHTML = formatColor(currentSummary.totalProfitLoss);
  document.getElementById('perf-return-pct').innerHTML = formatColor(currentSummary.returnPercentage, true);
  document.getElementById('portfolio-performance-section').style.display = 'block';
  
  document.getElementById('inv-metrics').style.display = 'grid';

  // Asset Allocation
  const alloc = currentSummary.assetAllocation || {};
  if (Object.keys(alloc).length > 0) {
    document.getElementById('allocation-section').style.display = 'block';
    const chart = document.getElementById('allocation-chart');
    chart.innerHTML = '';
    
    const sortedAlloc = Object.entries(alloc).sort((a, b) => b[1] - a[1]);
    sortedAlloc.forEach(([type, pct]) => {
      const safeType = type.replace(/\s+/g, '-');
      const row = document.createElement('div');
      row.className = 'cat-row';
      row.innerHTML = `
        <div class="cat-name">${type}</div>
        <div class="cat-track"><div class="cat-fill" id="alloc-bar-${safeType}" style="width: 0%; transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1)"></div></div>
        <div class="cat-share" id="alloc-pct-${safeType}">${pct.toFixed(1)}%</div>
      `;
      chart.appendChild(row);
      setTimeout(() => {
        document.getElementById(`alloc-bar-${safeType}`).style.width = pct + '%';
      }, 50);
    });
  }

  // Table
  document.getElementById('inv-table-wrapper').style.display = 'block';
  const list = document.getElementById('inv-list');
  list.innerHTML = '';

  currentInvestments.forEach((inv) => {
    const row = document.createElement('div');
    row.className = 'inv-row';
    row.id = `inv-row-${inv.id}`;
    row.onclick = () => window.openEditModal(inv.id);
    
    let metaText = `${inv.typeName}`;
    let qtyText = '—';
    let priceText = '—';
    if ((inv.type === 'STOCKS' || inv.type === 'ETFS' || inv.type === 'MUTUAL_FUNDS') && inv.symbol) {
      metaText = `${inv.symbol} · ${inv.exchange} · ${fmtDate.format(new Date(inv.purchaseDate))}`;
      qtyText = inv.quantity;
      priceText = fmtMoney.format(inv.purchasePrice);
    } else {
      metaText = `${inv.typeName} · ${fmtDate.format(new Date(inv.purchaseDate))}`;
    }
    
    row.innerHTML = `
      <div>
        <div class="tx-desc" style="font-weight: 600; font-size: 14px; color: var(--text-1);">${inv.name}</div>
        <div class="tx-date" style="margin-top: 4px; text-transform: none; font-size: 11px;">${metaText}</div>
      </div>
      <div><span class="tx-cat">${inv.typeName}</span></div>
      <div class="tx-amount right" style="font-size: 14px; color: var(--text-2); font-family: var(--font-ui); font-weight: 500;">${qtyText}</div>
      <div class="tx-amount right" style="font-size: 14px; color: var(--text-2);">${priceText}</div>
      <div class="tx-amount right" id="inv-${inv.id}-current">${fmtMoney.format(inv.currentValue)}</div>
      <div class="tx-amount right" id="inv-${inv.id}-profit">${formatColor(inv.profitLoss)}</div>
      <div class="tx-amount right" style="font-size: 13px;" id="inv-${inv.id}-pct">${formatColor(inv.returnPercentage, true)}</div>
    `;
    list.appendChild(row);
  });
}

// Modals
window.toggleMarketFields = () => {
  const type = document.getElementById('inv-type').value;
  const isMarket = (type === 'STOCKS' || type === 'ETFS' || type === 'MUTUAL_FUNDS');
  
  document.getElementById('modal-symbol-group').style.display = isMarket ? 'block' : 'none';
  document.getElementById('modal-exchange-group').style.display = (type === 'STOCKS' || type === 'ETFS') ? 'block' : 'none';
  
  const currentHelp = document.getElementById('inv-current-help');
  const currentInput = document.getElementById('inv-current');
  const currentGroup = document.getElementById('modal-current-group');
  
  if (isMarket) {
    if (currentGroup) currentGroup.style.display = 'none';
    currentHelp.innerText = "Price fetched automatically from market.";
    currentInput.required = false;
  } else {
    if (currentGroup) currentGroup.style.display = 'block';
    currentHelp.innerText = "Manually update current price here.";
    currentInput.required = true;
  }
};

window.openAddModal = () => {
  document.getElementById('inv-form').reset();
  document.getElementById('inv-id').value = '';
  document.getElementById('inv-date').value = new Date().toISOString().split('T')[0];
  document.getElementById('modal-title').innerText = 'Add an Investment';
  document.getElementById('btn-inv-delete').style.display = 'none';
  document.getElementById('modal-feedback').classList.remove('visible');
  document.getElementById('inv-modal').classList.add('open');
  window.toggleMarketFields();
};

window.openEditModal = (id) => {
  const inv = currentInvestments.find(i => i.id === id);
  if (!inv) return;

  document.getElementById('inv-form').reset();
  document.getElementById('inv-id').value = inv.id;
  document.getElementById('inv-name').value = inv.name;
  document.getElementById('inv-symbol').value = inv.symbol || '';
  document.getElementById('inv-exchange').value = inv.exchange || 'NSE';
  document.getElementById('inv-type').value = inv.type;
  document.getElementById('inv-qty').value = inv.quantity;
  document.getElementById('inv-purchase').value = inv.purchasePrice;
  document.getElementById('inv-current').value = inv.currentPrice;
  document.getElementById('inv-date').value = inv.purchaseDate;
  document.getElementById('inv-notes').value = inv.notes || '';
  
  document.getElementById('modal-title').innerText = 'Edit Investment';
  document.getElementById('btn-inv-delete').style.display = 'block';
  document.getElementById('modal-feedback').classList.remove('visible');
  
  window.toggleMarketFields();
  document.getElementById('inv-modal').classList.add('open');
};

window.closeInvModal = () => {
  document.getElementById('inv-modal').classList.remove('open');
};

// Form Save
window.saveInvestment = async (e) => {
  e.preventDefault();
  
  const id = document.getElementById('inv-id').value;
  const name = document.getElementById('inv-name').value.trim();
  const symbol = document.getElementById('inv-symbol').value.trim();
  const exchange = document.getElementById('inv-exchange').value;
  const type = document.getElementById('inv-type').value;
  const quantity = parseFloat(document.getElementById('inv-qty').value);
  const purchasePrice = parseFloat(document.getElementById('inv-purchase').value);
  let currentPrice = parseFloat(document.getElementById('inv-current').value);
  const purchaseDate = document.getElementById('inv-date').value;
  const notes = document.getElementById('inv-notes').value.trim();
  
  if (isNaN(currentPrice)) currentPrice = purchasePrice; // safe fallback for market assets initially

  // Validate
  if (!name || !type || quantity <= 0 || purchasePrice <= 0 || currentPrice < 0 || !purchaseDate) {
    showError("Invalid input", "Please ensure quantity and prices are valid positive numbers.");
    return;
  }

  const btn = document.getElementById('btn-inv-save');
  btn.disabled = true;
  btn.innerText = 'SAVING...';

  const data = { name, symbol, exchange, type, quantity, purchasePrice, currentPrice, purchaseDate, notes };
  const method = id ? 'PUT' : 'POST';
  const url = id ? `${API_URL}/${id}` : API_URL;

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
      signal: AbortSignal.timeout(5000)
    });

    if (!res.ok) throw new Error('API Error');

    showToast('SUCCESS', id ? 'Investment updated' : 'Investment added');
    window.closeInvModal();
    fetchInvestments(); // Refresh full structure
  } catch (err) {
    showError("Failed to save", "Could not connect to the server.");
  } finally {
    btn.disabled = false;
    btn.innerText = 'Save Investment';
  }
};

window.deleteInvestment = async () => {
  const id = document.getElementById('inv-id').value;
  if (!id) return;

  const btn = document.getElementById('btn-inv-delete');
  btn.disabled = true;
  btn.innerText = 'DELETING...';

  try {
    const res = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE',
      signal: AbortSignal.timeout(5000)
    });

    if (!res.ok) throw new Error('API Error');

    showToast('DELETED', 'Investment record removed');
    window.closeInvModal();
    fetchInvestments(); // Refresh
  } catch (err) {
    showError("Failed to delete", "Could not connect to the server.");
  } finally {
    btn.disabled = false;
    btn.innerText = 'Delete';
  }
};

function showError(title, msg) {
  const banner = document.getElementById('modal-feedback');
  banner.className = 'feedback-banner error visible';
  document.getElementById('modal-feedback-type').innerText = title;
  document.getElementById('modal-feedback-msg').innerText = msg;
}
