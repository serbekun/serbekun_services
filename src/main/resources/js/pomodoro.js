(function () {
    const STORAGE_KEY = "serbekun.pomodoro";
    const DEFAULTS = {
        focus: 25,
        short: 5,
        long: 15,
        round: 4
    };

    const state = {
        mode: "focus",
        running: false,
        remaining: DEFAULTS.focus * 60,
        total: DEFAULTS.focus * 60,
        completedFocus: 0,
        intervalId: null,
        settings: loadSettings()
    };

    const modes = {
        focus: { label: "Focus", input: "focus-minutes" },
        short: { label: "Short break", input: "short-minutes" },
        long: { label: "Long break", input: "long-minutes" }
    };

    const els = {
        tabs: Array.from(document.querySelectorAll(".mode-tab")),
        start: document.getElementById("start-btn"),
        reset: document.getElementById("reset-btn"),
        skip: document.getElementById("skip-btn"),
        time: document.getElementById("time-display"),
        mode: document.getElementById("mode-label"),
        sessions: document.getElementById("session-line"),
        status: document.getElementById("status-line"),
        ring: document.getElementById("timer-ring"),
        progress: document.getElementById("ring-progress"),
        focus: document.getElementById("focus-minutes"),
        short: document.getElementById("short-minutes"),
        long: document.getElementById("long-minutes"),
        round: document.getElementById("round-size")
    };

    function loadSettings() {
        try {
            const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
            return {
                focus: clamp(saved && saved.focus, 1, 180, DEFAULTS.focus),
                short: clamp(saved && saved.short, 1, 60, DEFAULTS.short),
                long: clamp(saved && saved.long, 1, 90, DEFAULTS.long),
                round: clamp(saved && saved.round, 1, 12, DEFAULTS.round)
            };
        } catch (e) {
            return { ...DEFAULTS };
        }
    }

    function saveSettings() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(state.settings));
        } catch (e) {}
    }

    function clamp(value, min, max, fallback) {
        const number = Number.parseInt(value, 10);
        if (!Number.isFinite(number)) return fallback;
        return Math.min(max, Math.max(min, number));
    }

    function secondsFor(mode) {
        return state.settings[mode] * 60;
    }

    function setMode(mode, keepStatus) {
        stopTimer();
        state.mode = mode;
        state.total = secondsFor(mode);
        state.remaining = state.total;
        if (!keepStatus) setStatus("ready");
        render();
    }

    function startTimer() {
        if (state.running) {
            stopTimer();
            setStatus("paused");
            render();
            return;
        }

        state.running = true;
        state.intervalId = window.setInterval(tick, 1000);
        setStatus("running");
        render();
    }

    function stopTimer() {
        state.running = false;
        if (state.intervalId) {
            window.clearInterval(state.intervalId);
            state.intervalId = null;
        }
    }

    function tick() {
        state.remaining -= 1;
        if (state.remaining <= 0) {
            completeMode();
            return;
        }
        render();
    }

    function completeMode() {
        const finishedMode = state.mode;
        stopTimer();

        if (finishedMode === "focus") {
            state.completedFocus += 1;
        }

        const nextMode = nextModeAfter(finishedMode);
        setMode(nextMode, true);
        setStatus(finishedMode === "focus" ? "break time" : "ready for focus");
        beep();
    }

    function nextModeAfter(mode) {
        if (mode !== "focus") return "focus";
        return state.completedFocus > 0 && state.completedFocus % state.settings.round === 0
            ? "long"
            : "short";
    }

    function resetTimer() {
        setMode(state.mode);
    }

    function skipTimer() {
        completeMode();
    }

    function updateSettings() {
        state.settings.focus = clamp(els.focus.value, 1, 180, DEFAULTS.focus);
        state.settings.short = clamp(els.short.value, 1, 60, DEFAULTS.short);
        state.settings.long = clamp(els.long.value, 1, 90, DEFAULTS.long);
        state.settings.round = clamp(els.round.value, 1, 12, DEFAULTS.round);
        saveSettings();
        writeSettings();

        if (!state.running) {
            state.total = secondsFor(state.mode);
            state.remaining = state.total;
            render();
        }
    }

    function writeSettings() {
        els.focus.value = state.settings.focus;
        els.short.value = state.settings.short;
        els.long.value = state.settings.long;
        els.round.value = state.settings.round;
    }

    function setStatus(text) {
        els.status.textContent = text;
        els.status.classList.toggle("ok", text === "running");
    }

    function render() {
        const minutes = Math.floor(state.remaining / 60);
        const seconds = state.remaining % 60;
        const progress = state.total === 0 ? 1 : state.remaining / state.total;
        const circumference = 2 * Math.PI * 96;

        els.time.textContent = String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
        els.mode.textContent = modes[state.mode].label;
        els.sessions.textContent = state.completedFocus + (state.completedFocus === 1 ? " session" : " sessions");
        els.progress.style.strokeDashoffset = String(circumference * (1 - progress));
        els.ring.classList.toggle("running", state.running);

        els.tabs.forEach((tab) => {
            const active = tab.dataset.mode === state.mode;
            tab.classList.toggle("active", active);
            tab.setAttribute("aria-selected", String(active));
        });

        els.start.querySelector("span").textContent = state.running ? "Pause" : "Start";
        document.title = els.time.textContent + " Pomodoro";
    }

    function beep() {
        if (!window.AudioContext && !window.webkitAudioContext) return;
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        const audio = new AudioContext();
        const oscillator = audio.createOscillator();
        const gain = audio.createGain();

        oscillator.type = "sine";
        oscillator.frequency.value = 880;
        gain.gain.setValueAtTime(0.0001, audio.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.12, audio.currentTime + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, audio.currentTime + 0.28);
        oscillator.connect(gain);
        gain.connect(audio.destination);
        oscillator.start();
        oscillator.stop(audio.currentTime + 0.3);
    }

    els.tabs.forEach((tab) => {
        tab.addEventListener("click", () => setMode(tab.dataset.mode));
    });
    els.start.addEventListener("click", startTimer);
    els.reset.addEventListener("click", resetTimer);
    els.skip.addEventListener("click", skipTimer);
    [els.focus, els.short, els.long, els.round].forEach((input) => {
        input.addEventListener("change", updateSettings);
    });

    writeSettings();
    setMode("focus");
})();
