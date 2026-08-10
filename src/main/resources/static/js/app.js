/**
 * SPENDEX — Shared Utilities v2.0
 * Animation system · API helpers · Toast · Count-up · Page transitions
 */

/* ─────────────────────────────────────────────────────────
   FORMATTING
───────────────────────────────────────────────────────── */

function formatRupees(amount) {
  if (amount == null || isNaN(amount)) return '₹0';
  return '₹' + Math.round(parseFloat(amount))
    .toLocaleString('en-IN', { maximumFractionDigits: 0 });
}

function formatRupeesExact(amount) {
  if (amount == null || isNaN(amount)) return '₹0';
  return '₹' + parseFloat(amount)
    .toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

function formatDateShort(dateStr) {
  if (!dateStr) return '';
  try {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }).toUpperCase();
  } catch { return dateStr; }
}

function formatDateLong(dateStr) {
  if (!dateStr) return '';
  try {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch { return dateStr; }
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function escHtml(str) {
  const d = document.createElement('div');
  d.textContent = String(str ?? '');
  return d.innerHTML;
}

/* ─────────────────────────────────────────────────────────
   API FETCH WRAPPER
───────────────────────────────────────────────────────── */

async function apiFetch(url, options = {}) {
  const resp = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });

  let data;
  try { data = await resp.json(); } catch { data = {}; }

  if (!resp.ok) {
    throw new Error(data.error || `HTTP ${resp.status}`);
  }
  return data;
}

/* ─────────────────────────────────────────────────────────
   COUNT-UP NUMBER ANIMATION
───────────────────────────────────────────────────────── */

function animateNumber(element, targetValue, duration = 950, formatter) {
  if (typeof targetValue !== 'number' || isNaN(targetValue)) {
    if (element) element.textContent = formatter ? formatter(targetValue) : targetValue;
    return;
  }

  const fmt = formatter || formatRupees;
  const startTime = performance.now();

  // Ease-out cubic
  const ease = t => 1 - Math.pow(1 - t, 3);

  function step(now) {
    const elapsed  = now - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased    = ease(progress);
    const current  = targetValue * eased;

    if (element) element.textContent = fmt(current);
    if (progress < 1) requestAnimationFrame(step);
    else if (element) element.textContent = fmt(targetValue);
  }

  requestAnimationFrame(step);
}

function animateCount(element, target, duration = 720) {
  animateNumber(element, target, duration, v => Math.round(v));
}

/* ─────────────────────────────────────────────────────────
   TOAST NOTIFICATION SYSTEM
───────────────────────────────────────────────────────── */

function ensureToastContainer() {
  let c = document.getElementById('toast-container');
  if (!c) {
    c = document.createElement('div');
    c.id = 'toast-container';
    document.body.appendChild(c);
  }
  return c;
}

function showToast(title, message, type = 'success') {
  const container = ensureToastContainer();
  const toast = document.createElement('div');
  toast.className = 'toast' + (type === 'error' ? ' toast-error' : '');
  toast.innerHTML =
    `<div class="toast-title">${escHtml(title)}</div>` +
    (message ? `<div class="toast-msg">${escHtml(message)}</div>` : '');

  // Click to dismiss
  toast.addEventListener('click', () => dismiss(toast));

  container.appendChild(toast);

  // Trigger enter (double RAF for paint flush)
  requestAnimationFrame(() => requestAnimationFrame(() => {
    toast.classList.add('visible');
  }));

  // Auto-dismiss
  const delay = type === 'error' ? 5500 : 3800;
  const timer = setTimeout(() => dismiss(toast), delay);
  toast._timer = timer;

  function dismiss(t) {
    clearTimeout(t._timer);
    t.classList.remove('visible');
    setTimeout(() => t.remove(), 360);
  }
}

/* ─────────────────────────────────────────────────────────
   TRANSACTION ROW BUILDER
───────────────────────────────────────────────────────── */

/**
 * @param {Object}  expense
 * @param {boolean} showDelete   — include delete button
 * @param {number}  delay        — animation-delay ms for stagger
 * @param {boolean} compact      — 4-column (no delete column space)
 */
function buildTxRow(expense, showDelete = false, delay = 0, compact = false) {
  const date  = formatDateShort(expense.date || '');
  const desc  = expense.description || '—';
  const id    = escHtml(expense.id || '');
  const cls   = 'tx-row' + (compact ? ' no-del' : '');
  const delBtn = showDelete
    ? `<button class="tx-del" onclick="deleteExpense('${id}')"
              title="Delete this expense" aria-label="Delete expense">×</button>`
    : (compact ? '' : '<span></span>');

  return `<div class="${cls}" data-id="${id}" style="animation-delay:${delay}ms">
    <span class="tx-date">${escHtml(date)}</span>
    <span class="tx-cat">${escHtml(expense.category || '')}</span>
    <span class="tx-desc">${escHtml(desc)}</span>
    <span class="tx-amount">${formatRupees(expense.amount)}</span>
    ${delBtn}
  </div>`;
}

/* ─────────────────────────────────────────────────────────
   EMPTY STATE BUILDER
───────────────────────────────────────────────────────── */

function buildEmptyState(eyebrow, title, text, linkHref, linkText) {
  return `<div class="empty-state">
    <div class="empty-state-eyebrow">${escHtml(eyebrow || '')}</div>
    <div class="empty-state-title">${escHtml(title)}</div>
    <p class="empty-state-text">${escHtml(text)}</p>
    ${linkHref
      ? `<a href="${escHtml(linkHref)}" class="btn-primary">${escHtml(linkText)}</a>`
      : ''}
  </div>`;
}

/* ─────────────────────────────────────────────────────────
   BAR CHART ANIMATION
   Call after bars are in the DOM. Width goes 0 → data-pct.
───────────────────────────────────────────────────────── */

function triggerBars(initialDelay = 80) {
  setTimeout(() => {
    document.querySelectorAll('.cat-fill, .rpt-fill').forEach((bar, i) => {
      setTimeout(() => {
        bar.style.width = (bar.dataset.pct || 0) + '%';
      }, i * 95);
    });
  }, initialDelay);
}

/* ─────────────────────────────────────────────────────────
   DELETE EXPENSE  (global, called from inline onclick)
───────────────────────────────────────────────────────── */

window.deleteExpense = async function(id) {
  const row = document.querySelector(`[data-id="${CSS.escape(id)}"]`);
  if (!row) return;

  // Optimistic: start delete animation immediately
  row.classList.add('deleting');

  try {
    await apiFetch(`/api/expenses/${encodeURIComponent(id)}`, { method: 'DELETE' });

    // Wait for CSS animation to complete, then remove
    setTimeout(() => row.remove(), 370);

    showToast('Expense Removed', 'The entry has been deleted.');

    // Decrement count label if present
    const countEl = document.getElementById('result-count');
    if (countEl) {
      const m = countEl.textContent.match(/(\d+)/);
      if (m) {
        const n = parseInt(m[1]) - 1;
        countEl.textContent = n === 0
          ? 'No transactions'
          : `${n} transaction${n !== 1 ? 's' : ''}`;
      }
    }

  } catch (err) {
    // Undo animation on failure
    row.classList.remove('deleting');
    showToast('Could not delete', err.message || 'Please try again.', 'error');
  }
};

/* ─────────────────────────────────────────────────────────
   PAGE TRANSITIONS
   Intercept same-origin links → fade-out → navigate
───────────────────────────────────────────────────────── */

function initPageTransitions() {
  document.querySelectorAll('a[href]').forEach(link => {
    const href = link.getAttribute('href') || '';
    if (!href
      || href.startsWith('#')
      || href.startsWith('http')
      || href.startsWith('mailto')
      || link.hasAttribute('data-no-transition')
      || link.target === '_blank') return;

    link.addEventListener('click', e => {
      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      e.preventDefault();
      const target = link.href;
      const main   = document.querySelector('.main-content');
      if (main) {
        main.classList.add('page-exit');
        setTimeout(() => { window.location.href = target; }, 210);
      } else {
        window.location.href = target;
      }
    });
  });
}

/* ─────────────────────────────────────────────────────────
   INIT
───────────────────────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
  initPageTransitions();
  ensureToastContainer();
});
