document.addEventListener('DOMContentLoaded', () => {
    // 1. Navigation Tab Switching in Simulator
    const navButtons = document.querySelectorAll('.sim-nav-btn');
    const tabContents = document.querySelectorAll('.sim-tab-content');
    const hudTargetVal = document.getElementById('hud-target-val');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            navButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const targetTab = btn.getAttribute('data-tab');
            const targetContent = document.getElementById(`tab-${targetTab}`);
            if (targetContent) {
                targetContent.classList.add('active');
            }

            // Update HUD target preview based on active tab
            if (targetTab === 'combat') {
                updateSlayerHudPreview();
            } else if (targetTab === 'mining') {
                hudTargetVal.textContent = 'Mithril Ore (Dwarven Mines)';
            } else if (targetTab === 'foraging') {
                hudTargetVal.textContent = 'Dark Oak Tree Cluster (5-50 Logs)';
            } else if (targetTab === 'farming') {
                hudTargetVal.textContent = 'Nether Wart Plot (S-Shape Lane)';
            } else if (targetTab === 'failsafes') {
                hudTargetVal.textContent = 'Sentinel Anti-Staff Engine';
            }
        });
    });

    // 2. Slayer Boss & Tier Selector Handlers
    const bossSelect = document.getElementById('slayer-boss-select');
    const tierSelect = document.getElementById('slayer-tier-select');

    function updateSlayerHudPreview() {
        if (!bossSelect || !tierSelect || !hudTargetVal) return;
        const bossName = bossSelect.options[bossSelect.selectedIndex].text;
        const tierName = tierSelect.options[tierSelect.selectedIndex].text.split(' ')[0] + ' ' + tierSelect.options[tierSelect.selectedIndex].text.split(' ')[1];
        hudTargetVal.textContent = `${bossName} (${tierName})`;
    }

    if (bossSelect) bossSelect.addEventListener('change', updateSlayerHudPreview);
    if (tierSelect) tierSelect.addEventListener('change', updateSlayerHudPreview);

    // 3. Macro Start/Stop Toggle Handlers & HUD Clock
    const toggleButtons = document.querySelectorAll('.sim-toggle-btn');
    const hudStatusText = document.getElementById('hud-status-text');
    const hudTimer = document.getElementById('hud-timer');
    let activeMacroTimer = null;
    let timerSeconds = 0;

    function formatTime(totalSeconds) {
        const hrs = String(Math.floor(totalSeconds / 3600)).padStart(2, '0');
        const mins = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0');
        const secs = String(totalSeconds % 60).padStart(2, '0');
        return `${hrs}:${mins}:${secs}`;
    }

    toggleButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const isCurrentlyRunning = btn.classList.contains('running');

            // Reset all other toggle buttons
            toggleButtons.forEach(b => {
                b.classList.remove('running');
                b.textContent = 'START MACRO';
            });

            if (!isCurrentlyRunning) {
                btn.classList.add('running');
                btn.textContent = 'STOP MACRO';
                hudStatusText.textContent = 'MACRO RUNNING — ACTIVE';
                hudStatusText.style.color = '#10b981';

                // Start timer
                clearInterval(activeMacroTimer);
                timerSeconds = 0;
                hudTimer.textContent = '00:00:00';
                activeMacroTimer = setInterval(() => {
                    timerSeconds++;
                    hudTimer.textContent = formatTime(timerSeconds);
                }, 1000);
            } else {
                hudStatusText.textContent = 'MACRO READY — IDLE';
                hudStatusText.style.color = '#0ea5e9';
                clearInterval(activeMacroTimer);
            }
        });
    });

    // 4. Heal Slider Sync
    const healSlider = document.getElementById('heal-slider');
    const healVal = document.getElementById('heal-val');
    if (healSlider && healVal) {
        healSlider.addEventListener('input', (e) => {
            healVal.textContent = `${e.target.value}%`;
        });
    }

    // 5. Interactive Bezier Curve Canvas Lab
    const canvas = document.getElementById('bezier-canvas');
    if (canvas) {
        const ctx = canvas.getContext('2d');
        const curveFactorSlider = document.getElementById('curve-factor');
        const jitterFactorSlider = document.getElementById('jitter-factor');
        const replayBtn = document.getElementById('btn-replay-curve');

        let startPoint = { x: 80, y: 260 };
        let endPoint = { x: 500, y: 80 };
        let animProgress = 0;
        let animFrameId = null;

        function resizeCanvas() {
            const rect = canvas.getBoundingClientRect();
            canvas.width = rect.width * window.devicePixelRatio;
            canvas.height = (rect.width * 0.55) * window.devicePixelRatio;
            ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
            drawFrame();
        }

        function getControlPoints(p0, p3, curvature) {
            const dx = p3.x - p0.x;
            const dy = p3.y - p0.y;
            const curveOffset = (curvature - 5) * 20;

            const p1 = {
                x: p0.x + dx * 0.35 - curveOffset * 0.5,
                y: p0.y + dy * 0.15 - Math.abs(curveOffset)
            };
            const p2 = {
                x: p0.x + dx * 0.75 + curveOffset * 0.3,
                y: p0.y + dy * 0.85 - curveOffset * 0.5
            };
            return { p1, p2 };
        }

        function cubicBezier(p0, p1, p2, p3, t) {
            const u = 1 - t;
            const tt = t * t;
            const uu = u * u;
            const uuu = uu * u;
            const ttt = tt * t;

            return {
                x: uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x,
                y: uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
            };
        }

        function drawFrame() {
            const width = canvas.width / window.devicePixelRatio;
            const height = canvas.height / window.devicePixelRatio;

            ctx.clearRect(0, 0, width, height);

            const curvature = parseFloat(curveFactorSlider ? curveFactorSlider.value : 5);
            const { p1, p2 } = getControlPoints(startPoint, endPoint, curvature);

            // 1. Grid Background Lines
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.04)';
            ctx.lineWidth = 1;
            for (let x = 0; x < width; x += 30) {
                ctx.beginPath();
                ctx.moveTo(x, 0);
                ctx.lineTo(x, height);
                ctx.stroke();
            }
            for (let y = 0; y < height; y += 30) {
                ctx.beginPath();
                ctx.moveTo(0, y);
                ctx.lineTo(width, y);
                ctx.stroke();
            }

            // 2. Draw Linear Detected Path (Red Dashed)
            ctx.strokeStyle = 'rgba(239, 68, 68, 0.6)';
            ctx.lineWidth = 2;
            ctx.setLineDash([5, 5]);
            ctx.beginPath();
            ctx.moveTo(startPoint.x, startPoint.y);
            ctx.lineTo(endPoint.x, endPoint.y);
            ctx.stroke();
            ctx.setLineDash([]);

            // 3. Draw Vertex Bezier Path (Cyan Solid)
            ctx.strokeStyle = '#38bdf8';
            ctx.lineWidth = 3;
            ctx.beginPath();
            ctx.moveTo(startPoint.x, startPoint.y);
            ctx.bezierCurveTo(p1.x, p1.y, p2.x, p2.y, endPoint.x, endPoint.y);
            ctx.stroke();

            // 4. Draw Start and Target Crosshairs
            drawCrosshair(startPoint.x, startPoint.y, 'Start Point (Player Crosshair)', '#94a3b8');
            drawCrosshair(endPoint.x, endPoint.y, 'Target (Mob / Ore Hitbox)', '#38bdf8');

            // 5. Draw Animated Cursor Particle
            if (animProgress <= 1) {
                const currentPos = cubicBezier(startPoint, p1, p2, endPoint, animProgress);
                
                // Glow
                ctx.shadowColor = '#38bdf8';
                ctx.shadowBlur = 15;
                ctx.fillStyle = '#ffffff';
                ctx.beginPath();
                ctx.arc(currentPos.x, currentPos.y, 6, 0, Math.PI * 2);
                ctx.fill();
                ctx.shadowBlur = 0;

                // Trail particle
                ctx.fillStyle = 'rgba(56, 189, 248, 0.4)';
                ctx.beginPath();
                ctx.arc(currentPos.x, currentPos.y, 12, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        function drawCrosshair(x, y, label, color) {
            ctx.strokeStyle = color;
            ctx.fillStyle = color;
            ctx.lineWidth = 1.5;

            ctx.beginPath();
            ctx.arc(x, y, 8, 0, Math.PI * 2);
            ctx.stroke();

            ctx.beginPath();
            ctx.moveTo(x - 12, y);
            ctx.lineTo(x + 12, y);
            ctx.moveTo(x, y - 12);
            ctx.lineTo(x, y + 12);
            ctx.stroke();

            ctx.font = '10px "JetBrains Mono", monospace';
            ctx.fillText(label, x + 12, y - 10);
        }

        function startAnimation() {
            animProgress = 0;
            if (animFrameId) cancelAnimationFrame(animFrameId);

            function step() {
                animProgress += 0.015;
                drawFrame();
                if (animProgress < 1.0) {
                    animFrameId = requestAnimationFrame(step);
                } else {
                    animProgress = 1.0;
                    drawFrame();
                }
            }
            step();
        }

        canvas.addEventListener('click', (e) => {
            const rect = canvas.getBoundingClientRect();
            const scaleX = (canvas.width / window.devicePixelRatio) / rect.width;
            const scaleY = (canvas.height / window.devicePixelRatio) / rect.height;

            endPoint = {
                x: (e.clientX - rect.left) * scaleX,
                y: (e.clientY - rect.top) * scaleY
            };
            startAnimation();
        });

        if (curveFactorSlider) curveFactorSlider.addEventListener('input', () => drawFrame());
        if (jitterFactorSlider) jitterFactorSlider.addEventListener('input', () => drawFrame());
        if (replayBtn) replayBtn.addEventListener('click', startAnimation);

        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();
        startAnimation();
    }
});
