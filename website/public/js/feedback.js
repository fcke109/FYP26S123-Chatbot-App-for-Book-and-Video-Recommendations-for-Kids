import {
  auth, db, onAuthStateChanged,
  collection, query, orderBy, limit, onSnapshot
} from './firebase-config.js';

export function initFeedbackDashboard() {
  const dashboardEl = document.getElementById('feedback-dashboard');
  const loginPrompt = document.getElementById('login-prompt');
  const loadingEl = document.getElementById('feedback-loading');

  onAuthStateChanged(auth, (user) => {
    if (!user) {
      if (dashboardEl) dashboardEl.style.display = 'none';
      if (loginPrompt) loginPrompt.style.display = 'block';
      return;
    }

    if (loginPrompt) loginPrompt.style.display = 'none';
    if (dashboardEl) dashboardEl.style.display = 'block';

    const feedbackQuery = query(
      collection(db, 'feedback'),
      orderBy('timestamp', 'desc'),
      limit(200)
    );

    onSnapshot(feedbackQuery, (snapshot) => {
      if (loadingEl) loadingEl.style.display = 'none';
      const items = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      renderDashboard(items);
    }, (error) => {
      console.error('Feedback listener error:', error);
      if (loadingEl) loadingEl.style.display = 'none';
    });
  });
}

function renderDashboard(items) {
  renderStats(items);
  renderTopItems(items);
  renderRecentFeedback(items);
}

function renderStats(items) {
  const total = items.length;
  const positive = items.filter(f => f.isPositive === true).length;
  const negative = total - positive;
  const pct = total > 0 ? Math.round((positive / total) * 100) : 0;

  document.getElementById('stat-total').textContent = total;
  document.getElementById('stat-positive').textContent = positive;
  document.getElementById('stat-negative').textContent = negative;
  document.getElementById('stat-pct').textContent = pct + '%';
}

function renderTopItems(items) {
  const byTitle = {};
  items.forEach(f => {
    const key = f.recommendationTitle || 'Unknown';
    if (!byTitle[key]) byTitle[key] = { title: key, type: f.recommendationType || 'BOOK', up: 0, down: 0 };
    if (f.isPositive) byTitle[key].up++;
    else byTitle[key].down++;
  });

  const sorted = Object.values(byTitle).sort((a, b) => (b.up - b.down) - (a.up - a.down));
  const container = document.getElementById('top-items');
  container.innerHTML = '';

  const topItems = sorted.slice(0, 8);
  if (topItems.length === 0) {
    container.innerHTML = '<p style="color:var(--muted);text-align:center;">No feedback yet</p>';
    return;
  }

  topItems.forEach(item => {
    const net = item.up - item.down;
    const card = document.createElement('div');
    card.className = 'top-item-card';
    card.innerHTML = `
      <div class="top-item-header">
        <span class="badge ${item.type === 'VIDEO' ? 'badge-video' : 'badge-book'}">${esc(item.type)}</span>
        <span class="top-item-score ${net >= 0 ? 'score-positive' : 'score-negative'}">${net >= 0 ? '+' : ''}${net}</span>
      </div>
      <h4>${esc(item.title)}</h4>
      <div class="top-item-votes">
        <span class="vote-up">\u{1F44D} ${item.up}</span>
        <span class="vote-down">\u{1F44E} ${item.down}</span>
      </div>`;
    container.appendChild(card);
  });
}

function renderRecentFeedback(items) {
  const tbody = document.getElementById('feedback-tbody');
  tbody.innerHTML = '';

  const recent = items.slice(0, 30);
  if (recent.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--muted);">No feedback yet</td></tr>';
    return;
  }

  recent.forEach(f => {
    const row = document.createElement('tr');
    const ts = f.timestamp?.toDate ? f.timestamp.toDate() : new Date();
    const timeStr = ts.toLocaleDateString() + ' ' + ts.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const type = f.recommendationType || 'BOOK';

    row.innerHTML = `
      <td>${esc(f.recommendationTitle || 'Unknown')}</td>
      <td><span class="badge ${type === 'VIDEO' ? 'badge-video' : 'badge-book'}">${esc(type)}</span></td>
      <td class="${f.isPositive ? 'feedback-positive' : 'feedback-negative'}">${f.isPositive ? '\u{1F44D}' : '\u{1F44E}'}</td>
      <td>${timeStr}</td>`;
    tbody.appendChild(row);
  });
}

function esc(s) {
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}
