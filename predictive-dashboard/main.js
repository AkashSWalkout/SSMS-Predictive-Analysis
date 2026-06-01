import Chart from 'chart.js/auto';

const API_BASE = window.location.port === '5173'
  ? 'http://localhost:9091/api/predictive'
  : '/api/predictive';

// ===== DOM REFS =====
const sidebar = document.getElementById('sidebar');
const sidebarToggle = document.getElementById('sidebarToggle');
const fileInput = document.getElementById('fileInput');
const dropzone = document.getElementById('dropzone');
const uploadFooter = document.getElementById('uploadFooter');
const selectedFileName = document.getElementById('selectedFileName');
const clearFile = document.getElementById('clearFile');
const uploadForm = document.getElementById('uploadForm');
const analyzeBtn = document.getElementById('analyzeBtn');
const errorBanner = document.getElementById('errorBanner');
const loadingOverlay = document.getElementById('loadingOverlay');
const progressFill = document.getElementById('progressFill');
const resultsSection = document.getElementById('resultsSection');
const uploadCard = document.getElementById('uploadCard');
const newAnalysisBtn = document.getElementById('newAnalysisBtn');
const exportBtn = document.getElementById('exportBtn');

const charts = {};
let lastAnalysis = null;

// ===== CHART DEFAULTS =====
Chart.defaults.color = '#94a3b8';
Chart.defaults.borderColor = 'rgba(255,255,255,0.04)';
Chart.defaults.font.family = "'Inter', system-ui, sans-serif";

// ===== SIDEBAR =====
sidebarToggle.addEventListener('click', () => sidebar.classList.toggle('open'));

document.querySelectorAll('.nav-link').forEach(link => {
  link.addEventListener('click', (e) => {
    e.preventDefault();
    const view = link.dataset.view;
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    link.classList.add('active');
    document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));
    const panel = document.getElementById('view' + view.charAt(0).toUpperCase() + view.slice(1));
    if (panel) panel.classList.add('active');
    sidebar.classList.remove('open');
  });
});

// ===== TABS =====
document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    const panel = document.getElementById('panel-' + tab.dataset.tab);
    if (panel) panel.classList.add('active');
  });
});

// ===== FILE UPLOAD =====
dropzone.addEventListener('click', () => fileInput.click());
dropzone.addEventListener('dragover', e => { e.preventDefault(); dropzone.classList.add('dragover'); });
dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
dropzone.addEventListener('drop', e => {
  e.preventDefault();
  dropzone.classList.remove('dragover');
  if (e.dataTransfer.files.length) { fileInput.files = e.dataTransfer.files; onFileSelected(); }
});
fileInput.addEventListener('change', onFileSelected);
clearFile.addEventListener('click', () => { fileInput.value = ''; uploadFooter.style.display = 'none'; });

function onFileSelected() {
  if (!fileInput.files[0]) return;
  selectedFileName.textContent = fileInput.files[0].name;
  uploadFooter.style.display = 'flex';
}

// ===== SUBMIT =====
uploadForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const file = fileInput.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  hideError();
  setLoading(true);

  try {
    const res = await fetch(`${API_BASE}/analyze-file?docType=report_card`, { method: 'POST', body: formData });
    if (!res.ok) throw new Error(await res.text());
    const analysis = await res.json();
    lastAnalysis = analysis;
    renderAnalysis(analysis);
  } catch (err) {
    showError('Analysis failed. Ensure the backend is running on port 9091.');
    console.error(err);
  } finally {
    setLoading(false);
  }
});

// ===== NEW ANALYSIS =====
newAnalysisBtn.addEventListener('click', resetToUpload);
function resetToUpload() {
  resultsSection.style.display = 'none';
  uploadCard.style.display = 'block';
  uploadFooter.style.display = 'none';
  fileInput.value = '';
  document.getElementById('confidenceWarning').style.display = 'none';
}

// ===== EXPORT =====
exportBtn.addEventListener('click', () => {
  if (!lastAnalysis) return;
  const blob = new Blob([JSON.stringify(lastAnalysis, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = `analysis_${lastAnalysis.studentName || 'report'}.json`; a.click();
  URL.revokeObjectURL(url);
});

// ===== LOADING =====
function setLoading(on) {
  loadingOverlay.style.display = on ? 'flex' : 'none';
  analyzeBtn.disabled = on;
  if (on) {
    progressFill.style.width = '0%';
    let w = 0;
    const iv = setInterval(() => { w += Math.random() * 15; if (w > 90) clearInterval(iv); progressFill.style.width = Math.min(w, 90) + '%'; }, 300);
    loadingOverlay._iv = iv;
  } else {
    clearInterval(loadingOverlay._iv);
    progressFill.style.width = '100%';
  }
}

function showError(msg) { errorBanner.textContent = msg; errorBanner.style.display = 'block'; }
function hideError() { errorBanner.style.display = 'none'; }

// ===== RENDER ANALYSIS =====
function renderAnalysis(a) {
  uploadCard.style.display = 'none';
  resultsSection.style.display = 'block';

  // Student header
  const name = a.studentName || 'Student';
  document.getElementById('studentName').textContent = name;
  document.getElementById('studentAvatar').textContent = getInitials(name);
  document.getElementById('studentClass').textContent = a.className || 'N/A';
  document.getElementById('studentRoll').textContent = a.rollNumber || 'N/A';

  // Score gauge
  const pct = a.overallPercentage || 0;
  const circumference = 326.73;
  const offset = circumference * (1 - pct / 100);
  const ring = document.getElementById('gaugeRing');
  ring.style.stroke = pct >= 80 ? '#22c55e' : pct >= 60 ? '#f59e0b' : '#ef4444';
  setTimeout(() => { ring.style.strokeDashoffset = offset; }, 100);
  animateCounter('gaugeValue', 0, pct, '%', 1200);
  document.getElementById('gaugeGrade').textContent = 'Grade ' + (a.overallGrade || 'N/A');

  // KPIs
  const dash = a.dashboardData || {};

  // Risk KPI
  const riskDist = dash.riskDistribution || {};
  const riskLevel = Object.keys(riskDist)[0] || 'MODERATE';
  const riskEl = document.getElementById('kpiRisk');
  riskEl.textContent = riskLevel;
  const riskColors = { LOW: '#22c55e', MODERATE: '#f59e0b', HIGH: '#ef4444', CRITICAL: '#ef4444' };
  riskEl.style.color = riskColors[riskLevel] || '#f59e0b';

  // Charts
  renderSubjectChart(a.subjectPerformanceChart);
  renderRadarChart(a.subjectPerformanceChart);
  renderRiskChart(riskDist);

  // Subject table
  renderSubjectTable(a.subjectPerformanceChart);

  // AI Insights
  renderInsights(a);

  // Reset tabs to first
  document.querySelectorAll('.tab')[0]?.click();
}

// ===== CHARTS =====
function makeChart(id, config) {
  if (charts[id]) charts[id].destroy();
  const ctx = document.getElementById(id);
  if (!ctx) return;
  charts[id] = new Chart(ctx.getContext('2d'), config);
}

function renderSubjectChart(cd) {
  if (!cd) return;
  makeChart('subjectChart', {
    type: 'bar',
    data: {
      labels: cd.labels,
      datasets: cd.datasets.map(ds => ({
        label: ds.label, data: ds.data,
        backgroundColor: ds.backgroundColor || ['#6366f1', '#8b5cf6', '#a855f7', '#d946ef', '#ec4899'],
        borderRadius: 8, borderSkipped: false, maxBarThickness: 44,
      }))
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true, max: 100, grid: { color: 'rgba(255,255,255,0.04)' } }, x: { grid: { display: false } } }
    }
  });
}

function renderRadarChart(cd) {
  if (!cd) return;
  makeChart('radarChart', {
    type: 'radar',
    data: {
      labels: cd.labels,
      datasets: [{
        label: 'Score', data: cd.datasets[0]?.data || [],
        backgroundColor: 'rgba(99,102,241,0.15)', borderColor: '#6366f1',
        borderWidth: 2, pointBackgroundColor: '#6366f1', pointRadius: 4,
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      scales: { r: { beginAtZero: true, max: 100, ticks: { display: false }, grid: { color: 'rgba(255,255,255,0.06)' }, pointLabels: { color: '#94a3b8', font: { size: 11 } } } },
      plugins: { legend: { display: false } }
    }
  });
}

function renderAttendanceChart(cd) {
  if (!cd) return;
  makeChart('attendanceChart', {
    type: 'doughnut',
    data: {
      labels: cd.labels,
      datasets: [{
        data: cd.datasets[0]?.data || [],
        backgroundColor: cd.datasets[0]?.backgroundColor || ['#22c55e', '#ef4444'],
        borderWidth: 0, cutout: '72%',
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyle: 'circle' } } }
    }
  });
}

function renderRiskChart(riskDist) {
  const labels = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'];
  const data = labels.map(l => Number(riskDist[l] || 0));
  const colors = ['#22c55e', '#f59e0b', '#ef4444', '#dc2626'];
  makeChart('riskChart', {
    type: 'doughnut',
    data: { labels, datasets: [{ data, backgroundColor: colors, borderWidth: 0, cutout: '72%' }] },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyle: 'circle' } } }
    }
  });
}

// ===== SUBJECT TABLE =====
function renderSubjectTable(cd) {
  const tbody = document.getElementById('subjectTableBody');
  if (!tbody || !cd) return;
  tbody.innerHTML = '';
  cd.labels.forEach((label, i) => {
    const score = cd.datasets[0]?.data[i] || 0;
    const grade = score >= 90 ? 'A+' : score >= 80 ? 'A' : score >= 70 ? 'B+' : score >= 60 ? 'B' : score >= 50 ? 'C' : 'D';
    const status = score >= 85 ? 'excellent' : score >= 70 ? 'good' : score >= 50 ? 'average' : 'poor';
    const statusLabel = score >= 85 ? 'Excellent' : score >= 70 ? 'Good' : score >= 50 ? 'Average' : 'Needs Work';
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${label}</td><td><strong>${score}</strong>/100</td><td>${grade}</td><td><span class="status-pill ${status}">${statusLabel}</span></td>`;
    tbody.appendChild(tr);
  });
}

// ===== AI INSIGHTS =====
function renderInsights(a) {
  const cd = a.subjectPerformanceChart;
  const strengths = [], improvements = [], recs = [];

  if (cd && cd.datasets[0]) {
    const scores = cd.datasets[0].data;
    const labels = cd.labels;
    const sorted = labels.map((l, i) => ({ name: l, score: scores[i] })).sort((a, b) => b.score - a.score);
    sorted.forEach(s => {
      if (s.score >= 70) strengths.push(`Strong performance in ${s.name} with a score of ${s.score}/100`);
      else if (s.score >= 50) improvements.push(`${s.name} scored ${s.score}/100 — has room for improvement`);
      else if (s.score > 0) improvements.push(`${s.name} scored ${s.score}/100 — needs immediate attention`);
      else improvements.push(`${s.name} — score could not be extracted from the report card`);
    });
  }

  const pct = a.overallPercentage || 0;
  if (pct >= 80) strengths.push('Overall academic standing is above average — excellent trajectory');
  else if (pct >= 60) improvements.push('Overall percentage could be improved with focused study plans');
  else improvements.push('Overall performance needs immediate attention and support');

  recs.push('Schedule a one-on-one meeting with the student to discuss progress');
  recs.push('Consider peer study groups for subjects with lower scores');
  if (pct >= 80) recs.push('Recommend advanced placement or enrichment programs');
  else recs.push('Assign a tutor for subjects scoring below 70');
  recs.push('Monitor attendance trends over the next 30 days');

  const summary = `${a.studentName || 'The student'} has an overall score of ${fmt(pct)}% (Grade ${a.overallGrade || 'N/A'}). ` +
    `AI Confidence in this prediction is ${fmt((a.dashboardData?.averageConfidence || 0) * 100)}%. ` +
    `The risk level is assessed as ${Object.keys(a.dashboardData?.riskDistribution || {})[0] || 'MODERATE'}. ` +
    `This analysis was generated from the uploaded file and may be refined with additional data.`;

  fillList('strengthsList', strengths);
  fillList('improvementsList', improvements.length ? improvements : ['No significant concerns identified']);
  fillList('recommendationsList', recs);
  document.getElementById('summaryText').textContent = summary;
}

function fillList(id, items) {
  const ul = document.getElementById(id);
  if (!ul) return;
  ul.innerHTML = items.map(t => `<li>${t}</li>`).join('');
}

// ===== UTILS =====
function getInitials(name) {
  return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
}

function fmt(v) {
  return Number(v || 0).toFixed(1).replace(/\.0$/, '');
}

function animateCounter(id, from, to, suffix, duration) {
  const el = document.getElementById(id);
  if (!el) return;
  const start = performance.now();
  const step = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    const ease = 1 - Math.pow(1 - progress, 3);
    el.textContent = fmt(from + (to - from) * ease) + (suffix || '');
    if (progress < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

// ===== EXAM ANALYSIS RENDERING =====
function renderExamAnalysis(a) {
  const summary = a.examAnalysis || {};
  const strengths = summary.conceptualStrengths || [];
  const gaps = summary.conceptualGaps || [];
  const handwritingScore = summary.handwritingScore || 0;
  const handwritingQuality = summary.handwritingQuality || 'N/A';
  const handwritingNotes = summary.handwritingNotes || 'Handwriting analysis was not available for this document.';
  const answerAnalysis = summary.detailedAnalysis || a.performanceSummary || 'Detailed answer analysis will appear here after processing.';

  // Handwriting meter
  const meter = document.getElementById('handwritingMeter');
  const label = document.getElementById('handwritingLabel');
  setTimeout(() => { meter.style.width = handwritingScore + '%'; }, 200);
  label.textContent = handwritingQuality;
  document.getElementById('handwritingNotes').textContent = handwritingNotes;

  // Conceptual strengths & gaps
  fillList('conceptStrengthsList', strengths.length ? strengths : ['No specific conceptual strengths identified yet']);
  fillList('conceptGapsList', gaps.length ? gaps : ['No significant conceptual gaps detected']);

  // Detailed answer analysis
  document.getElementById('answerAnalysisText').textContent = answerAnalysis;
}

// ============================================================
// ===== EXAM ANSWER SHEET PAGE (SEPARATE FROM DASHBOARD) =====
// ============================================================
(function initExamPage() {
  const examUploadCard = document.getElementById('examUploadCard');
  const examUploadForm = document.getElementById('examUploadForm');
  const examDropzone = document.getElementById('examDropzone');
  const examFileInput = document.getElementById('examFileInput');
  const examUploadFooter = document.getElementById('examUploadFooter');
  const examSelectedFileName = document.getElementById('examSelectedFileName');
  const examClearFile = document.getElementById('examClearFile');
  const examAnalyzeBtn = document.getElementById('examAnalyzeBtn');
  const examLoadingOverlay = document.getElementById('examLoadingOverlay');
  const examProgressFill = document.getElementById('examProgressFill');
  const examErrorBanner = document.getElementById('examErrorBanner');
  const examResultsSection = document.getElementById('examResultsSection');
  const examNewBtn = document.getElementById('examNewBtn');
  const examExportBtn = document.getElementById('examExportBtn');

  if (!examDropzone) return; // guard

  let lastExamAnalysis = null;

  // Drag & drop
  examDropzone.addEventListener('click', () => examFileInput.click());
  examDropzone.addEventListener('dragover', e => { e.preventDefault(); examDropzone.classList.add('dragover'); });
  examDropzone.addEventListener('dragleave', () => examDropzone.classList.remove('dragover'));
  examDropzone.addEventListener('drop', e => {
    e.preventDefault();
    examDropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length) { examFileInput.files = e.dataTransfer.files; onExamFileSelected(); }
  });
  examFileInput.addEventListener('change', onExamFileSelected);
  examClearFile.addEventListener('click', () => { examFileInput.value = ''; examUploadFooter.style.display = 'none'; });

  function onExamFileSelected() {
    if (!examFileInput.files[0]) return;
    examSelectedFileName.textContent = examFileInput.files[0].name;
    examUploadFooter.style.display = 'flex';
  }

  // Submit
  examUploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const file = examFileInput.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    examErrorBanner.style.display = 'none';
    setExamLoading(true);

    try {
      const res = await fetch(`${API_BASE}/analyze-file?docType=exam_paper`, { method: 'POST', body: formData });
      if (!res.ok) throw new Error(await res.text());
      const analysis = await res.json();
      lastExamAnalysis = analysis;
      renderExamResults(analysis);
    } catch (err) {
      examErrorBanner.textContent = 'Analysis failed. Ensure the backend is running on port 9091.';
      examErrorBanner.style.display = 'block';
      console.error(err);
    } finally {
      setExamLoading(false);
    }
  });

  // Loading
  function setExamLoading(on) {
    examLoadingOverlay.style.display = on ? 'flex' : 'none';
    examAnalyzeBtn.disabled = on;
    if (on) {
      examProgressFill.style.width = '0%';
      let w = 0;
      const iv = setInterval(() => { w += Math.random() * 10; if (w > 90) clearInterval(iv); examProgressFill.style.width = Math.min(w, 90) + '%'; }, 400);
      examLoadingOverlay._iv = iv;
    } else {
      clearInterval(examLoadingOverlay._iv);
      examProgressFill.style.width = '100%';
    }
  }

  // Render exam results
  function renderExamResults(a) {
    examUploadCard.style.display = 'none';
    examResultsSection.style.display = 'block';

    // Student header
    const name = a.studentName || 'Student';
    document.getElementById('examStudentName').textContent = name;
    document.getElementById('examStudentAvatar').textContent = getInitials(name);
    document.getElementById('examStudentClass').textContent = a.className || 'N/A';
    document.getElementById('examStudentRoll').textContent = a.rollNumber || 'N/A';

    // Render exam analysis cards
    renderExamAnalysis(a);

    // Confidence warning
    const conf = (a.dashboardData?.averageConfidence || a.examAnalysis?.handwritingScore / 100 || 0) * 100;
    const confWarning = document.getElementById('examConfidenceWarning');
    confWarning.style.display = conf < 60 ? 'flex' : 'none';
  }

  // New analysis
  examNewBtn.addEventListener('click', () => {
    examResultsSection.style.display = 'none';
    examUploadCard.style.display = 'block';
    examUploadFooter.style.display = 'none';
    examFileInput.value = '';
    document.getElementById('examConfidenceWarning').style.display = 'none';
  });

  // Export
  examExportBtn.addEventListener('click', () => {
    if (!lastExamAnalysis) return;
    const blob = new Blob([JSON.stringify(lastExamAnalysis, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `exam_analysis_${lastExamAnalysis.studentName || 'report'}.json`; a.click();
    URL.revokeObjectURL(url);
  });
})();
