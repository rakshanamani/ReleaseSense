/**
 * ReleaseSense - Frontend Application Controller
 * Handles form validation, asynchronous API communication, canvas charts,
 * history management, and real-time system health checks.
 */

document.addEventListener("DOMContentLoaded", () => {
    ReleaseSenseApp.init();
});

const ReleaseSenseApp = (() => {
    // API Endpoints
    const API = {
        PREDICT: "api/predict",
        HISTORY: "api/history",
        HEALTH: "api/health"
    };

    // State
    let historyData = [];
    let activeFilter = "ALL";

    // DOM Elements
    let elements = {};

    function init() {
        cacheElements();
        bindEvents();
        checkSystemHealth();
        loadHistoryAndStats();

        // Periodic health check
        setInterval(checkSystemHealth, 25000);
    }

    function cacheElements() {
        elements = {
            form: document.getElementById("releaseMetricsForm"),
            btnPredict: document.getElementById("btnPredict"),
            btnSpinner: document.getElementById("btnSpinner"),
            btnText: document.getElementById("btnText"),

            // Inputs
            inputChangedFiles: document.getElementById("changedFiles"),
            inputFailedTests: document.getElementById("failedTests"),
            inputCriticalBugs: document.getElementById("criticalBugs"),
            inputCoverage: document.getElementById("coverage"),
            inputFailureRate: document.getElementById("previousFailureRate"),

            // Error displays
            errChangedFiles: document.getElementById("errChangedFiles"),
            errFailedTests: document.getElementById("errFailedTests"),
            errCriticalBugs: document.getElementById("errCriticalBugs"),
            errCoverage: document.getElementById("errCoverage"),
            errFailureRate: document.getElementById("errFailureRate"),

            // Result container
            resultPlaceholder: document.getElementById("resultPlaceholder"),
            resultActive: document.getElementById("resultActive"),
            riskBanner: document.getElementById("riskBanner"),
            riskLevelText: document.getElementById("riskLevelText"),
            confidenceVal: document.getElementById("confidenceVal"),
            recommendationSummary: document.getElementById("recommendationSummary"),
            actionItemsList: document.getElementById("actionItemsList"),
            dbPersistBadge: document.getElementById("dbPersistBadge"),

            // Charts
            canvasGauge: document.getElementById("canvasGauge"),
            canvasMetrics: document.getElementById("canvasMetrics"),

            // KPIs
            kpiTotal: document.getElementById("kpiTotal"),
            kpiLowRate: document.getElementById("kpiLowRate"),
            kpiHighPrevented: document.getElementById("kpiHighPrevented"),
            kpiAvgCoverage: document.getElementById("kpiAvgCoverage"),

            // System status
            statusMl: document.getElementById("statusMl"),
            statusDb: document.getElementById("statusDb"),

            // History
            historyTableBody: document.getElementById("historyTableBody"),
            historySearch: document.getElementById("historySearch"),
            filterBtns: document.querySelectorAll(".filter-btn"),
            btnRefreshHistory: document.getElementById("btnRefreshHistory"),

            // Presets
            presetClean: document.getElementById("presetClean"),
            presetBorderline: document.getElementById("presetBorderline"),
            presetRisky: document.getElementById("presetRisky")
        };
    }

    function bindEvents() {
        // Form submit
        elements.form.addEventListener("submit", handleSubmit);

        // Real-time input validation on change/input
        [
            elements.inputChangedFiles,
            elements.inputFailedTests,
            elements.inputCriticalBugs,
            elements.inputCoverage,
            elements.inputFailureRate
        ].forEach(input => {
            input.addEventListener("input", () => validateSingleInput(input));
        });

        // Presets
        elements.presetClean.addEventListener("click", () => setPreset(18, 0, 0, 92.5, 2.5));
        elements.presetBorderline.addEventListener("click", () => setPreset(95, 2, 0, 68.0, 16.0));
        elements.presetRisky.addEventListener("click", () => setPreset(310, 7, 3, 41.5, 42.0));

        // History filters & refresh
        elements.filterBtns.forEach(btn => {
            btn.addEventListener("click", (e) => {
                elements.filterBtns.forEach(b => b.classList.remove("active"));
                e.target.classList.add("active");
                activeFilter = e.target.dataset.filter;
                renderHistoryTable();
            });
        });

        elements.historySearch.addEventListener("input", renderHistoryTable);
        elements.btnRefreshHistory.addEventListener("click", loadHistoryAndStats);
    }

    function setPreset(changed, failed, bugs, cov, failRate) {
        elements.inputChangedFiles.value = changed;
        elements.inputFailedTests.value = failed;
        elements.inputCriticalBugs.value = bugs;
        elements.inputCoverage.value = cov;
        elements.inputFailureRate.value = failRate;

        // Clear previous validation errors
        clearErrors();
    }

    function clearErrors() {
        [
            elements.errChangedFiles,
            elements.errFailedTests,
            elements.errCriticalBugs,
            elements.errCoverage,
            elements.errFailureRate
        ].forEach(errEl => errEl.textContent = "");

        [
            elements.inputChangedFiles,
            elements.inputFailedTests,
            elements.inputCriticalBugs,
            elements.inputCoverage,
            elements.inputFailureRate
        ].forEach(inp => inp.classList.remove("error"));
    }

    function validateSingleInput(input) {
        let valid = true;
        let msg = "";
        const val = input.value.trim();

        if (val === "") {
            valid = false;
            msg = "Field is required.";
        } else {
            const num = Number(val);
            if (isNaN(num)) {
                valid = false;
                msg = "Must be a valid number.";
            } else if (input.id === "changedFiles" || input.id === "failedTests" || input.id === "criticalBugs") {
                if (num < 0 || !Number.isInteger(num)) {
                    valid = false;
                    msg = "Must be a positive integer.";
                }
            } else if (input.id === "coverage" || input.id === "previousFailureRate") {
                if (num < 0 || num > 100) {
                    valid = false;
                    msg = "Must be between 0.0% and 100.0%.";
                }
            }
        }

        const errorEl = document.getElementById("err" + input.id.charAt(0).toUpperCase() + input.id.slice(1));
        if (errorEl) {
            errorEl.textContent = msg;
        }

        if (valid) {
            input.classList.remove("error");
        } else {
            input.classList.add("error");
        }

        return valid;
    }

    function validateForm() {
        const v1 = validateSingleInput(elements.inputChangedFiles);
        const v2 = validateSingleInput(elements.inputFailedTests);
        const v3 = validateSingleInput(elements.inputCriticalBugs);
        const v4 = validateSingleInput(elements.inputCoverage);
        const v5 = validateSingleInput(elements.inputFailureRate);
        return v1 && v2 && v3 && v4 && v5;
    }

    async function handleSubmit(e) {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        const payload = {
            changedFiles: parseInt(elements.inputChangedFiles.value, 10),
            failedTests: parseInt(elements.inputFailedTests.value, 10),
            criticalBugs: parseInt(elements.inputCriticalBugs.value, 10),
            coverage: parseFloat(elements.inputCoverage.value),
            previousFailureRate: parseFloat(elements.inputFailureRate.value)
        };

        setLoading(true);

        try {
            const response = await fetch(API.PREDICT, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (!response.ok || data.status === "error") {
                throw new Error(data.message || `Server returned status ${response.status}`);
            }

            displayPredictionResult(data.prediction, data.savedToDatabase);
            loadHistoryAndStats(); // Refresh table and stats after saving
        } catch (err) {
            alert("Prediction Request Failed: " + err.message);
        } finally {
            setLoading(false);
        }
    }

    function setLoading(isLoading) {
        elements.btnPredict.disabled = isLoading;
        if (isLoading) {
            elements.btnSpinner.style.display = "inline-block";
            elements.btnText.textContent = "Analyzing Release...";
        } else {
            elements.btnSpinner.style.display = "none";
            elements.btnText.textContent = "Predict Release Risk";
        }
    }

    function displayPredictionResult(pred, savedToDb) {
        elements.resultPlaceholder.style.display = "none";
        elements.resultActive.style.display = "flex";

        const risk = pred.riskLevel || "Medium";
        const riskClass = risk.toLowerCase();

        // Update Risk Banner
        elements.riskBanner.className = `risk-banner ${riskClass}`;
        elements.riskLevelText.textContent = `${risk} Risk`;
        elements.confidenceVal.textContent = `${pred.confidencePercent.toFixed(1)}%`;

        if (savedToDb) {
            elements.dbPersistBadge.innerHTML = `<span style="color: var(--risk-low);">●</span> Stored in Oracle DB (Record #${pred.id || 'new'})`;
        } else {
            elements.dbPersistBadge.innerHTML = `<span style="color: var(--risk-medium);">●</span> Evaluated`;
        }

        // Recommendations
        elements.recommendationSummary.textContent = pred.recommendation;

        elements.actionItemsList.innerHTML = "";
        let actions = [];
        if (risk === "High") {
            if (pred.criticalBugs > 0) actions.push(`Block deployment: Resolve ${pred.criticalBugs} critical defect(s) immediately.`);
            if (pred.failedTests > 0) actions.push(`Fix ${pred.failedTests} broken automated unit/integration test(s).`);
            if (pred.coverage < 60) actions.push(`Test coverage is inadequate (${pred.coverage}%). Minimum 75% coverage mandatory.`);
            actions.push("Cancel automatic deployment triggers and notify engineering lead.");
        } else if (risk === "Medium") {
            actions.push("Deploy to isolated Canary cluster (5% traffic ring) for 30 minutes.");
            actions.push("Verify database rollback procedures and schema migration scripts.");
            if (pred.coverage < 75) actions.push(`Increase test coverage from ${pred.coverage}% to at least 75% in next iteration.`);
            actions.push("Monitor APM latency percentiles and error budgets during rollout.");
        } else {
            actions.push("Quality gates passed. Approve for automated Blue/Green production deployment.");
            actions.push("Standard APM alerting and distributed tracing enabled for post-release monitoring.");
            actions.push("Release notes automatically generated and tagged in Git repository.");
        }

        actions.forEach(act => {
            const li = document.createElement("li");
            li.innerHTML = `
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="9 11 12 14 22 4"></polyline>
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
                </svg>
                <span>${act}</span>
            `;
            elements.actionItemsList.appendChild(li);
        });

        // Draw Visualizations
        drawGaugeChart(pred.confidencePercent, risk);
        drawMetricsFootprintChart(pred);
    }

    function drawGaugeChart(confidence, risk) {
        const canvas = elements.canvasGauge;
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        const w = canvas.width;
        const h = canvas.height;
        ctx.clearRect(0, 0, w, h);

        const centerX = w / 2;
        const centerY = h / 2 + 10;
        const radius = 55;

        // Background Arc
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, Math.PI * 0.8, Math.PI * 2.2, false);
        ctx.strokeStyle = "rgba(255, 255, 255, 0.08)";
        ctx.lineWidth = 10;
        ctx.lineCap = "round";
        ctx.stroke();

        // Value Arc
        const startAngle = Math.PI * 0.8;
        const totalAngle = Math.PI * 1.4;
        const endAngle = startAngle + (totalAngle * (confidence / 100));

        let strokeColor = "#10b981";
        if (risk === "Medium") strokeColor = "#f59e0b";
        if (risk === "High") strokeColor = "#ef4444";

        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, endAngle, false);
        ctx.strokeStyle = strokeColor;
        ctx.lineWidth = 10;
        ctx.lineCap = "round";
        ctx.stroke();

        // Center Text
        ctx.fillStyle = "#f8fafc";
        ctx.font = "bold 18px 'JetBrains Mono', monospace";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(`${confidence.toFixed(1)}%`, centerX, centerY - 2);

        ctx.fillStyle = "#94a3b8";
        ctx.font = "10px sans-serif";
        ctx.fillText("Confidence", centerX, centerY + 16);
    }

    function drawMetricsFootprintChart(pred) {
        const canvas = elements.canvasMetrics;
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        const w = canvas.width;
        const h = canvas.height;
        ctx.clearRect(0, 0, w, h);

        // Normalize metrics into 0-100 risk pressure scores
        const metrics = [
            { label: "Bugs", value: Math.min(pred.criticalBugs * 33.3, 100), color: "#ef4444" },
            { label: "Fails", value: Math.min(pred.failedTests * 15, 100), color: "#f97316" },
            { label: "Cov Deficit", value: Math.max(0, 100 - pred.coverage), color: "#f59e0b" },
            { label: "Hist Fail", value: Math.min(pred.previousFailureRate * 2, 100), color: "#06b6d4" },
            { label: "Scale", value: Math.min(pred.changedFiles / 3, 100), color: "#8b5cf6" }
        ];

        const barWidth = 24;
        const gap = 12;
        const startX = (w - (metrics.length * barWidth + (metrics.length - 1) * gap)) / 2;
        const chartBottom = h - 25;
        const maxBarHeight = h - 45;

        metrics.forEach((m, idx) => {
            const x = startX + idx * (barWidth + gap);
            const barHeight = (m.value / 100) * maxBarHeight;
            const y = chartBottom - barHeight;

            // Background track
            ctx.fillStyle = "rgba(255, 255, 255, 0.05)";
            ctx.fillRect(x, chartBottom - maxBarHeight, barWidth, maxBarHeight);

            // Active bar
            ctx.fillStyle = m.color;
            ctx.fillRect(x, y, barWidth, barHeight);

            // Label
            ctx.fillStyle = "#94a3b8";
            ctx.font = "9px sans-serif";
            ctx.textAlign = "center";
            ctx.fillText(m.label, x + barWidth / 2, h - 8);
        });
    }

    async function loadHistoryAndStats() {
        try {
            const resp = await fetch(API.HISTORY);
            if (!resp.ok) return;

            const data = await resp.json();
            if (data.status === "success") {
                historyData = data.history || [];
                renderHistoryTable();

                if (data.statistics) {
                    renderKPIs(data.statistics);
                }
            }
        } catch (e) {
            console.warn("Unable to fetch history:", e);
        }
    }

    function renderKPIs(stats) {
        const total = stats.totalReleases || 0;
        const low = stats.lowRiskCount || 0;
        const high = stats.highRiskCount || 0;
        const avgCov = stats.avgCoverage || 0;

        elements.kpiTotal.textContent = total;
        elements.kpiLowRate.textContent = total > 0 ? `${Math.round((low / total) * 100)}%` : "0%";
        elements.kpiHighPrevented.textContent = high;
        elements.kpiAvgCoverage.textContent = `${avgCov.toFixed(1)}%`;
    }

    function renderHistoryTable() {
        const tbody = elements.historyTableBody;
        tbody.innerHTML = "";

        const query = elements.historySearch.value.toLowerCase().trim();

        const filtered = historyData.filter(item => {
            const matchesFilter = (activeFilter === "ALL") || (item.riskLevel.toUpperCase() === activeFilter);
            const matchesSearch = query === "" ||
                item.riskLevel.toLowerCase().includes(query) ||
                item.recommendation.toLowerCase().includes(query) ||
                String(item.changedFiles).includes(query);
            return matchesFilter && matchesSearch;
        });

        if (filtered.length === 0) {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td colspan="7" style="text-align: center; color: var(--text-dim); padding: 2rem;">No release predictions match criteria.</td>`;
            tbody.appendChild(tr);
            return;
        }

        filtered.forEach(item => {
            const tr = document.createElement("tr");

            const riskClass = (item.riskLevel || "").toLowerCase();
            const dateStr = item.createdAt ? formatDateTime(item.createdAt) : "Just now";

            tr.innerHTML = `
                <td>#${item.id || '-'}</td>
                <td><span class="badge-risk ${riskClass}">${item.riskLevel}</span></td>
                <td><strong>${item.confidencePercent.toFixed(1)}%</strong></td>
                <td>${item.changedFiles} files · ${item.failedTests} fails · ${item.criticalBugs} bugs</td>
                <td>${item.coverage.toFixed(1)}%</td>
                <td style="color: var(--text-muted); font-size: 0.8rem;">${dateStr}</td>
                <td>
                    <button class="btn-sm-action" title="Load metrics into form">Load</button>
                </td>
            `;

            tr.querySelector(".btn-sm-action").addEventListener("click", () => {
                setPreset(item.changedFiles, item.failedTests, item.criticalBugs, item.coverage, item.previousFailureRate);
                window.scrollTo({ top: 0, behavior: "smooth" });
            });

            tbody.appendChild(tr);
        });
    }

    function formatDateTime(val) {
        try {
            const d = new Date(val);
            if (isNaN(d.getTime())) return String(val);
            return d.toLocaleDateString() + " " + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            return String(val);
        }
    }

    async function checkSystemHealth() {
        try {
            const resp = await fetch(API.HEALTH);
            if (resp.ok) {
                const data = await resp.json();

                // ML Service Indicator
                if (data.ml_service && data.ml_service.status === "REACHABLE") {
                    elements.statusMl.innerHTML = `<span class="status-dot online"></span> ML Service: Connected`;
                } else {
                    elements.statusMl.innerHTML = `<span class="status-dot warning"></span> ML: Fallback Mode`;
                }

                // DB Indicator
                if (data.database && data.database.status === "CONNECTED") {
                    const isOracle = data.database.driver.includes("Oracle");
                    elements.statusDb.innerHTML = `<span class="status-dot online"></span> DB: ${isOracle ? 'Oracle Active' : 'Oracle Mode (Local)'}`;
                } else {
                    elements.statusDb.innerHTML = `<span class="status-dot offline"></span> DB: Offline`;
                }
            }
        } catch (e) {
            elements.statusMl.innerHTML = `<span class="status-dot warning"></span> ML: Offline`;
            elements.statusDb.innerHTML = `<span class="status-dot warning"></span> DB: Offline`;
        }
    }

    return {
        init
    };
})();
