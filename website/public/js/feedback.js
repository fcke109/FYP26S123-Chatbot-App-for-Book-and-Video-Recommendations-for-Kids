import {
  auth,
  db,
  onAuthStateChanged,
  collection,
  query,
  where,
  onSnapshot,
  deleteDoc,
  doc
} from './firebase-config.js';

export function initFeedbackDashboard() {
  const dashboardEl = document.getElementById('feedback-dashboard');
  const loginPrompt = document.getElementById('login-prompt');
  const loadingEl = document.getElementById('feedback-loading');

  onAuthStateChanged(auth, (user) => {
    if (!user) {
      if (dashboardEl) dashboardEl.style.display = 'none';
      if (loginPrompt) loginPrompt.style.display = 'block';
      if (loadingEl) loadingEl.style.display = 'none';
      return;
    }

    if (loginPrompt) loginPrompt.style.display = 'none';
    if (dashboardEl) dashboardEl.style.display = 'block';

    const feedbackQuery = query(
      collection(db, 'feedback'),
      where('status', '==', 'published')
    );

    onSnapshot(
      feedbackQuery,
      (snapshot) => {
        if (loadingEl) loadingEl.style.display = 'none';

        const items = snapshot.docs
          .map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }))
          .sort((a, b) => getFeedbackDate(b).getTime() - getFeedbackDate(a).getTime());

        renderDashboard(items, user);
      },
      (error) => {
        console.error('Feedback listener error:', error);
        if (loadingEl) loadingEl.style.display = 'none';
      }
    );
  });
}

function isPositiveFeedback(f) {
  if (typeof f.isPositive === 'boolean') return f.isPositive;
  if (typeof f.positive === 'boolean') return f.positive;

  const rating = Number(f.rating);
  if (!Number.isNaN(rating)) return rating >= 4;

  return false;
}

function getFeedbackTitle(f) {
  return f.title || f.contentTitle || f.recommendationTitle || 'Unknown';
}

function getFeedbackType(f) {
  return String(f.contentType || f.type || f.recommendationType || 'BOOK').toUpperCase();
}

function getFeedbackDate(f) {
  const raw = f.createdAt || f.timestamp || f.updatedAt;

  if (raw?.toDate) return raw.toDate();
  if (raw instanceof Date) return raw;

  const parsed = new Date(raw);
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
}

function getRating(f) {
  const rating = Number(f.rating);
  if (!Number.isNaN(rating) && rating >= 1 && rating <= 5) return rating;
  return isPositiveFeedback(f) ? 5 : 3;
}

function canDeleteFeedback(item, user) {
  if (!user) return false;
  const isAdmin = user.email === 'admin@littledino.com';
  const isOwner = item.userId && item.userId === user.uid;
  return isAdmin || isOwner;
}

async function deleteFeedback(feedbackId) {
  const confirmed = window.confirm('Delete this feedback?');
  if (!confirmed) return;

  try {
    await deleteDoc(doc(db, 'feedback', feedbackId));
  } catch (error) {
    console.error('Delete failed:', error);
    alert('Could not delete feedback.');
  }
}

function renderDashboard(items, user) {
  renderStats(items);
  renderTopItems(items);
  renderRecentFeedback(items, user);
}

function renderStats(items) {
  const total = items.length;
  const positive = items.filter((f) => isPositiveFeedback(f)).length;
  const negative = total - positive;
  const pct = total > 0 ? Math.round((positive / total) * 100) : 0;
  const avgRating = total > 0
    ? (items.reduce((sum, item) => sum + getRating(item), 0) / total).toFixed(1)
    : '0.0';

  const totalEl = document.getElementById('stat-total');
  const positiveEl = document.getElementById('stat-positive');
  const negativeEl = document.getElementById('stat-negative');
  const pctEl = document.getElementById('stat-pct');
  const ratingEl = document.getElementById('stat-rating');

  if (totalEl) totalEl.textContent = total;
  if (positiveEl) positiveEl.textContent = positive;
  if (negativeEl) negativeEl.textContent = negative;
  if (pctEl) pctEl.textContent = `${pct}%`;
  if (ratingEl) ratingEl.textContent = avgRating;
}

function renderTopItems(items) {
  const byTitle = {};

  items.forEach((f) => {
    const key = getFeedbackTitle(f);
    const type = getFeedbackType(f);

    if (!byTitle[key]) {
      byTitle[key] = {
        title: key,
        type,
        up: 0,
        down: 0,
        totalRating: 0,
        count: 0
      };
    }

    if (isPositiveFeedback(f)) byTitle[key].up += 1;
    else byTitle[key].down += 1;

    byTitle[key].totalRating += getRating(f);
    byTitle[key].count += 1;
  });

  const sorted = Object.values(byTitle).sort((a, b) => {
    const aScore = (a.up - a.down) + (a.totalRating / a.count);
    const bScore = (b.up - b.down) + (b.totalRating / b.count);
    return bScore - aScore;
  });

  const container = document.getElementById('top-items');
  if (!container) return;

  container.innerHTML = '';

  const topItems = sorted.slice(0, 8);

  if (topItems.length === 0) {
    container.innerHTML = '<p style="color:var(--muted);text-align:center;">No feedback yet</p>';
    return;
  }

  topItems.forEach((item) => {
    const net = item.up - item.down;
    const avgRating = (item.totalRating / item.count).toFixed(1);

    const card = document.createElement('div');
    card.className = 'top-item-card';
    card.innerHTML = `
      <div class="top-item-header">
        <span class="badge ${item.type === 'VIDEO' ? 'badge-video' : 'badge-book'}">${esc(item.type)}</span>
        <span class="top-item-score ${net >= 0 ? 'score-positive' : 'score-negative'}">
          ${net >= 0 ? '+' : ''}${net}
        </span>
      </div>
      <h4>${esc(item.title)}</h4>
      <div class="top-item-votes">
        <span class="vote-up">👍 ${item.up}</span>
        <span class="vote-down">👎 ${item.down}</span>
      </div>
      <div class="top-item-rating">⭐ ${avgRating}/5</div>
    `;
    container.appendChild(card);
  });
}

function renderRecentFeedback(items, user) {
  const tbody = document.getElementById('feedback-tbody');
  if (!tbody) return;

  tbody.innerHTML = '';

  const recent = items.slice(0, 30);

  if (recent.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--muted);">No feedback yet</td></tr>';
    return;
  }

  recent.forEach((f) => {
    const row = document.createElement('tr');
    const ts = getFeedbackDate(f);
    const timeStr = ts.toLocaleDateString() + ' ' + ts.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const type = getFeedbackType(f);
    const pos = isPositiveFeedback(f);
    const rating = getRating(f);
    const userName = f.userName || f.userEmail?.split('@')[0] || 'Anonymous User';

    row.innerHTML = `
      <td>${esc(getFeedbackTitle(f))}</td>
      <td>${esc(userName)}</td>
      <td><span class="badge ${type === 'VIDEO' ? 'badge-video' : 'badge-book'}">${esc(type)}</span></td>
      <td>${'⭐'.repeat(rating)}</td>
      <td class="${pos ? 'feedback-positive' : 'feedback-negative'}">${pos ? '👍' : '👎'}</td>
      <td>${timeStr}</td>
      <td>${canDeleteFeedback(f, user) ? `<button class="feedback-delete-btn" data-id="${f.id}">Delete</button>` : ''}</td>
    `;
    tbody.appendChild(row);
  });

  tbody.querySelectorAll('.feedback-delete-btn').forEach((btn) => {
    btn.addEventListener('click', () => deleteFeedback(btn.dataset.id));
  });
}

function esc(s) {
  const d = document.createElement('div');
  d.textContent = s ?? '';
  return d.innerHTML;
}
