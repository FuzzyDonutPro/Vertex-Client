<script>
    export let isRunning = false;
    export let activeMacro = 'Idle';
    export let runtime = '00:00:00';
    export let profit = '0';
    export let profitRate = '0/hr';
    export let failsafe = false;
    export let objective = 'Mining Commissions';
    export let statLabel = 'SESSION TIME';
    export let statVal = '00:00:00';
    export let themeGradientClass = 'from-sky-400 to-sky-600';
</script>

<div class="fixed top-5 left-5 z-50 pointer-events-none select-none transition-all duration-500 opacity-100 scale-100">
    <div class="bg-slate-950/80 backdrop-blur-md border border-[var(--theme-400)] rounded-xl p-4 w-[260px] shadow-2xl font-sans flex flex-col relative overflow-hidden">
        
        <!-- Top Section: Icon & Macro Name/State -->
        <div class="flex items-center gap-3 mb-3.5">
            <div class="w-10 h-10 rounded-lg bg-slate-800/90 border border-white/10 flex items-center justify-center shadow-inner shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-sky-400">
                    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
            </div>
            <div class="flex flex-col min-w-0">
                <span class="text-[13px] font-bold text-[var(--theme-400)] tracking-wide truncate">{activeMacro}</span>
                <span class="text-[10px] text-slate-300 font-medium">{isRunning ? 'Running smoothly' : (failsafe ? 'Failsafe triggered' : 'Idle')}</span>
            </div>
        </div>

        <!-- Big Stat 1: Profit Rate -->
        <div class="mb-3.5">
            <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest block mb-0.5">PROFIT / HR</span>
            <div class="text-[26px] font-extrabold text-white leading-none tracking-tight">{profitRate.replace('/hr', '')}</div>
        </div>

        <!-- Current Objective -->
        <div class="mb-3.5 bg-slate-900/60 p-2.5 rounded-lg border border-white/5">
            <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest block mb-0.5">CURRENT OBJECTIVE</span>
            <div class="text-[11px] font-semibold text-white leading-snug truncate">{objective || (activeMacro === 'Idle' ? 'None' : activeMacro)}</div>
        </div>

        <!-- Stats Grid -->
        <div class="grid grid-cols-2 gap-y-3.5 mb-3.5">
            <div class="flex flex-col">
                <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">TOTAL PROFIT</span>
                <span class="text-[12px] font-bold text-white mt-0.5">{profit}</span>
            </div>
            <div class="flex flex-col text-right">
                <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">SESSION TIME</span>
                <span class="text-[12px] font-bold text-white mt-0.5">{runtime}</span>
            </div>
            <div class="flex flex-col">
                <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">STATUS</span>
                <span class="text-[12px] font-bold mt-0.5 {failsafe ? 'text-red-400' : (isRunning ? 'text-emerald-400' : 'text-slate-300')}">
                    {failsafe ? 'FAILSAFE' : (isRunning ? 'ACTIVE' : 'IDLE')}
                </span>
            </div>
            <div class="flex flex-col text-right">
                <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{statLabel}</span>
                <span class="text-[12px] font-bold text-sky-400 mt-0.5 truncate">{statVal}</span>
            </div>
        </div>
    </div>
</div>
