<script>
    export let isRunning = false;
    export let runtime = '00:00:00';
    export let target = 'Searching...';
    export let speed = 2000;
    export let themeColorClass = 'border-sky-400';
    export let themeGradientClass = 'from-sky-400 to-sky-600';
</script>

{#if isRunning}
<div class="fixed top-4 left-1/2 -translate-x-1/2 flex items-center justify-center pointer-events-none select-none z-50 transition-all duration-500 opacity-100 scale-100">
    <div class="bg-slate-950/25 backdrop-blur-xl border-2 border-[var(--theme-400)] rounded-2xl shadow-[0_0_20px_rgba(56,189,248,0.35)] flex items-center gap-4 px-5 py-2.5 min-w-[280px]">
        
        <div class="w-8 h-8 rounded-full bg-gradient-to-br {themeGradientClass} flex items-center justify-center shrink-0 shadow-[0_0_15px_rgba(56,189,248,0.4)] animate-pulse">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
        </div>

        <div class="flex-1 flex flex-col justify-center">
            <div class="flex items-center justify-between">
                <span class="text-[10px] text-sky-400 font-bold uppercase tracking-wider">Target</span>
                <span class="text-[11px] text-white font-semibold truncate max-w-[100px]">{target}</span>
            </div>
            
            <div class="flex items-center justify-between mt-1 pt-1 border-t border-white/10">
                <span class="text-[9px] text-slate-400">Speed</span>
                <span class="text-[10px] text-slate-200 font-mono">{speed}ms</span>
            </div>
            
            <div class="flex items-center justify-between mt-0.5">
                <span class="text-[9px] text-slate-400">Uptime</span>
                <span class="text-[10px] text-slate-200 font-mono">{runtime}</span>
            </div>
        </div>
    </div>
</div>
{/if}
