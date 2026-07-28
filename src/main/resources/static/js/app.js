/* ==========================================
   AI Automation Assistant - JavaScript App Logic
   ========================================== */

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initSidebar();
  highlightActiveNav();
  initCalendar();
  initTaskManager();
  initChatInterface();
  initToastContainer();
});

window.toggleReadMore = function(btn) {
  const wrapper = btn.previousElementSibling;
  if (!wrapper) return;
  const isCollapsed = wrapper.classList.contains('collapsed');
  if (isCollapsed) {
    wrapper.classList.remove('collapsed');
    btn.innerHTML = '<i class="bi bi-chevron-up me-1"></i>Read Less';
  } else {
    wrapper.classList.add('collapsed');
    btn.innerHTML = '<i class="bi bi-chevron-down me-1"></i>Read More';
  }
};

/* ==========================================
   1. Theme Management (Dark Mode)
   ========================================== */
function initTheme() {
  const savedTheme = localStorage.getItem('saas_theme') || 'light';
  applyTheme(savedTheme);

  const themeToggles = document.querySelectorAll('.theme-toggle');
  themeToggles.forEach(toggle => {
    toggle.addEventListener('click', (e) => {
      e.preventDefault();
      const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
      const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(nextTheme);
      showToast(`Switched to ${nextTheme === 'dark' ? 'Dark' : 'Light'} mode`, 'info');
    });
  });
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-bs-theme', theme);
  localStorage.setItem('saas_theme', theme);

  const icons = document.querySelectorAll('.theme-toggle i');
  icons.forEach(icon => {
    if (theme === 'dark') {
      icon.className = 'bi bi-sun-fill';
    } else {
      icon.className = 'bi bi-moon-stars-fill';
    }
  });
}

/* ==========================================
   2. Sidebar & Navigation Logic
   ========================================== */
function initSidebar() {
  const sidebar = document.querySelector('.app-sidebar');
  const toggleBtn = document.querySelector('.btn-sidebar-toggle');
  const overlay = document.querySelector('.sidebar-overlay');

  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener('click', () => {
      if (window.innerWidth < 992) {
        sidebar.classList.toggle('show');
        if (overlay) overlay.classList.toggle('show');
      } else {
        document.body.classList.toggle('sidebar-collapsed');
        localStorage.setItem('sidebar_collapsed', document.body.classList.contains('sidebar-collapsed'));
      }
    });
  }

  if (overlay) {
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('show');
      overlay.classList.remove('show');
    });
  }

  // Restore collapsed desktop state
  if (localStorage.getItem('sidebar_collapsed') === 'true' && window.innerWidth >= 992) {
    document.body.classList.add('sidebar-collapsed');
  }
}

function highlightActiveNav() {
  const currentPath = window.location.pathname;
  const pageName = currentPath.split('/').pop() || 'index.html';

  const navLinks = document.querySelectorAll('.sidebar-nav-link, .navbar-nav .nav-link');
  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (href && (href === pageName || (pageName === '' && href === 'index.html'))) {
      const parentLi = link.closest('.sidebar-nav-item');
      if (parentLi) parentLi.classList.add('active');
      link.classList.add('active');
    } else {
      const parentLi = link.closest('.sidebar-nav-item');
      if (parentLi) parentLi.classList.remove('active');
      link.classList.remove('active');
    }
  });
}

/* ==========================================
   3. Calendar Controller
   ========================================== */
let currentCalDate = new Date();

function initCalendar() {
  const miniCalendarEl = document.getElementById('dashboard-mini-calendar');
  const fullCalendarEl = document.getElementById('full-calendar-grid');

  const monthSelect = document.getElementById('calendar-month-select');
  const yearSelect = document.getElementById('calendar-year-select');

  const prevBtn = document.getElementById('cal-prev-btn');
  const todayBtn = document.getElementById('cal-today-btn');
  const nextBtn = document.getElementById('cal-next-btn');

  const monthNames = ["January", "February", "March", "April", "May", "June", 
                      "July", "August", "September", "October", "November", "December"];

  if (monthSelect && yearSelect) {
    // Populate Months
    monthSelect.innerHTML = monthNames.map((m, idx) => `<option value="${idx}">${m}</option>`).join('');
    
    // Populate Years (current year - 5 to current year + 5)
    const currentYear = new Date().getFullYear();
    let yearOptions = '';
    for (let y = currentYear - 5; y <= currentYear + 5; y++) {
      yearOptions += `<option value="${y}">${y}</option>`;
    }
    yearSelect.innerHTML = yearOptions;

    // Set initial values
    monthSelect.value = currentCalDate.getMonth();
    yearSelect.value = currentCalDate.getFullYear();

    monthSelect.addEventListener('change', () => {
      currentCalDate.setMonth(parseInt(monthSelect.value));
      if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
    });

    yearSelect.addEventListener('change', () => {
      currentCalDate.setFullYear(parseInt(yearSelect.value));
      if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
    });
  }

  if (prevBtn) {
    prevBtn.addEventListener('click', () => {
      currentCalDate.setMonth(currentCalDate.getMonth() - 1);
      syncCalendarSelects();
      if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
    });
  }

  if (todayBtn) {
    todayBtn.addEventListener('click', () => {
      currentCalDate = new Date();
      syncCalendarSelects();
      if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
    });
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', () => {
      currentCalDate.setMonth(currentCalDate.getMonth() + 1);
      syncCalendarSelects();
      if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
    });
  }

  function syncCalendarSelects() {
    if (monthSelect) monthSelect.value = currentCalDate.getMonth();
    if (yearSelect) yearSelect.value = currentCalDate.getFullYear();
  }

  if (miniCalendarEl) renderCalendarGrid(miniCalendarEl, false);
  if (fullCalendarEl) renderCalendarGrid(fullCalendarEl, true);
}

async function renderCalendarGrid(container, isFull) {
  const year = currentCalDate.getFullYear();
  const month = currentCalDate.getMonth();
  const today = new Date();
  
  const firstDayIndex = new Date(year, month, 1).getDay();
  const lastDate = new Date(year, month + 1, 0).getDate();
  const prevLastDate = new Date(year, month, 0).getDate();
  
  const monthNames = ["January", "February", "March", "April", "May", "June", 
                      "July", "August", "September", "October", "November", "December"];
  
  const titleEl = document.getElementById('calendar-month-year');
  if (titleEl) {
    titleEl.textContent = `${monthNames[month]} ${year}`;
  }

  // Fetch month activity summary
  let monthSummary = {};
  try {
    const res = await fetch(`/api/calendar/month-summary?year=${year}&month=${month + 1}`);
    if (res.ok) {
      monthSummary = await res.json();
    }
  } catch (e) {}

  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  let gridHTML = dayNames.map(d => `<div class="calendar-day-header">${d}</div>`).join('');
  
  // Previous month days padding
  for (let x = firstDayIndex; x > 0; x--) {
    gridHTML += `<div class="calendar-day other-month">${prevLastDate - x + 1}</div>`;
  }
  
  // Current month days
  for (let i = 1; i <= lastDate; i++) {
    const isToday = i === today.getDate() && month === today.getMonth() && year === today.getFullYear();
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
    const dayInfo = monthSummary[dateStr] || {};

    let eventsHTML = '';
    const items = dayInfo.eventItems || [];

    if (isFull && items.length > 0) {
      if (items.length <= 3 && window.innerWidth >= 768) {
        // <= 3 events: Render individual event text pills in category colors
        eventsHTML += '<div class="w-100 mt-1 d-flex flex-column gap-1 overflow-hidden" style="max-height: 75px;">';
        items.forEach(evt => {
          const cat = (evt.category || '1').toString().trim().toLowerCase();
          let bgStyle = 'background-color: #3b82f6;'; // Cat 1 Blue
          if (cat === '2' || cat.includes('email')) bgStyle = 'background-color: #8b5cf6;'; // Cat 2 Purple
          else if (cat === '3' || cat.includes('schedule') || cat.includes('reminder')) bgStyle = 'background-color: #10b981;'; // Cat 3 Green
          else if (cat === '4' || cat.includes('database')) bgStyle = 'background-color: #f97316;'; // Cat 4 Orange

          eventsHTML += `
            <div class="calendar-event-pill text-white rounded px-1.5 py-0.5 text-truncate" style="${bgStyle} font-size: 0.7rem; font-weight: 600; line-height: 1.2; box-shadow: 0 1px 2px rgba(0,0,0,0.15);" title="${escapeHtml(evt.title)} (${evt.timeStr})">
              <span>${escapeHtml(evt.timeStr)}</span> ${escapeHtml(evt.title)}
            </div>
          `;
        });
        eventsHTML += '</div>';
      } else {
        // > 3 events: Render Category Count Badges in category colors
        let catCounts = { cat1: 0, cat2: 0, cat3: 0, cat4: 0, failed: 0 };
        items.forEach(evt => {
          const cat = (evt.category || '1').toString().trim().toLowerCase();
          if (cat === '2' || cat.includes('email')) catCounts.cat2++;
          else if (cat === '3' || cat.includes('schedule') || cat.includes('reminder')) catCounts.cat3++;
          else if (cat === '4' || cat.includes('database')) catCounts.cat4++;
          else catCounts.cat1++;
        });
        if (dayInfo.hasFailed) catCounts.failed++;

        eventsHTML += '<div class="w-100 mt-1 d-flex flex-wrap justify-content-center gap-1 overflow-hidden" style="max-height: 65px;">';
        if (catCounts.cat1 > 0) eventsHTML += `<span class="badge text-white rounded-pill px-2 py-1" style="background-color: #3b82f6; font-size: 0.68rem; font-weight: 600;" title="${catCounts.cat1} General Automations">${catCounts.cat1} ${window.innerWidth < 576 ? '' : 'General'}</span>`;
        if (catCounts.cat2 > 0) eventsHTML += `<span class="badge text-white rounded-pill px-2 py-1" style="background-color: #8b5cf6; font-size: 0.68rem; font-weight: 600;" title="${catCounts.cat2} Email Automations">${catCounts.cat2} ${window.innerWidth < 576 ? '' : 'Email'}</span>`;
        if (catCounts.cat3 > 0) eventsHTML += `<span class="badge text-white rounded-pill px-2 py-1" style="background-color: #10b981; font-size: 0.68rem; font-weight: 600;" title="${catCounts.cat3} Schedule & Reminders">${catCounts.cat3} ${window.innerWidth < 576 ? '' : 'Schedule'}</span>`;
        if (catCounts.cat4 > 0) eventsHTML += `<span class="badge text-white rounded-pill px-2 py-1" style="background-color: #f97316; font-size: 0.68rem; font-weight: 600;" title="${catCounts.cat4} Database Tasks">${catCounts.cat4} ${window.innerWidth < 576 ? '' : 'Database'}</span>`;
        if (catCounts.failed > 0) eventsHTML += `<span class="badge text-white rounded-pill px-2 py-1" style="background-color: #ef4444; font-size: 0.68rem; font-weight: 600;" title="${catCounts.failed} Failed Executions">${catCounts.failed} ${window.innerWidth < 576 ? '' : 'Failed'}</span>`;
        eventsHTML += '</div>';
      }
    }

    let dotsHTML = '<div class="d-flex justify-content-center align-items-center gap-1 position-absolute bottom-0 start-50 translate-middle-x mb-1">';
    if (dayInfo.hasTask) dotsHTML += `<span class="badge rounded-circle" style="width:6px; height:6px; padding:0; background-color: #3b82f6;" title="General Automation (Cat 1)"></span>`;
    if (dayInfo.hasEmail) dotsHTML += `<span class="badge rounded-circle" style="width:6px; height:6px; padding:0; background-color: #8b5cf6;" title="Email Management (Cat 2)"></span>`;
    if (dayInfo.hasReminder) dotsHTML += `<span class="badge rounded-circle" style="width:6px; height:6px; padding:0; background-color: #10b981;" title="Schedule & Reminders (Cat 3)"></span>`;
    if (dayInfo.hasDatabase) dotsHTML += `<span class="badge rounded-circle" style="width:6px; height:6px; padding:0; background-color: #f97316;" title="Database Management (Cat 4)"></span>`;
    if (dayInfo.hasFailed) dotsHTML += `<span class="badge rounded-circle" style="width:6px; height:6px; padding:0; background-color: #ef4444;" title="Failed Executions"></span>`;
    dotsHTML += '</div>';

    gridHTML += `<div class="calendar-day position-relative ${isToday ? 'today selected' : ''}" data-day="${i}" data-date="${dateStr}">
      <span class="fw-bold">${i}</span>
      ${isFull ? (eventsHTML || dotsHTML) : dotsHTML}
    </div>`;
  }
  
  container.innerHTML = gridHTML;

  // Helper for Category Badges
  function formatCategoryBadge(catRaw) {
    const c = (catRaw || '1').toString().trim().toLowerCase();
    if (c === '2' || c.includes('email')) {
      return `<span class="badge text-white me-1" style="background-color: #8b5cf6;"><i class="bi bi-envelope-fill me-1"></i>Email (Cat 2)</span>`;
    } else if (c === '3' || c.includes('schedule') || c.includes('reminder')) {
      return `<span class="badge text-white me-1" style="background-color: #10b981;"><i class="bi bi-calendar-event me-1"></i>Schedule (Cat 3)</span>`;
    } else if (c === '4' || c.includes('database')) {
      return `<span class="badge text-white me-1" style="background-color: #f97316;"><i class="bi bi-database-fill me-1"></i>Database (Cat 4)</span>`;
    }
    return `<span class="badge text-white me-1" style="background-color: #3b82f6;"><i class="bi bi-gear-fill me-1"></i>General (Cat 1)</span>`;
  }

  // Function to load and render activities for a given date
  async function loadDayDetails(dateStr, formattedDate) {
    const detailEl = document.getElementById('selected-day-details');
    if (!detailEl) return;

    detailEl.innerHTML = `
      <div class="p-3 text-center text-muted">
        <span class="spinner-border spinner-border-sm text-primary mb-2" role="status"></span>
        <p class="small mb-0">Loading day schedule...</p>
      </div>
    `;

    try {
      const res = await fetch(`/api/calendar/day?date=${dateStr}`);
      const data = await res.json();

      let html = `<div class="p-3">
        <div class="border-bottom pb-2 mb-3">
          <h6 class="fw-bold text-body mb-0"><i class="bi bi-calendar-event me-2 text-primary"></i>${formattedDate}</h6>
        </div>`;

      const tasks = data.tasks || [];
      const automations = data.automations || [];
      const logs = data.activities || [];

      if (tasks.length === 0 && automations.length === 0 && logs.length === 0) {
        html += `
          <div class="text-center py-3 text-muted">
            <i class="bi bi-calendar-x fs-3 d-block mb-2 opacity-50"></i>
            <p class="small mb-0">No scheduled tasks or activity for this day.</p>
          </div>
        `;
      } else {
        if (tasks.length > 0) {
          html += `<div class="mb-3"><h6 class="small fw-bold text-uppercase text-secondary mb-2"><i class="bi bi-check2-square me-1"></i> Scheduled Events & Tasks (${tasks.length})</h6><div class="d-flex flex-column gap-2">`;
          tasks.forEach(t => {
            let timeBadge = '';
            if (t.formattedDueDate) {
              const timeParts = t.formattedDueDate.split(' ');
              const timeOnly = timeParts.length >= 3 ? timeParts[2] : (timeParts.length >= 2 ? timeParts[1] : t.formattedDueDate);
              timeBadge = `<span class="badge bg-info-subtle text-info-emphasis me-1"><i class="bi bi-clock me-1"></i>${escapeHtml(timeOnly)}</span>`;
            } else if (t.dueDate) {
              try {
                const dt = new Date(t.dueDate);
                const timeOnly = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                timeBadge = `<span class="badge bg-info-subtle text-info-emphasis me-1"><i class="bi bi-clock me-1"></i>${escapeHtml(timeOnly)}</span>`;
              } catch (e) {}
            }

            const catBadge = formatCategoryBadge(t.category);

            html += `
              <div class="p-2 border rounded bg-body-tertiary small d-flex align-items-center justify-content-between">
                <div>
                  <div class="fw-bold ${t.completed ? 'text-decoration-line-through text-muted' : 'text-body'}">${escapeHtml(t.title)}</div>
                  <div class="text-muted" style="font-size: 0.75rem;">${timeBadge}${catBadge}${t.description ? '• ' + escapeHtml(t.description) : ''}</div>
                </div>
                <span class="badge ${t.completed ? 'bg-success-subtle text-success' : 'bg-primary-subtle text-primary'}">${t.completed ? 'Done' : 'Pending'}</span>
              </div>
            `;
          });
          html += `</div></div>`;
        }

        if (automations.length > 0) {
          html += `<div class="mb-3"><h6 class="small fw-bold text-uppercase text-secondary mb-2"><i class="bi bi-cpu me-1"></i> Automations (${automations.length})</h6><div class="d-flex flex-column gap-2">`;
          automations.forEach(a => {
            html += `
              <div class="p-2 border rounded bg-body-tertiary small">
                <div class="fw-bold text-body">${escapeHtml(a.intent || a.actionType)}</div>
                <div class="text-muted" style="font-size: 0.75rem;">Status: ${escapeHtml(a.status)}</div>
              </div>
            `;
          });
          html += `</div></div>`;
        }
      }

      html += `</div>`;
      detailEl.innerHTML = html;
    } catch (err) {
      detailEl.innerHTML = `<div class="p-3 text-center text-danger small">Failed to load day details</div>`;
    }
  }

  // Day selection click handlers
  const dayElements = container.querySelectorAll('.calendar-day:not(.other-month)');
  dayElements.forEach(dayEl => {
    dayEl.addEventListener('click', () => {
      dayElements.forEach(d => d.classList.remove('selected'));
      dayEl.classList.add('selected');
      
      const selectedDay = dayEl.getAttribute('data-day');
      const dateStr = dayEl.getAttribute('data-date');
      const formattedDate = `${monthNames[month]} ${selectedDay}, ${year}`;
      
      loadDayDetails(dateStr, formattedDate);
    });
  });

  // Load today by default if selected
  const selectedDayEl = container.querySelector('.calendar-day.selected:not(.other-month)');
  if (selectedDayEl) {
    const selectedDay = selectedDayEl.getAttribute('data-day');
    const dateStr = selectedDayEl.getAttribute('data-date');
    const formattedDate = `${monthNames[month]} ${selectedDay}, ${year}`;
    loadDayDetails(dateStr, formattedDate);
  }
}

/* ==========================================
   4. Task Manager (Local Memory CRUD)
   ========================================== */
let tasksStore = JSON.parse(localStorage.getItem('saas_tasks_data')) || [];

function initTaskManager() {
  const taskForm = document.getElementById('create-task-form');

  if (taskForm) {
    taskForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const titleInput = document.getElementById('task-title');
      const descInput = document.getElementById('task-desc');
      const categoryInput = document.getElementById('task-category');
      const dueDateInput = document.getElementById('task-due-date');
      const recurrenceInput = document.getElementById('task-recurrence');
      const reminderOffsetInput = document.getElementById('task-reminder-offset');

      if (!titleInput || !titleInput.value.trim()) return;

      try {
        const response = await fetch('/api/tasks/create', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: titleInput.value.trim(),
            description: descInput ? descInput.value.trim() : '',
            category: categoryInput ? categoryInput.value : '1',
            dueDate: dueDateInput ? dueDateInput.value : '',
            recurrence: recurrenceInput ? recurrenceInput.value : 'ONCE',
            reminderOffset: reminderOffsetInput ? reminderOffsetInput.value : 'NONE'
          })
        });

        const data = await response.json();
        if (data.status === 'success') {
          showToast('Task created successfully', 'success');
          taskForm.reset();
          const modalEl = document.getElementById('createTaskModal');
          if (modalEl) {
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
          }
          window.location.reload();
        } else {
          showToast('Failed to create task', 'danger');
        }
      } catch (err) {
        showToast('Error connecting to backend', 'danger');
      }
    });
  }
}

window.toggleBackendTask = async function(target) {
  const id = typeof target === 'string' ? target : target.getAttribute('data-id');
  if (!id) return;
  try {
    const res = await fetch(`/api/tasks/toggle/${id}`, { method: 'POST' });
    const data = await res.json();
    if (data.status === 'success') {
      showToast('Task status updated', 'info');
      window.location.reload();
    }
  } catch (err) {
    showToast('Failed to update task', 'danger');
  }
};

window.deleteBackendTask = async function(target) {
  const id = typeof target === 'string' ? target : target.getAttribute('data-id');
  if (!id) return;
  if (!confirm('Are you sure you want to delete this task?')) return;
  try {
    const res = await fetch(`/api/tasks/delete/${id}`, { method: 'POST' });
    const data = await res.json();
    if (data.status === 'success') {
      showToast('Task deleted successfully', 'warning');
      window.location.reload();
    } else {
      showToast('Failed to delete task', 'danger');
    }
  } catch (err) {
    showToast('Error connecting to backend', 'danger');
  }
};

window.triggerTestEmail = async function(btn) {
  const originalText = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1" role="status"></span> Sending...`;

  try {
    const res = await fetch('/api/settings/gmail/test', { method: 'POST' });
    const data = await res.json();
    if (data.status === 'success') {
      showToast(data.message || 'Test email sent successfully! Gmail connected.', 'success');
    } else {
      showToast(data.message || 'Failed to send test email', 'danger');
    }
  } catch (err) {
    showToast('Failed to connect to server for test email', 'danger');
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalText;
  }
};

window.markAllNotificationsRead = async function() {
  try {
    const res = await fetch('/api/notifications/read-all', { method: 'POST' });
    const data = await res.json();
    if (data.status === 'success') {
      showToast('All notifications marked as read', 'info');
      window.location.reload();
    }
  } catch (err) {
    showToast('Failed to update notifications', 'danger');
  }
};

function saveTasks() {
  localStorage.setItem('saas_tasks_data', JSON.stringify(tasksStore));
}

function renderTasks() {
  const container = document.getElementById('tasks-container');
  if (!container) return;

  if (tasksStore.length === 0) {
    container.innerHTML = `
      <div class="card-saas">
        <div class="empty-state">
          <div class="empty-state-icon">
            <i class="bi bi-check2-square"></i>
          </div>
          <h5 class="empty-state-title">No tasks created</h5>
          <p class="empty-state-text">You have no pending tasks. Click the button above to create your first automation task.</p>
          <button class="btn btn-primary-saas" data-bs-toggle="modal" data-bs-target="#createTaskModal">
            <i class="bi bi-plus-lg me-1"></i> Create Task
          </button>
        </div>
      </div>
    `;
    return;
  }

  let html = `<div class="d-flex flex-column gap-3">`;
  tasksStore.forEach(task => {
    html += `
      <div class="card-saas p-3 d-flex flex-row align-items-center justify-content-between ${task.completed ? 'opacity-75' : ''}">
        <div class="d-flex align-items-center gap-3">
          <input class="form-check-input mt-0 fs-5" type="checkbox" ${task.completed ? 'checked' : ''} onchange="toggleTask(${task.id})">
          <div>
            <h6 class="mb-1 fw-bold ${task.completed ? 'text-decoration-line-through text-muted' : 'text-body'}">${escapeHtml(task.title)}</h6>
            <div class="d-flex align-items-center gap-2">
              <span class="badge bg-secondary-subtle text-secondary small">${escapeHtml(task.category)}</span>
              ${task.description ? `<span class="small text-muted">${escapeHtml(task.description)}</span>` : ''}
            </div>
          </div>
        </div>
        <div class="d-flex gap-2">
          <button class="btn btn-icon btn-sm text-danger" onclick="deleteTask(${task.id})">
            <i class="bi bi-trash"></i>
          </button>
        </div>
      </div>
    `;
  });
  html += `</div>`;
  container.innerHTML = html;
}

window.toggleTask = function(id) {
  const task = tasksStore.find(t => t.id === id);
  if (task) {
    task.completed = !task.completed;
    saveTasks();
    renderTasks();
    showToast(`Task marked as ${task.completed ? 'completed' : 'pending'}`, 'info');
  }
};

window.deleteTask = function(id) {
  tasksStore = tasksStore.filter(t => t.id !== id);
  saveTasks();
  renderTasks();
  showToast('Task deleted', 'warning');
};

/* ==========================================
   5. AI Agent Chat Interface & Action Cards
   ========================================== */
function initChatInterface() {
  const chatMessages = document.getElementById('chat-messages');
  const chatInput = document.getElementById('chat-input');
  const chatSendBtn = document.getElementById('chat-send-btn');
  const promptChips = document.querySelectorAll('.prompt-chip');
  const sessionIdEl = document.getElementById('active-session-id');

  if (!chatMessages) return;

  // Render pre-existing messages loaded from backend if present
  const existingData = document.getElementById('existing-messages-data');
  if (existingData) {
    const rawMsgs = existingData.querySelectorAll('.raw-msg');
    if (rawMsgs.length > 0) {
      chatMessages.innerHTML = '';
      rawMsgs.forEach(msgEl => {
        const sender = msgEl.getAttribute('data-sender');
        const content = msgEl.getAttribute('data-content');
        const time = msgEl.getAttribute('data-time') || '';

        const msgId = msgEl.getAttribute('data-id');

        if (sender === 'USER') {
          appendUserMessageBubble(content, time);
        } else {
          appendAiMessageBubble(content, time, msgId);
        }
      });
    }
  }

  function getFileIconClass(extRaw) {
    const ext = (extRaw || '').toLowerCase();
    if (ext === 'pdf') return 'bi bi-file-earmark-pdf-fill text-danger';
    if (ext === 'doc' || ext === 'docx') return 'bi bi-file-earmark-word-fill text-primary';
    if (ext === 'csv' || ext === 'xlsx' || ext === 'xls') return 'bi bi-file-earmark-excel-fill text-success';
    if (ext === 'json' || ext === 'xml' || ext === 'html' || ext === 'js') return 'bi bi-file-earmark-code-fill text-warning';
    if (ext === 'txt' || ext === 'log' || ext === 'md') return 'bi bi-file-earmark-text-fill text-secondary';
    return 'bi bi-file-earmark-fill text-primary';
  }

  function appendUserMessageBubble(text, timeStr, attachmentInfo) {
    const time = timeStr || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userDiv = document.createElement('div');
    userDiv.className = 'chat-message user-message mb-3 align-self-end text-end';

    let attachmentHtml = '';
    if (attachmentInfo) {
      const iconClass = getFileIconClass(attachmentInfo.ext);
      attachmentHtml = `
        <div class="mb-2 p-2 rounded-3 bg-white bg-opacity-10 text-white d-flex align-items-center gap-2.5 text-start border border-white border-opacity-25" style="box-shadow: inset 0 1px 2px rgba(255,255,255,0.1);">
          <i class="${iconClass} fs-3 bg-white rounded p-1" style="line-height: 1;"></i>
          <div class="text-truncate">
            <div class="fw-bold small text-truncate">${escapeHtml(attachmentInfo.fileName)}</div>
            <div class="small opacity-75" style="font-size:0.68rem;"><i class="bi bi-paperclip me-1"></i>Attached Document (${(attachmentInfo.size / 1024).toFixed(1)} KB)</div>
          </div>
        </div>
      `;
    }

    const formattedBody = escapeHtml(text).replace(/\n/g, '<br>');
    let messageBodyHtml = formattedBody;
    let readMoreBtnHtml = '';

    if (formattedBody.length > 450) {
      messageBodyHtml = `<div class="chat-collapsible-wrapper collapsed">${formattedBody}</div>`;
      readMoreBtnHtml = `<button type="button" class="read-toggle-btn text-white-50 mt-1" onclick="toggleReadMore(this)"><i class="bi bi-chevron-down me-1"></i>Read More</button>`;
    }

    userDiv.innerHTML = `
      <div class="d-inline-block text-start chat-message-bubble">
        <div class="d-flex align-items-center justify-content-end gap-2 mb-1">
          <span class="small text-muted">${time}</span>
          <span class="fw-bold small text-body">You</span>
        </div>
        <div class="p-3 rounded-3 bg-primary text-white shadow-sm chat-message-content">
          ${attachmentHtml}
          ${messageBodyHtml}
          ${readMoreBtnHtml}
        </div>
      </div>
    `;
    chatMessages.appendChild(userDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  function appendAiMessageBubble(rawContent, timeStr, msgId) {
    let parsed = null;
    try {
      let clean = rawContent.trim();
      if (clean.includes('```')) {
        clean = clean.replace(/```[a-zA-Z]*/g, '').replace(/```/g, '').trim();
      }
      const firstBrace = clean.indexOf('{');
      const lastBrace = clean.lastIndexOf('}');
      if (firstBrace >= 0 && lastBrace > firstBrace) {
        clean = clean.substring(firstBrace, lastBrace + 1).trim();
      }
      parsed = JSON.parse(clean);
    } catch (e) {
      parsed = { type: 'message', message: rawContent };
    }

    const time = timeStr || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const aiDiv = document.createElement('div');
    aiDiv.className = 'chat-message ai-message mb-3 align-self-start';

    let contentHtml = "";

    if (parsed && (parsed.type === 'confirmation' || parsed.action)) {
      const actionStatus = parsed.actionStatus || (parsed.data ? parsed.data.actionStatus : null);
      contentHtml = renderActionCardHtml(parsed.action || 'create_tasks', parsed.message || 'Proposed Action', parsed.data || parsed, msgId, actionStatus, parsed.actionResultMessage);
    } else {
      let textMsg = rawContent;
      if (parsed && typeof parsed === 'object') {
        textMsg = parsed.message || parsed.content || parsed.response || parsed.text || parsed.answer || rawContent;
      }
      const formattedText = escapeHtml(textMsg).replace(/\n/g, '<br>');
      let bodyHtml = formattedText;
      let readMoreBtn = '';
      if (formattedText.length > 450) {
        bodyHtml = `<div class="chat-collapsible-wrapper collapsed">${formattedText}</div>`;
        readMoreBtn = `<button type="button" class="read-toggle-btn text-primary mt-1" onclick="toggleReadMore(this)"><i class="bi bi-chevron-down me-1"></i>Read More</button>`;
      }

      contentHtml = `
        <div class="p-3 rounded-3 bg-body-tertiary border text-body shadow-sm chat-message-content">
          ${bodyHtml}
          ${readMoreBtn}
        </div>
      `;
    }

    aiDiv.innerHTML = `
      <div class="d-inline-block text-start chat-message-bubble">
        <div class="d-flex align-items-center gap-2 mb-1">
          <div class="sidebar-brand-icon bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 24px; height: 24px; font-size: 0.75rem;">
            <i class="bi bi-robot"></i>
          </div>
          <span class="fw-bold small text-body">AI Automation Agent</span>
          <span class="small text-muted ms-auto">${time}</span>
        </div>
        ${contentHtml}
      </div>
    `;

    chatMessages.appendChild(aiDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  let attachedFileData = null;
  let isUploading = false;

  const chatAttachBtn = document.getElementById('chat-attach-btn');
  const chatFileInput = document.getElementById('chat-file-input');
  const attachedFilePreview = document.getElementById('attached-file-preview');
  const attachedFileName = document.getElementById('attached-file-name');
  const attachedFileSize = document.getElementById('attached-file-size');
  const removeFileBtn = document.getElementById('remove-file-btn');

  function setUploadingState(uploading) {
    isUploading = uploading;
    if (chatSendBtn) {
      chatSendBtn.disabled = uploading;
      if (uploading) {
        chatSendBtn.classList.add('disabled', 'opacity-50');
        chatSendBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span>';
      } else {
        chatSendBtn.classList.remove('disabled', 'opacity-50');
        chatSendBtn.innerHTML = '<i class="bi bi-send-fill"></i>';
      }
    }
    if (chatAttachBtn) {
      chatAttachBtn.disabled = uploading;
    }
  }

  if (chatAttachBtn && chatFileInput) {
    chatAttachBtn.addEventListener('click', () => {
      if (isUploading) return;
      chatFileInput.click();
    });

    chatFileInput.addEventListener('change', async () => {
      const file = chatFileInput.files[0];
      if (!file) return;

      const allowedExtensions = ['txt', 'pdf', 'csv', 'json', 'doc', 'docx', 'md', 'log', 'xml', 'html'];
      const fileName = file.name;
      const ext = fileName.split('.').pop().toLowerCase();

      if (!allowedExtensions.includes(ext)) {
        showToast(`Unsupported file format (.${ext}). Supported formats: TXT, PDF, CSV, JSON, DOC, DOCX, MD, LOG.`, 'danger');
        chatFileInput.value = '';
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      setUploadingState(true);

      try {
        showToast(`Uploading ${fileName}...`, 'info');
        const res = await fetch('/api/chat/upload', {
          method: 'POST',
          body: formData
        });
        const data = await res.json();

        if (res.ok && data.status === 'success') {
          attachedFileData = data;
          const fileExt = (data.fileType || data.fileName.split('.').pop()).toLowerCase();
          const attachedFileIcon = document.getElementById('attached-file-icon');
          if (attachedFileIcon) {
            attachedFileIcon.className = `${getFileIconClass(fileExt)} fs-5 me-1`;
          }
          if (attachedFileName) attachedFileName.innerText = data.fileName;
          if (attachedFileSize) attachedFileSize.innerText = `(${(data.fileSize / 1024).toFixed(1)} KB)`;
          if (attachedFilePreview) attachedFilePreview.classList.remove('d-none');
          showToast(`Attached ${data.fileName} successfully`, 'success');
        } else {
          showToast(data.message || 'Failed to upload file', 'danger');
          chatFileInput.value = '';
        }
      } catch (e) {
        showToast('Error uploading file to server', 'danger');
        chatFileInput.value = '';
      } finally {
        setUploadingState(false);
      }
    });
  }

  function removeAttachedFile() {
    attachedFileData = null;
    if (chatFileInput) chatFileInput.value = '';
    if (attachedFilePreview) attachedFilePreview.classList.add('d-none');
  }

  if (removeFileBtn) {
    removeFileBtn.addEventListener('click', removeAttachedFile);
  }

  async function sendMessage() {
    if (isUploading) {
      showToast('Please wait for file upload to complete', 'warning');
      return;
    }

    const text = chatInput.value.trim();
    if (!text && !attachedFileData) return;

    // Clear empty state if visible
    const emptyState = chatMessages.querySelector('.empty-state');
    if (emptyState) {
      chatMessages.innerHTML = '';
    }

    let attachmentInfo = null;
    if (attachedFileData) {
      attachmentInfo = {
        fileName: attachedFileData.fileName,
        ext: (attachedFileData.fileType || attachedFileData.fileName.split('.').pop()).toLowerCase(),
        size: attachedFileData.fileSize
      };
    }

    const displayText = text || (attachedFileData ? `Analyzing document: ${attachedFileData.fileName}` : '');
    appendUserMessageBubble(displayText, null, attachmentInfo);
    chatInput.value = '';

    let promptToSend = text;
    if (attachedFileData && attachedFileData.content) {
      promptToSend = `[DOCUMENT ATTACHMENT: ${attachedFileData.fileName}]\n[Document Full Extracted Content]:\n${attachedFileData.content}\n\n[User Query/Instruction]: ${text || 'Please read, summarize, and analyze this document.'}`;
    }

    removeAttachedFile();

    // Append AI typing indicator
    const typingDiv = document.createElement('div');
    typingDiv.className = 'chat-message ai-message mb-3 align-self-start';
    typingDiv.id = 'ai-typing-indicator';
    typingDiv.innerHTML = `
      <div class="p-3 rounded-3 bg-body-tertiary border text-muted shadow-sm d-flex align-items-center gap-2">
        <span class="spinner-border spinner-border-sm text-primary" role="status"></span>
        <span>AI Automation Agent is reasoning...</span>
      </div>
    `;
    chatMessages.appendChild(typingDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;

    try {
      const sessionId = sessionIdEl ? sessionIdEl.value : '';
      const response = await fetch('/api/chat/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, prompt: promptToSend, displayPrompt: displayText })
      });

      const data = await response.json();
      typingDiv.remove();

      if (data && data.aiResponse) {
        appendAiMessageBubble(data.aiResponse, null, data.messageId);
      } else {
        showToast('Error receiving response from AI Agent', 'danger');
      }
    } catch (err) {
      if (typingDiv) typingDiv.remove();
      showToast('Connection failed to AI Agent backend', 'danger');
    }
  }

  const chatModelSelect = document.getElementById('chat-model-select');
  if (chatModelSelect) {
    chatModelSelect.addEventListener('change', async () => {
      const model = chatModelSelect.value;
      const sessionId = sessionIdEl ? sessionIdEl.value : '';
      try {
        await fetch(`/api/chat/session/model?sessionId=${sessionId}&model=${encodeURIComponent(model)}`, { method: 'POST' });
        showToast(`AI Model set to ${model}`, 'info');
      } catch (e) {
        showToast('Failed to switch model', 'danger');
      }
    });
  }

  if (chatSendBtn) {
    chatSendBtn.addEventListener('click', () => {
      sendMessage();
      if (chatInput) chatInput.style.height = 'auto';
    });
  }

  if (chatInput) {
    chatInput.addEventListener('input', function() {
      this.style.height = 'auto';
      this.style.height = Math.min(this.scrollHeight, 140) + 'px';
    });

    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
        chatInput.style.height = 'auto';
      }
    });
  }

  promptChips.forEach(chip => {
    chip.addEventListener('click', () => {
      if (chatInput) {
        chatInput.value = chip.getAttribute('data-prompt') || chip.innerText.trim();
        sendMessage();
      }
    });
  });
}

function renderActionCardHtml(action, message, data, messageId, actionStatus, actionResultMessage) {
  const dataJsonStr = escapeHtml(JSON.stringify(data || {}));
  let cardTitle = "Proposed Automation Action";
  let cardIcon = "bi-lightning-charge-fill";

  if (action === "create_tasks") {
    cardTitle = "Task Generation Approval";
    cardIcon = "bi-check2-square";
  } else if (action === "send_email") {
    cardTitle = "Email Dispatch Approval";
    cardIcon = "bi-envelope-paper-fill";
  } else if (action === "create_reminder") {
    cardTitle = "Schedule Reminder Approval";
    cardIcon = "bi-alarm-fill";
  }

  let dataPreviewHtml = "";

  if (action === "create_tasks" && data && data.tasks && Array.isArray(data.tasks)) {
    dataPreviewHtml = `<div class="d-flex flex-column gap-2 mt-2">`;
    data.tasks.forEach((t, i) => {
      let categoryName = t.category || 'General Automation';
      if (categoryName === '1') categoryName = '1. General Automation';
      else if (categoryName === '2') categoryName = '2. Email Management';
      else if (categoryName === '3') categoryName = '3. Schedule & Reminders';
      else if (categoryName === '4') categoryName = '4. Database Management';

      dataPreviewHtml += `
        <div class="p-2 border rounded bg-body small d-flex align-items-center justify-content-between">
          <div>
            <div class="fw-bold text-body">${i + 1}. ${escapeHtml(t.title || 'New Task')}</div>
            ${t.description ? `<div class="text-muted small">${escapeHtml(t.description)}</div>` : ''}
          </div>
          <span class="badge bg-primary-subtle text-primary">${escapeHtml(categoryName)}</span>
        </div>
      `;
    });
    dataPreviewHtml += `</div>`;
  } else if (action === "send_email" && data) {
    dataPreviewHtml = `
      <div class="p-3 border rounded bg-body small mt-2">
        <div class="mb-1"><strong>To:</strong> <span class="text-primary">${escapeHtml(data.recipient || '')}</span></div>
        <div class="mb-2"><strong>Subject:</strong> <span class="fw-bold">${escapeHtml(data.subject || '')}</span></div>
        <div class="p-2 rounded bg-body-tertiary border text-secondary">${escapeHtml(data.body || '').replace(/\n/g, '<br>')}</div>
      </div>
    `;
  } else if (action === "create_reminder" && data) {
    dataPreviewHtml = `
      <div class="p-3 border rounded bg-body small mt-2">
        <div class="fw-bold text-body mb-1"><i class="bi bi-clock me-1 text-primary"></i> ${escapeHtml(data.title || 'Reminder')}</div>
        <div class="text-muted">${escapeHtml(data.description || '')}</div>
      </div>
    `;
  }

  let buttonsOrStatusHtml = "";
  if (actionStatus === 'executed') {
    buttonsOrStatusHtml = `
      <span class="badge bg-success-subtle text-success p-2 small">
        <i class="bi bi-check-circle-fill me-1"></i> ${escapeHtml(actionResultMessage || 'Executed successfully')}
      </span>
    `;
  } else if (actionStatus === 'cancelled') {
    buttonsOrStatusHtml = `
      <span class="badge bg-secondary-subtle text-secondary p-2 small">
        <i class="bi bi-x-circle me-1"></i> Action Cancelled
      </span>
    `;
  } else {
    buttonsOrStatusHtml = `
      <button class="btn btn-secondary-saas btn-sm" data-msg-id="${messageId || ''}" onclick="cancelChatAction(this)">Cancel</button>
      <button class="btn btn-primary-saas btn-sm px-3" data-msg-id="${messageId || ''}" data-action="${action}" data-payload='${dataJsonStr}' data-card-message="${escapeHtml(message || '')}" onclick="executeChatAction(this)">
        <i class="bi bi-check-circle me-1"></i> Confirm Action
      </button>
    `;
  }

  const badgeHeader = actionStatus === 'executed' ? `<span class="badge bg-success-subtle text-success">Executed</span>` :
                      actionStatus === 'cancelled' ? `<span class="badge bg-secondary-subtle text-secondary">Cancelled</span>` :
                      `<span class="badge bg-warning-subtle text-warning-emphasis">Requires Approval</span>`;

  return `
    <div class="action-card card-saas border-primary p-3 shadow-sm" data-msg-id="${messageId || ''}">
      <div class="d-flex align-items-center justify-content-between border-bottom pb-2 mb-2">
        <div class="d-flex align-items-center gap-2 text-primary fw-bold">
          <i class="bi ${cardIcon} fs-5"></i>
          <span>${cardTitle}</span>
        </div>
        ${badgeHeader}
      </div>
      <p class="small text-body mb-2 fw-semibold">${escapeHtml(message || '')}</p>
      ${dataPreviewHtml}
      <div class="action-card-buttons mt-3 pt-2 border-top d-flex gap-2 justify-content-end">
        ${buttonsOrStatusHtml}
      </div>
    </div>
  `;
}

window.executeChatAction = async function(btn) {
  const action = btn.getAttribute('data-action');
  const messageId = btn.getAttribute('data-msg-id') || '';
  const cardMessage = btn.getAttribute('data-card-message') || '';
  const payloadStr = btn.getAttribute('data-payload');
  let data = {};
  try {
    data = JSON.parse(payloadStr);
  } catch (e) {}

  const card = btn.closest('.action-card');
  const buttonsContainer = card.querySelector('.action-card-buttons');
  buttonsContainer.innerHTML = `
    <div class="d-flex align-items-center gap-2 text-primary small fw-semibold">
      <span class="spinner-border spinner-border-sm" role="status"></span> Executing automation...
    </div>
  `;

  try {
    const res = await fetch('/api/chat/action/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ messageId, action, message: cardMessage, data })
    });
    const result = await res.json();
    if (result.status === 'success') {
      buttonsContainer.innerHTML = `
        <span class="badge bg-success-subtle text-success p-2 small">
          <i class="bi bi-check-circle-fill me-1"></i> ${escapeHtml(result.message || 'Executed successfully')}
        </span>
      `;
      showToast(result.message || 'Action executed successfully', 'success');
    } else {
      buttonsContainer.innerHTML = `
        <span class="badge bg-danger-subtle text-danger p-2 small">
          <i class="bi bi-exclamation-triangle-fill me-1"></i> ${escapeHtml(result.message || 'Execution failed')}
        </span>
      `;
      showToast(result.message || 'Action execution failed', 'danger');
    }
  } catch (err) {
    buttonsContainer.innerHTML = `
      <span class="badge bg-danger-subtle text-danger p-2 small">
        <i class="bi bi-exclamation-triangle-fill me-1"></i> Server connection failed
      </span>
    `;
    showToast('Failed to connect to backend', 'danger');
  }
};

window.cancelChatAction = async function(btn) {
  const messageId = btn.getAttribute('data-msg-id') || '';
  const card = btn.closest('.action-card');
  const buttonsContainer = card.querySelector('.action-card-buttons');
  buttonsContainer.innerHTML = `
    <span class="badge bg-secondary-subtle text-secondary p-2 small">
      <i class="bi bi-x-circle me-1"></i> Action Cancelled
    </span>
  `;

  try {
    await fetch('/api/chat/action/cancel', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ messageId })
    });
  } catch (e) {}

  showToast('Action cancelled', 'info');
};

/* ==========================================
   6. Toast System Helper
   ========================================== */
function initToastContainer() {
  if (!document.getElementById('toast-container')) {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
    container.style.zIndex = '1090';
    document.body.appendChild(container);
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const bgClass = type === 'success' ? 'bg-success text-white' :
                  type === 'danger' ? 'bg-danger text-white' :
                  type === 'warning' ? 'bg-warning text-dark' : 'bg-primary text-white';

  const iconClass = type === 'success' ? 'bi-check-circle-fill' :
                    type === 'danger' ? 'bi-x-circle-fill' :
                    type === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill';

  const toastId = 'toast-' + Date.now();
  const toastHTML = `
    <div id="${toastId}" class="toast align-items-center ${bgClass} border-0 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true">
      <div class="d-flex">
        <div class="toast-body d-flex align-items-center gap-2">
          <i class="bi ${iconClass} fs-5"></i>
          <span>${escapeHtml(message)}</span>
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
    </div>
  `;

  container.insertAdjacentHTML('beforeend', toastHTML);
  const toastEl = document.getElementById(toastId);
  const bsToast = new bootstrap.Toast(toastEl, { delay: 3500 });
  bsToast.show();

  toastEl.addEventListener('hidden.bs.toast', () => {
    toastEl.remove();
  });
}

function escapeHtml(str) {
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}
