<script>
    export let isRunning = false;
    export let activeMacro = 'Idle';
    export let runtime = '00:00:00';
    export let profit = '+0';
    export let profitRate = '0/hr';
    export let failsafe = false;
    export let themeGradientClass = 'from-sky-400 to-sky-600';
</script>

<div class="fixed top-5 left-5 z-50 pointer-events-none select-none transition-all duration-500 opacity-100 scale-100">
    <!-- Glassmorphic Container -->
    <div class="bg-slate-950/35 backdrop-blur-2xl border-2 border-[var(--theme-400)] rounded-3xl p-5 min-w-[300px] shadow-[0_0_35px_rgba(56,189,248,0.35),0_12px_40px_rgba(0,0,0,0.7)] flex flex-col gap-3.5 relative overflow-hidden group">
        
        <!-- Top Glass Reflection Sheen -->
        <div class="absolute top-0 left-5 right-5 h-[1px] bg-gradient-to-r from-transparent via-white/35 to-transparent"></div>

        <!-- Header -->
        <div class="flex items-center justify-between pb-2.5 border-b border-white/10">
            <div>
                <h3 class="font-outfit text-[14px] font-extrabold tracking-widest text-white uppercase leading-none">
                    VERTEX <span class="text-sky-400">STATUS</span>
                </h3>
                <span class="text-[9px] text-slate-400 tracking-widest uppercase block mt-1 font-mono">v1.0.6 • Live Engine</span>
            </div>

            <!-- Status Indicator Pulse -->
            <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[9px] font-bold uppercase tracking-wider border {failsafe ? 'bg-red-500/20 border-red-500/50 text-red-400 shadow-[0_0_12px_rgba(239,68,68,0.4)]' : (isRunning ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-400 shadow-[0_0_12px_rgba(16,185,129,0.4)]' : 'bg-slate-800/60 border-white/10 text-slate-400')}">
                {#if failsafe}
                    <span class="w-2 h-2 rounded-full bg-red-400 animate-ping"></span>
                    <span>FAILSAFE</span>
                {:else if isRunning}
                    <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                    <span>RUNNING</span>
                {:else}
                    <span class="w-2 h-2 rounded-full bg-slate-500"></span>
                    <span>IDLE</span>
                {/if}
            </div>
        </div>

        <!-- Metrics Body -->
        <div class="flex flex-col gap-2.5">
            <!-- Active State -->
            <div class="flex items-center justify-between bg-slate-900/50 border border-white/5 px-3.5 py-2.5 rounded-2xl">
                <span class="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Active Macro</span>
                <span class="text-[11px] font-bold truncate max-w-[170px] {isRunning ? 'text-emerald-400' : 'text-slate-300'}">
                    {activeMacro}
                </span>
            </div>

            <!-- Session Uptime -->
            <div class="flex items-center justify-between bg-slate-900/50 border border-white/5 px-3.5 py-2.5 rounded-2xl">
                <span class="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Uptime</span>
                <span class="text-[11px] font-mono font-bold text-white tracking-wide">
                    {runtime}
                </span>
            </div>

            <!-- Est. Profit -->
            <div class="flex items-center justify-between bg-slate-900/50 border border-white/5 px-3.5 py-2.5 rounded-2xl">
                <span class="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Session Profit</span>
                <div class="flex items-center gap-1.5 font-mono text-[11px]">
                    <span class="font-bold text-emerald-400">{profit}</span>
                    <span class="text-[9px] text-slate-400">({profitRate})</span>
                </div>
            </div>

            <!-- Security & Anti-Ban -->
            <div class="flex items-center justify-between bg-slate-900/50 border border-white/5 px-3.5 py-2.5 rounded-2xl">
                <span class="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Anti-Ban</span>
                <span class="text-[10px] font-bold text-sky-400 flex items-center gap-1">
                    <span class="w-1.5 h-1.5 rounded-full bg-sky-400"></span> Active
                </span>
            </div>
        </div>
    </div>
</div>
