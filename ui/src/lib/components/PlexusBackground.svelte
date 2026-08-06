<script>
    import { onMount, onDestroy } from 'svelte';

    let canvas;
    let ctx;
    let animFrame;
    let particles = [];
    const MAX_PARTICLES = 70;
    const MAX_DIST = 120;

    onMount(() => {
        if (!canvas) return;
        ctx = canvas.getContext('2d');
        resize();

        particles = [];
        for (let i = 0; i < MAX_PARTICLES; i++) {
            particles.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                vx: (Math.random() - 0.5) * 0.7,
                vy: (Math.random() - 0.5) * 0.7,
                radius: Math.random() * 1.8 + 1.2
            });
        }

        window.addEventListener('resize', resize);
        loop();
    });

    onDestroy(() => {
        if (typeof window !== 'undefined') {
            window.removeEventListener('resize', resize);
        }
        if (animFrame) cancelAnimationFrame(animFrame);
    });

    function resize() {
        if (!canvas) return;
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    function loop() {
        if (!ctx || !canvas) return;
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Update positions
        for (let p of particles) {
            p.x += p.vx;
            p.y += p.vy;

            if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
            if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
        }

        // Draw connecting lines with sub-pixel anti-aliased vectors
        ctx.lineWidth = 0.8;
        for (let i = 0; i < particles.length; i++) {
            let p1 = particles[i];
            for (let j = i + 1; j < particles.length; j++) {
                let p2 = particles[j];
                let dx = p1.x - p2.x;
                let dy = p1.y - p2.y;
                let dist = Math.hypot(dx, dy);

                if (dist < MAX_DIST) {
                    let alpha = (1.0 - dist / MAX_DIST) * 0.35;
                    ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`;
                    ctx.beginPath();
                    ctx.moveTo(p1.x, p1.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.stroke();
                }
            }
        }

        // Draw smooth radial glowing dots
        for (let p of particles) {
            let grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.radius * 3);
            grad.addColorStop(0, 'rgba(186, 230, 253, 0.9)');
            grad.addColorStop(0.4, 'rgba(56, 189, 248, 0.4)');
            grad.addColorStop(1, 'rgba(14, 165, 233, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.radius * 3, 0, Math.PI * 2);
            ctx.fill();
        }

        animFrame = requestAnimationFrame(loop);
    }
</script>

<canvas bind:this={canvas} class="absolute inset-0 pointer-events-none z-0 w-full h-full"></canvas>
