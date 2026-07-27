<script>
    import { onMount } from 'svelte';
    import './app.css';

    let currentTab = 'farming';
    let searchQuery = '';
    let playerName = 'Player';
    let isConnected = true;
    let activeMacroName = 'None';
    let farmingSpeed = '0.0 BPS';
    let estProfit = '0 / hr';
    let liveFps = 0;
    let selectedFont = 'Outfit';

    let activeMacroSettingsModal = null;
    let dynamicConfigSchema = null;

    // Map Svelte macro categories to Java config category IDs
    const categoryMapping = {
        'farming': ['farming', 'melonPumpkin', 'farmBuilder'],
        'mining': ['miningMacro', 'powderMacro', 'routeMiner', 'commission'],
        'fishing': ['fishing'],
        'slayer': ['combat'],
        'foraging': ['foraging']
    };

    let macros = [
        // Farming & Garden
        { id: 'crop', category: 'farming', title: 'Crop/Wart S-Shape Macro', desc: 'Auto 45° S-Shape lane traversal for Wheat, Carrot, Potato & Nether Wart.', running: false, target: 'Wheat', options: ['Wheat', 'Carrot', 'Potato', 'Nether Wart'] },
        { id: 'sugarcane', category: 'farming', title: 'Sugarcane & Cactus Straight Macro', desc: 'Straight lane traversal for Sugarcane & Cactus plots.', running: false, target: 'Sugar Cane', options: ['Sugar Cane', 'Cactus'] },
        { id: 'melon_pumpkin', category: 'farming', title: 'Melon & Pumpkin Macro', desc: 'Auto lane traversal specifically tuned for Melon & Pumpkin farms.', running: false, target: 'Melon', options: ['Melon', 'Pumpkin'] },
        { id: 'farm_builder', category: 'farming', title: 'Garden Plot Auto Farm Builder', desc: 'Auto-builds preset infinite farm layouts across Garden plots.', running: false, target: 'Preset S-Shape' },
        { id: 'visitor', category: 'farming', title: 'Garden Visitor Trader', desc: 'Fulfills NPC visitor crop trades for Copper & EXP.', running: false, target: 'Auto Trade' },
        { id: 'pest_hunter', category: 'farming', title: 'Pest Hunter & Vacuum', desc: 'Auto-detects and vacuoms Garden plot pests and trades with Pest Broker.', running: false, target: 'Vacuum + Trade' },

        // Mining & Commissions
        { id: 'commission', category: 'mining', title: 'Commission Auto-Miner', desc: 'Dwarven Mines & Crystal Hollows commission route solver with Etherwarp.', running: false, target: 'Dwarven Mines', options: ['Dwarven Mines', 'Crystal Hollows', 'Glacite Mineshafts'] },
        { id: 'gemstone', category: 'mining', title: 'Gemstone Etherwarp Route Miner', desc: 'Follows custom JSON waypoint routes with 0-tick Etherwarp & Pickaxe swap.', running: false, target: 'Ruby Route #1', options: ['Ruby Route #1', 'Jasper Route #1', 'Sapphire Route #2', 'Topaz Magma Fields'] },
        { id: 'mining_general', category: 'mining', title: 'Mithril & Ore Miner', desc: 'Auto-mines Mithril & Titanium ores with smooth head rotation.', running: false, target: 'Titanium & Mithril' },
        { id: 'powder', category: 'mining', title: 'Chest & Mithril Powder Miner', desc: 'Uncovers and loots buried treasure chests in Crystal Hollows for Powder.', running: false, target: 'Chest Solver + Mining' },
        { id: 'glacial', category: 'mining', title: 'Glacial Cave Ice/Mithril Miner', desc: 'Auto-mines Glacite & Glacial Ice in Mineshafts with pathfinder.', running: false, target: 'Glacial Ice' },
        { id: 'nuker', category: 'mining', title: 'Custom Block & Ore Nuker', desc: 'High-speed block nuker with range & FOV filters.', running: false, target: 'Mithril Ores' },

        // Slayer & Combat
        { id: 'slayer', category: 'slayer', title: 'Slayer Boss & Mob Killer', desc: 'Auto-spawns and slays Slayer bosses (Revenant, Tarantula, Sven, Voidgloom).', running: false, target: 'Revenant Horror', options: ['Revenant Horror', 'Tarantula Broodfather', 'Sven Packmaster', 'Voidgloom Seraph'] },
        { id: 'zealot', category: 'slayer', title: 'End Zealot & Bruiser Farmer', desc: 'Auto-pathfinds and kills Zealots & Special Zealot Bruisers in The End.', running: false, target: 'Special Zealots' },
        { id: 'dungeon', category: 'slayer', title: 'Dungeons & Catacombs Solver', desc: 'Auto room clear, secret finder & boss room combat helper.', running: false, target: 'Catacombs Floor 7' },

        // Fishing
        { id: 'fishing', category: 'fishing', title: 'Lava & Water Auto-Fisher', desc: 'Auto-casts rod, detects bobber hook animation, and slays sea creatures.', running: false, target: 'Crimson Isle Lava', options: ['Crimson Isle Lava', 'Main Hub Water'] },
        { id: 'trophy_fishing', category: 'fishing', title: 'Trophy Fishing Solver', desc: 'Optimized trophy fish hook timing with auto-slugfish & Obfuscated Fish filleting.', running: false, target: 'Trophy Fish Auto-Catch' },

        // Foraging
        { id: 'foraging', category: 'foraging', title: 'Park & Jungle Wood Foraging', desc: 'Auto-cuts Dark Oak, Acacia & Jungle trees with Treecapitator auto-swap.', running: false, target: 'Dark Oak' },

        // Economy & Alchemy
        { id: 'alchemy', category: 'alchemy', title: 'Auto Alchemy Potion Brewer', desc: 'Auto-brews Speed / EXP potions via Nether Wart & Enchanted Sugarcane.', running: false, target: 'Speed Potion', options: ['Speed Potion', 'EXP Potion', 'Alchemy 50 Batch'] },
        { id: 'flip', category: 'alchemy', title: 'Bazaar & AH Order Flipper', desc: 'Auto-monitors profit margins and posts buy/sell orders on Bazaar.', running: false, target: 'Bazaar Auto-Flip' },

        // Diana Mythological
        { id: 'diana', category: 'diana', title: 'Diana Mythological Burrow Finder', desc: 'Ancestral spade particle trail pathfinding, digging, and Mythological mob killer.', running: false, target: 'Daedalus Axe', options: ['Daedalus Axe', 'Ancestral Spade Only', 'Inquisitor Hunter'] }
    ];

    onMount(() => {
        fetchStatus();
        const interval = setInterval(fetchStatus, 2000);
        
        if (window.cefQuery) {
            window.cefQuery({
                request: "get_config_schema",
                onSuccess: function(response) {
                    try {
                        dynamicConfigSchema = JSON.parse(response);
                    } catch(e) {}
                },
                onFailure: function() {}
            });
        } else {
            fetch('/api/config/schema')
                .then(res => res.json())
                .then(data => {
                    dynamicConfigSchema = data;
                })
                .catch(e => console.error(e));
        }
        
        return () => clearInterval(interval);
    });

    function updateConfigValue(categoryId, fieldId, value) {
        if (window.cefQuery) {
            window.cefQuery({
                request: `update_config:${categoryId}:${fieldId}:${value}`,
                onSuccess: function() {},
                onFailure: function() {}
            });
        } else {
            fetch(`/api/config/update?categoryId=${categoryId}&fieldId=${fieldId}&value=${encodeURIComponent(value)}`);
        }
    }

    async function fetchStatus() {
        try {
            const res = await fetch('/api/status');
            if (res.ok) {
                const data = await res.json();
                if (data.playerName && data.playerName !== 'Player') {
                    playerName = data.playerName;
                }
                if (data.activeMacro) {
                    activeMacroName = data.activeMacro;
                }
                if (data.bps) {
                    farmingSpeed = data.bps;
                }
                if (data.estProfit) {
                    estProfit = data.estProfit;
                }
                if (data.fps) {
                    liveFps = data.fps;
                }
            }
        } catch (e) {}
    }

    function toggleMacro(id) {
        let macro = macros.find(m => m.id === id);
        if (macro) {
            macro.running = !macro.running;
            
            // If turning on, turn off others
            if (macro.running) {
                macros.forEach(m => { if (m.id !== id) m.running = false; });
                activeMacroName = macro.title;
                farmingSpeed = '20.0 BPS';
                estProfit = 'Calculating...';
            } else {
                activeMacroName = 'None';
                farmingSpeed = '0.0 BPS';
                estProfit = '0 / hr';
            }

            macros = [...macros]; // trigger reactivity
            
            // IPC Bridge: Send state back to Java MCEF
            if (window.cefQuery) {
                window.cefQuery({
                    request: `toggle_macro:${id}:${macro.running}`,
                    onSuccess: function(response) {},
                    onFailure: function(error_code, error_message) {}
                });
            }

            // Dual Channel: Also send via HTTP REST endpoint for 100% reliability
            fetch(`/api/macro/toggle?id=${encodeURIComponent(id)}&enabled=${macro.running}`)
                .catch(() => {});
        }
    }

    function onTargetChange(id, target) {
        if (window.cefQuery) {
            window.cefQuery({ request: `set_target:${id}:${target}` });
        }
        fetch(`/api/macro/target?id=${encodeURIComponent(id)}&target=${encodeURIComponent(target)}`).catch(() => {});
    }

    function openMacroSettings(macro) {
        activeMacroSettingsModal = macro;
    }

    function closeMacroSettings() {
        activeMacroSettingsModal = null;
    }

    function openConfigGui() {
        if (window.cefQuery) {
            window.cefQuery({ request: 'open_config_gui' });
        }
        fetch('/api/config/gui').catch(() => {});
    }

    $: filteredMacros = macros.filter(m => {
        const matchesTab = m.category === currentTab;
        const matchesSearch = !searchQuery || m.title.toLowerCase().includes(searchQuery.toLowerCase()) || m.desc.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesTab && matchesSearch;
    });
</script>

<div style="font-family: {selectedFont}, Inter, Roboto, sans-serif;" class="w-screen h-screen flex items-center justify-center bg-black/60 select-none overflow-hidden relative">
    <div class="w-[820px] h-[520px] bg-slate-900/98 border border-white/10 rounded-2xl shadow-[0_20px_40px_-10px_rgba(0,0,0,0.8),0_0_30px_rgba(56,189,248,0.25)] flex overflow-hidden relative">
    
    <!-- Sidebar Navigation -->
    <aside class="w-[210px] bg-slate-900/95 border-r border-white/10 p-5 flex flex-col justify-between shrink-0 select-none">
        <div>
            <div class="flex items-center gap-2.5 mb-5">
                <div class="w-[32px] h-[32px] bg-gradient-to-br from-sky-400 to-sky-600 rounded-lg flex items-center justify-center font-bold text-white text-sm shadow-[0_3px_10px_rgba(56,189,248,0.4)]">
                    V
                </div>
                <div>
                    <h1 class="font-outfit text-[15px] font-bold tracking-wide bg-gradient-to-r from-white to-sky-400 bg-clip-text text-transparent leading-none">VERTEX CLIENT</h1>
                    <span class="text-[9px] text-slate-400 uppercase tracking-widest block mt-0.5">Fabric v1.21.11 • v1.0.0</span>
                </div>
            </div>

            <ul class="flex flex-col gap-1">
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'farming' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'farming'}>
                    Farming & Garden
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'mining' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'mining'}>
                    Mining & Commissions
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'slayer' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'slayer'}>
                    Slayer & Combat
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'fishing' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'fishing'}>
                    Fishing
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'foraging' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'foraging'}>
                    Foraging
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'alchemy' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'alchemy'}>
                    Economy & Alchemy
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'diana' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'diana'}>
                    Diana Mythological
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'settings' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => currentTab = 'settings'}>
                    Settings & Config
                </li>
            </ul>
        </div>

        <div class="bg-slate-800/50 border border-white/10 p-2.5 rounded-xl flex items-center gap-2.5">
            <div class="w-7 h-7 rounded-full bg-sky-500/20 border border-sky-400/30 text-sky-400 flex items-center justify-center font-bold text-xs">V</div>
            <div>
                <h4 class="text-[11px] font-semibold leading-tight">{playerName}</h4>
                <p class="text-[9px] text-emerald-400">● Connected {#if liveFps > 0}• {liveFps} FPS{/if}</p>
            </div>
        </div>
    </aside>

    <!-- Main Workspace View -->
    <main class="flex-1 p-5 overflow-y-auto flex flex-col gap-4 select-none">
        
        {#if currentTab !== 'settings'}
            <!-- Header Stats Banner -->
            <div class="grid grid-cols-4 gap-3">
                <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col gap-1 relative overflow-hidden after:content-[''] after:absolute after:top-0 after:right-0 after:w-1 after:h-full after:bg-sky-400 after:rounded-r-xl">
                    <span class="text-[9px] text-slate-400 uppercase tracking-wide">Active Macro</span>
                    <span class="font-outfit text-[13px] font-bold truncate">{activeMacroName}</span>
                </div>
                <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col gap-1 relative overflow-hidden after:content-[''] after:absolute after:top-0 after:right-0 after:w-1 after:h-full after:bg-sky-400 after:rounded-r-xl">
                    <span class="text-[9px] text-slate-400 uppercase tracking-wide">Farming Speed</span>
                    <span class="font-outfit text-[13px] font-bold truncate">{farmingSpeed}</span>
                </div>
                <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col gap-1 relative overflow-hidden after:content-[''] after:absolute after:top-0 after:right-0 after:w-1 after:h-full after:bg-sky-400 after:rounded-r-xl">
                    <span class="text-[9px] text-slate-400 uppercase tracking-wide">Est. Profit / Hr</span>
                    <span class="font-outfit text-[13px] font-bold truncate">{estProfit}</span>
                </div>
                <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col gap-1 relative overflow-hidden after:content-[''] after:absolute after:top-0 after:right-0 after:w-1 after:h-full after:bg-sky-400 after:rounded-r-xl">
                    <span class="text-[9px] text-slate-400 uppercase tracking-wide">Session Status</span>
                    <span class="font-outfit text-[13px] font-bold text-sky-400 truncate">{activeMacroName !== 'None' ? 'Active' : 'Idle'}</span>
                </div>
            </div>

            <!-- Controls Bar -->
            <div class="flex justify-between items-center gap-3">
                <input type="text" bind:value={searchQuery} class="bg-slate-800/60 border border-white/10 px-3 py-1.5 rounded-lg w-[240px] text-xs outline-none transition-all duration-200 focus:border-sky-400 focus:shadow-[0_0_10px_rgba(56,189,248,0.2)]" placeholder="Search macros & automation...">
                <div class="text-[10px] text-slate-400 flex items-center gap-2 bg-slate-800/40 px-3 py-1.5 rounded-lg border border-white/5 font-mono">
                    <span>Right-click card for settings</span>
                </div>
            </div>

            <!-- Cards Grid -->
            <div class="grid grid-cols-2 gap-3">
                {#each filteredMacros as macro}
                <!-- svelte-ignore a11y_no_static_element_interactions -->
                <div class="bg-slate-800/70 border border-white/10 rounded-xl p-3.5 flex flex-col justify-between gap-2.5 transition-all duration-300 hover:border-sky-400/40 hover:shadow-[0_8px_20px_rgba(0,0,0,0.3)] min-w-0 cursor-pointer relative group" on:contextmenu|preventDefault={() => openMacroSettings(macro)}>
                    <div class="flex justify-between items-start gap-2">
                        <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-1.5">
                                <div class="text-[12px] font-semibold text-white truncate">{macro.title}</div>
                                <button title="Configure Settings" class="opacity-0 group-hover:opacity-100 transition-opacity text-slate-400 hover:text-sky-400 text-[10px] px-1 bg-white/5 rounded" on:click|stopPropagation={() => openMacroSettings(macro)}>⚙</button>
                            </div>
                            <div class="text-[10px] text-slate-400 mt-0.5 leading-snug line-clamp-2">{macro.desc}</div>
                        </div>
                        {#if macro.running}
                            <span class="px-2 py-0.5 rounded-full text-[8px] font-semibold uppercase bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 shrink-0">Running</span>
                        {:else}
                            <span class="px-2 py-0.5 rounded-full text-[8px] font-semibold uppercase bg-slate-400/15 text-slate-400 border border-slate-400/30 shrink-0">Idle</span>
                        {/if}
                    </div>
                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                    <div class="flex items-center justify-between gap-2 pt-1 border-t border-white/5 min-w-0" on:click|stopPropagation>
                        <select bind:value={macro.target} on:change={(e) => onTargetChange(macro.id, e.target.value)} class="bg-slate-900/90 border border-white/10 text-white px-2 py-1 rounded-lg text-[10px] outline-none cursor-pointer max-w-[140px] truncate shrink min-w-0">
                            {#if macro.options}
                                {#each macro.options as opt}
                                    <option value={opt}>{opt}</option>
                                {/each}
                            {:else}
                                <option>{macro.target}</option>
                            {/if}
                        </select>
                        {#if macro.running}
                            <button class="px-3 py-1 rounded-lg text-[10px] font-semibold bg-gradient-to-br from-red-500 to-red-600 text-white shadow-[0_2px_8px_rgba(239,68,68,0.3)] transition-all cursor-pointer whitespace-nowrap shrink-0" on:click={() => toggleMacro(macro.id)}>Stop Macro</button>
                        {:else}
                            <button class="px-3 py-1 rounded-lg text-[10px] font-semibold bg-gradient-to-br from-sky-400 to-sky-600 text-white shadow-[0_2px_8px_rgba(56,189,248,0.3)] transition-all cursor-pointer whitespace-nowrap shrink-0" on:click={() => toggleMacro(macro.id)}>Start Macro</button>
                        {/if}
                    </div>
                </div>
                {/each}
            </div>
        {/if}

        {#if currentTab === 'settings'}
            <div class="flex flex-col gap-3">
                <div class="flex justify-between items-center mb-1">
                    <h2 class="text-sm font-bold">Client & Interface Settings</h2>
                </div>
                
                <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex justify-between items-center">
                    <div>
                        <h3 class="text-[12px] font-semibold text-white">GUI Font Typography</h3>
                        <p class="text-[10px] text-slate-400 mt-0.5">Select interface font style across the entire dashboard UI</p>
                    </div>
                    <select bind:value={selectedFont} class="bg-slate-900/80 border border-white/10 text-white px-2.5 py-1 rounded-lg text-[10px] w-[130px] outline-none">
                        <option value="Outfit">Outfit</option>
                        <option value="Inter">Inter</option>
                        <option value="Roboto">Roboto</option>
                    </select>
                </div>
                
                <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex justify-between items-center">
                    <div>
                        <h3 class="text-[12px] font-semibold text-white">Smart Failsafe System</h3>
                        <p class="text-[10px] text-slate-400 mt-0.5">Automatically chooses and plays the best reaction recording</p>
                    </div>
                    <span class="px-2.5 py-1 rounded-lg text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">Active</span>
                </div>

                <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex justify-between items-center">
                    <div>
                        <h3 class="text-[12px] font-semibold text-white">Discord Webhook Integration</h3>
                        <p class="text-[10px] text-slate-400 mt-0.5">Real-time macro session reports, profit stats & alerts</p>
                    </div>
                    <input type="password" placeholder="https://discord.com/api/webhooks/..." class="bg-slate-900/80 border border-white/10 text-slate-300 px-2.5 py-1 rounded-lg text-[10px] w-[180px] outline-none" />
                </div>

                <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex justify-between items-center">
                    <div>
                        <h3 class="text-[12px] font-semibold text-white">GUI Delay Randomizer (ms)</h3>
                        <p class="text-[10px] text-slate-400 mt-0.5">Maximum random time to add to GUI Delay</p>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-[10px] text-sky-400 font-mono w-8 text-right">250</span>
                        <input type="range" min="50" max="1000" step="50" value="250" class="w-[100px] accent-sky-400 cursor-pointer" />
                    </div>
                </div>
            </div>
        {/if}

    </main>
</div>

{#if activeMacroSettingsModal}
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="absolute inset-0 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-6 z-50">
    <div class="w-[460px] bg-slate-900 border border-sky-500/30 rounded-2xl p-5 shadow-[0_20px_50px_rgba(0,0,0,0.8)] flex flex-col gap-4 relative animate-in fade-in zoom-in-95 duration-150">
        <div class="flex justify-between items-start border-b border-white/10 pb-3">
            <div>
                <h3 class="text-sm font-bold text-white">{activeMacroSettingsModal.title}</h3>
                <p class="text-[10px] text-sky-400 font-mono mt-0.5">Per-Macro Configuration & Fine-Tuning</p>
            </div>
            <!-- svelte-ignore a11y_click_events_have_key_events -->
            <button class="text-slate-400 hover:text-white text-xs font-bold px-2 py-1 bg-white/5 rounded-lg cursor-pointer" on:click={closeMacroSettings}>✕</button>
        </div>

        <div class="flex flex-col gap-2.5">
            {#if dynamicConfigSchema}
                {#each categoryMapping[macros.find(m => m.id === activeMacroSettingsModal)?.category] || [] as catId}
                    {#if dynamicConfigSchema[catId]}
                        <div class="mt-2 mb-1 pl-1">
                            <h4 class="text-[11px] font-bold text-sky-400 uppercase tracking-wider">{dynamicConfigSchema[catId].name}</h4>
                        </div>
                        {#each dynamicConfigSchema[catId].settings as setting}
                            <div class="flex justify-between items-center bg-slate-800/60 p-2.5 rounded-xl border border-white/5">
                                <div>
                                    <div class="text-xs text-slate-200 font-medium">{setting.name}</div>
                                    <div class="text-[9px] text-slate-400 max-w-[200px] leading-tight mt-0.5">{setting.desc}</div>
                                </div>
                                
                                {#if setting.type === 'boolean'}
                                    <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                                {:else if setting.type === 'slider'}
                                    <div class="flex items-center gap-2">
                                        <span class="text-[10px] text-sky-400 font-mono w-8 text-right">{setting.value}</span>
                                        <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="w-[100px] accent-sky-400 cursor-pointer" />
                                    </div>
                                {:else if setting.type === 'text'}
                                    <input type="text" bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1 rounded-lg text-[10px] outline-none w-[120px]" />
                                {:else if setting.type === 'dropdown'}
                                    <select bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1 rounded-lg text-[10px] outline-none w-[120px]">
                                        {#each setting.options as opt, i}
                                            <option value={i}>{opt}</option>
                                        {/each}
                                    </select>
                                {/if}
                            </div>
                        {/each}
                    {/if}
                {/each}
            {:else}
                <div class="text-xs text-slate-400 text-center py-4">Loading configuration schema from Java...</div>
            {/if}
        </div>

        <div class="flex justify-end items-center pt-2 border-t border-white/10">
            <button class="px-4 py-1.5 rounded-lg text-[10px] font-semibold bg-gradient-to-r from-sky-400 to-sky-600 text-white shadow-lg transition-all cursor-pointer" on:click={closeMacroSettings}>
                Save Settings
            </button>
        </div>
    </div>
</div>
{/if}
</div>
