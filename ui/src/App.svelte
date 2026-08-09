<script>
    import { onMount } from 'svelte';
    import './app.css';

    let isGuiOpen = true;
    let currentTab = 'dashboard';
    let searchQuery = '';
    let playerName = 'FuzzyDonutPro';
    let isConnected = true;
    let activeMacroName = 'None';
    let subState = '';
    let macroRuntime = '00:00:00';
    let isRunning = false;
    let farmingSpeed = '0.0 BPS';
    let estProfit = '0 / hr';
    let liveFps = 0;
    let selectedFont = 'Outfit';
    let uiSoundStyle = 1;
    let selectedTheme = 'cyber';
    let uiScale = 1.0;
    
    if (typeof window !== 'undefined') {
        const savedTheme = localStorage.getItem('vertex_uiTheme');
        if (savedTheme) selectedTheme = savedTheme;
        
        const savedScale = localStorage.getItem('vertex_uiScale');
        if (savedScale) uiScale = parseFloat(savedScale);

        window.setUiScale = (scale) => {
            if (!localStorage.getItem('vertex_uiScale')) {
                uiScale = scale;
            }
        };

        window.setGuiOpen = (open) => {
            isGuiOpen = open;
        };
    }
    
    $: if (typeof window !== 'undefined') {
        localStorage.setItem('vertex_uiTheme', selectedTheme);
        localStorage.setItem('vertex_uiScale', uiScale.toString());
    }
    
    const fontsList = ['Inter', 'Outfit', 'Roboto', 'JetBrains Mono', 'Minecraft'];

    const themesList = [
        { id: 'cyber', name: 'Cyber Sky', accent: 'sky', primaryColor: '#38bdf8', desc: 'Futuristic slate dashboard with neon cyan & sky blue accents.' },
        { id: 'purple', name: 'Neon Purple', accent: 'purple', primaryColor: '#a855f7', desc: 'Vibrant dark synthwave layout with glowing violet highlights.' },
        { id: 'emerald', name: 'Emerald Garden', accent: 'emerald', primaryColor: '#34d399', desc: 'Fresh green aesthetic tuned for Garden & Farming enthusiasts.' },
        { id: 'crimson', name: 'Crimson Peak', accent: 'rose', primaryColor: '#f43f5e', desc: 'Aggressive high-contrast red & rose theme for Combat & Slayer.' },
        { id: 'gold', name: 'Gold Luxe', accent: 'amber', primaryColor: '#fbbf24', desc: 'Premium gold & amber styling for Economy & Bazaar flippers.' },
        { id: 'rainbow', name: 'Chroma RGB', accent: 'rainbow', primaryColor: '#a855f7', desc: 'Dynamic RGB gamer theme with vibrant colors across the spectrum.' }
    ];

    function applyTheme(themeId) {
        selectedTheme = themeId;
        playUiSound();
    }

    let hue = 0;
    let rainbowFrame;

    function animateRainbow() {
        if (selectedTheme !== 'rainbow') {
            rainbowFrame = null;
            return;
        }
        hue = (hue + 1) % 360;
        if (typeof document !== 'undefined') {
            document.documentElement.style.setProperty('--theme-400', `hsl(${hue}, 100%, 65%)`);
            document.documentElement.style.setProperty('--theme-500', `hsl(${hue}, 100%, 50%)`);
            document.documentElement.style.setProperty('--theme-600', `hsl(${hue}, 100%, 40%)`);
        }
        rainbowFrame = requestAnimationFrame(animateRainbow);
    }

    $: {
        const theme = themesList.find(t => t.id === selectedTheme) || themesList[0];
        if (typeof document !== 'undefined') {
            if (theme.accent === 'rainbow') {
                if (!rainbowFrame) animateRainbow();
            } else {
                if (rainbowFrame) {
                    cancelAnimationFrame(rainbowFrame);
                    rainbowFrame = null;
                }
                let t400 = '#38bdf8', t500 = '#0ea5e9', t600 = '#0284c7';
                if (theme.accent === 'purple') { t400 = '#c084fc'; t500 = '#a855f7'; t600 = '#9333ea'; }
                else if (theme.accent === 'emerald') { t400 = '#34d399'; t500 = '#10b981'; t600 = '#059669'; }
                else if (theme.accent === 'rose') { t400 = '#fb7185'; t500 = '#f43f5e'; t600 = '#e11d48'; }
                else if (theme.accent === 'amber') { t400 = '#fbbf24'; t500 = '#f59e0b'; t600 = '#d97706'; }
                
                document.documentElement.style.setProperty('--theme-400', t400);
                document.documentElement.style.setProperty('--theme-500', t500);
                document.documentElement.style.setProperty('--theme-600', t600);
            }
        }
    }

    function playUiSound() {
        if (uiSoundStyle === 0) return;
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            const now = ctx.currentTime;
            
            if (uiSoundStyle === 1) {
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(1200, now);
                osc.frequency.exponentialRampToValueAtTime(300, now + 0.02);
                gain.gain.setValueAtTime(0.15, now);
                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.02);
                osc.start(now);
                osc.stop(now + 0.02);
            }
        } catch(e) {}
    }

    function fetchConfigSchema() {
        if (window.cefQuery) {
            window.cefQuery({
                request: 'get_config_schema',
                onSuccess: function(response) {
                    try {
                        let data = JSON.parse(response);
                        if (data && typeof data === 'object' && Object.keys(data).length > 0) {
                            dynamicConfigSchema = { ...defaultConfigSchema, ...data };
                        }
                    } catch (e) {}
                }
            });
        }
        fetch('/api/config/schema')
            .then(res => res.json())
            .then(data => {
                if (data && typeof data === 'object' && Object.keys(data).length > 0) {
                    dynamicConfigSchema = { ...defaultConfigSchema, ...data };
                }
            })
            .catch(() => {});
    }

    function executeButtonAction(catId, fieldId) {
        playUiSound();
        let targetCat = catId && catId !== 'undefined' ? catId : 'general';
        if (window.cefQuery) {
            window.cefQuery({
                request: `click_button:${targetCat}:${fieldId}`,
                onSuccess: function(response) {
                    try {
                        let data = JSON.parse(response);
                        if (data && typeof data === 'object' && Object.keys(data).length > 0) {
                            dynamicConfigSchema = { ...defaultConfigSchema, ...data };
                        } else {
                            fetchConfigSchema();
                        }
                    } catch (e) {
                        fetchConfigSchema();
                    }
                }
            });
        }
        fetch(`/api/config/button?catId=${encodeURIComponent(targetCat)}&fieldId=${encodeURIComponent(fieldId)}`, { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                if (data && typeof data === 'object' && Object.keys(data).length > 0) {
                    dynamicConfigSchema = { ...defaultConfigSchema, ...data };
                } else {
                    fetchConfigSchema();
                }
            })
            .catch(() => fetchConfigSchema());
    }

    let macros = [
        { id: 'crop', category: 'farming', title: 'Crop/Wart S-Shape Macro', desc: 'Auto 45° S-Shape lane traversal for Wheat, Carrot, Potato & Nether Wart.', running: false, target: 'Wheat', options: ['Wheat', 'Carrot', 'Potato', 'Nether Wart'] },
        { id: 'sugarcane', category: 'farming', title: 'Sugarcane & Cactus Straight Macro', desc: 'Straight lane traversal for Sugarcane & Cactus plots.', running: false, target: 'Sugar Cane', options: ['Sugar Cane', 'Cactus'] },
        { id: 'melon_pumpkin', category: 'farming', title: 'Melon & Pumpkin Macro', desc: 'Auto lane traversal specifically tuned for Melon & Pumpkin farms.', running: false, target: 'Melon', options: ['Melon', 'Pumpkin'] },
        { id: 'farm_builder', category: 'farming', title: 'Garden Plot Auto Farm Builder', desc: 'Auto-builds preset infinite farm layouts across Garden plots.', running: false, target: 'Preset S-Shape' },
        { id: 'visitor', category: 'farming', title: 'Garden Visitor Trader', desc: 'Fulfills NPC visitor crop trades for Copper & EXP.', running: false, target: 'Auto Trade' },
        { id: 'pest_hunter', category: 'farming', title: 'Pest Hunter & Vacuum', desc: 'Auto-detects and vacuoms Garden plot pests and trades with Pest Broker.', running: false, target: 'Vacuum + Trade' },

        { id: 'commission', category: 'mining', title: 'Commission Auto-Miner', desc: 'Dwarven Mines & Crystal Hollows commission route solver with Etherwarp.', running: false, target: 'Dwarven Mines', options: ['Dwarven Mines', 'Crystal Hollows', 'Glacite Mineshafts'] },
        { id: 'gemstone', category: 'mining', title: 'Gemstone Etherwarp Route Miner', desc: 'Follows custom JSON waypoint routes with 0-tick Etherwarp & Pickaxe swap.', running: false, target: 'Ruby Route #1', options: ['Ruby Route #1', 'Jasper Route #1', 'Sapphire Route #2', 'Topaz Magma Fields'] },
        { id: 'mining_general', category: 'mining', title: 'Mithril & Ore Miner', desc: 'Auto-mines Mithril, Titanium, Gemstones & All Ores with smooth head rotation.', running: false, target: 'Mithril & Titanium', options: ['Mithril & Titanium', 'Diamond', 'Emerald', 'Redstone', 'Lapis', 'Gold', 'Iron', 'Coal', 'Hardstone', 'Gemstones', 'Glacite', 'Tungsten', 'Umber'] },
        { id: 'powder', category: 'mining', title: 'Chest & Mithril Powder Miner', desc: 'Uncovers and loots buried treasure chests in Crystal Hollows for Powder.', running: false, target: 'Chest Solver + Mining' },
        { id: 'glacial', category: 'mining', title: 'Glacial Cave Ice/Mithril Miner', desc: 'Auto-mines Glacite & Glacial Ice in Mineshafts with pathfinder.', running: false, target: 'Glacial Ice' },
        { id: 'nuker', category: 'mining', title: 'Custom Block & Ore Nuker', desc: 'High-speed block nuker with range & FOV filters.', running: false, target: 'Mithril Ores' },

        { id: 'slayer', category: 'slayer', title: 'Slayer Boss Macro', desc: 'Auto-spawns and slays Slayer bosses (Revenant, Tarantula, Sven, Voidgloom).', running: false, target: 'Revenant Horror', options: ['Revenant Horror', 'Tarantula Broodfather', 'Sven Packmaster', 'Voidgloom Seraph'] },
        { id: 'mob_killer', category: 'slayer', title: 'General Mob Killer', desc: 'Auto-pathfinds and grinds area mobs for EXP, drops, and bestiary.', running: false, target: 'Graveyard Zombies', options: ['Zealots', 'Ghosts', 'Ice Walkers', 'Treasure Hoarders', 'Goblins', 'Glacite Walkers', 'Automotons', 'Sludge', 'Yog', 'Graveyard Zombies', "Spider's Den Spiders & Silverfish"] },
        { id: 'zealot', category: 'slayer', title: 'End Zealot & Bruiser Farmer', desc: 'Auto-pathfinds and kills Zealots & Special Zealot Bruisers in The End.', running: false, target: 'Special Zealots' },
        { id: 'dungeon', category: 'slayer', title: 'Dungeons & Catacombs Solver', desc: 'Auto room clear, secret finder & boss room combat helper.', running: false, target: 'Catacombs Floor 7' },
        { id: 'kuudra', category: 'slayer', title: 'Kuudra Boss Suite', desc: 'Auto-Supply Rush, Cannon Build/Fueling, Pod Head Stun, and Core DPS.', running: false, target: 'Kuudra T5', options: ['Kuudra T1', 'Kuudra T2', 'Kuudra T3', 'Kuudra T4', 'Kuudra T5'] },

        { id: 'fishing', category: 'fishing', title: 'Lava & Water Auto-Fisher', desc: 'Auto-casts rod, detects bobber hook animation, and slays sea creatures.', running: false, target: 'Crimson Isle Lava', options: ['Crimson Isle Lava', 'Main Hub Water'] },
        { id: 'trophy_fishing', category: 'fishing', title: 'Trophy Fishing Solver', desc: 'Optimized trophy fish hook timing with auto-slugfish & Obfuscated Fish filleting.', running: false, target: 'Trophy Fish Auto-Catch' },

        { id: 'foraging', category: 'foraging', title: 'Park & Jungle Wood Foraging', desc: 'Auto-cuts Dark Oak, Acacia, Jungle, Spruce, Oak & Birch trees with Treecapitator auto-swap.', running: false, target: 'Dark Oak', options: ['Dark Oak', 'Acacia', 'Jungle', 'Spruce', 'Oak', 'Birch'] },

        { id: 'alchemy', category: 'alchemy', title: 'Auto Alchemy Potion Brewer', desc: 'Auto-brews Speed / EXP potions via Nether Wart & Enchanted Sugarcane.', running: false, target: 'Speed Potion', options: ['Speed Potion', 'EXP Potion', 'Alchemy 50 Batch'] },
        { id: 'flip', category: 'alchemy', title: 'Bazaar & AH Order Flipper', desc: 'Auto-monitors profit margins and posts buy/sell orders on Bazaar.', running: false, target: 'Bazaar Auto-Flip' },

        { id: 'diana', category: 'diana', title: 'Diana Mythological Burrow Finder', desc: 'Ancestral spade particle trail pathfinding, digging, and Mythological mob killer.', running: false, target: 'Daedalus Axe', options: ['Daedalus Axe', 'Ancestral Spade Only', 'Inquisitor Hunter'] }
    ];

    const defaultConfigSchema = {
        gui: { id: 'gui', name: 'Themes & Styling', settings: [{ id: 'handChams', name: 'Hand Chams', desc: 'Render held item with opaque glowing effect', type: 'boolean', value: false }, { id: 'chamsGlowAmount', name: 'Glow Amount', desc: 'Intensity of hand chams glow', type: 'slider', min: 0.0, max: 2.0, step: 0.1, value: 1.0 }, { id: 'chamsGlowColor', name: 'Glow Color', desc: 'Hex color for hand chams', type: 'text', value: '#00FFFF' }] },
        farming: { id: 'farming', name: 'Farming Settings', settings: [{ id: 'farmingCrop', name: 'Target Crop', desc: 'Select target crop', type: 'dropdown', options: ['Wheat', 'Carrot', 'Potato', 'Nether Wart', 'Sugar Cane', 'Cactus', 'Melon', 'Pumpkin'], value: 0 }, { id: 'laneSpeed', name: 'Walk Speed (BPS)', desc: 'Movement speed during farming', type: 'slider', min: 1, max: 400, step: 1, value: 250 }, { id: 'autoTeleport', name: 'Auto Spawn Teleport', desc: 'Warp back to plot start when lane ends', type: 'boolean', value: true }] },
        miningMacro: { id: 'miningMacro', name: 'Mining Settings', settings: [{ id: 'oreType', name: 'Target Ore', desc: 'Select ore type to mine', type: 'dropdown', options: ['Mithril', 'Diamond', 'Emerald', 'Redstone', 'Lapis', 'Gold', 'Iron', 'Coal', 'Hardstone', 'Gemstones', 'Glacite', 'Tungsten', 'Umber'], value: 0 }, { id: 'allowPathfinder', name: 'Use Pathfinder', desc: 'Pathfind between ore veins', type: 'boolean', value: true }] },
        combat: { id: 'combat', name: 'Combat & Slayer Settings', settings: [{ id: 'slayerTarget', name: 'Slayer Target', desc: 'Select Slayer Boss tier', type: 'dropdown', options: ['Revenant Horror', 'Tarantula Broodfather', 'Sven Packmaster', 'Voidgloom Seraph'], value: 0 }, { id: 'autoWeaponSwap', name: 'Auto Weapon Swap', desc: 'Swap weapon when boss spawns', type: 'boolean', value: true }, { id: 'autoRogueSword', name: 'Auto Rogue Sword', desc: 'Use Rogue Sword speed boost', type: 'boolean', value: false }] },
        fishing: { id: 'fishing', name: 'Fishing Settings', settings: [{ id: 'rodAutoCast', name: 'Auto Recast Rod', desc: 'Recast rod automatically after catch', type: 'boolean', value: true }, { id: 'seaCreatureKill', name: 'Auto Slay Creatures', desc: 'Slay sea creatures instantly', type: 'boolean', value: true }] },
        foraging: { id: 'foraging', name: 'Foraging Settings', settings: [{ id: 'foragingTreeType', name: 'Target Tree Type', desc: 'Select target wood type', type: 'dropdown', options: ['Dark Oak', 'Acacia', 'Jungle', 'Spruce', 'Oak', 'Birch'], value: 0 }] },
        general: { id: 'general', name: 'General Settings', settings: [{ id: 'autoFailSafe', name: 'Failsafe Protection', desc: 'Pause macro when admin check detected', type: 'boolean', value: true }] }
    };

    let dynamicConfigSchema = defaultConfigSchema;

    onMount(() => {
        fetchStatus();
        const interval = setInterval(fetchStatus, 1000);
        fetchConfigSchema();
        return () => clearInterval(interval);
    });

    function updateConfigValue(categoryId, fieldId, value) {
        dynamicConfigSchema = dynamicConfigSchema;
        macros = macros;
        
        if (fieldId === 'guiFont') {
            selectedFont = fontsList[parseInt(value)] || 'Outfit';
        }
        if (fieldId === 'uiSoundStyle') {
            uiSoundStyle = parseInt(value) || 0;
            playUiSound();
        }

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
                if (data.playerName && data.playerName !== 'Player') playerName = data.playerName;
                if (data.activeMacro) activeMacroName = data.activeMacro;
                if (data.subState !== undefined) subState = data.subState;
                if (data.runtime) macroRuntime = data.runtime;
                if (data.isRunning !== undefined) isRunning = data.isRunning;
                if (data.bps) farmingSpeed = data.bps;
                if (data.estProfit) estProfit = data.estProfit;
                if (data.fps) liveFps = data.fps;
            }
        } catch (e) {}

        if (window.cefQuery) {
            window.cefQuery({
                request: 'get_status',
                onSuccess: function(response) {
                    try {
                        let data = JSON.parse(response);
                        if (data && data.status === 'ok') {
                            if (data.playerName) playerName = data.playerName;
                            if (data.activeMacro) activeMacroName = data.activeMacro;
                            if (data.subState !== undefined) subState = data.subState;
                            if (data.runtime) macroRuntime = data.runtime;
                            if (data.isRunning !== undefined) isRunning = data.isRunning;
                            if (data.bps) farmingSpeed = data.bps;
                            if (data.estProfit) estProfit = data.estProfit;
                            if (data.fps) liveFps = data.fps;
                        }
                    } catch (e) {}
                }
            });
        }
    }

    function toggleMacro(id) {
        let macro = macros.find(m => m.id === id);
        if (macro) {
            macro.running = !macro.running;
            if (macro.running) {
                macros.forEach(m => { if (m.id !== id) m.running = false; });
                activeMacroName = macro.title;
            } else {
                activeMacroName = 'None';
            }
            macros = [...macros];

            if (window.cefQuery) {
                window.cefQuery({ request: `toggle_macro:${id}:${macro.running}` });
            }
            fetch(`/api/macro/toggle?id=${encodeURIComponent(id)}&enabled=${macro.running}`).catch(() => {});
        }
    }

    function onTargetChange(id, target) {
        let macro = macros.find(m => m.id === id);
        if (macro) {
            macro.target = target;
            macros = [...macros];
        }
        if (window.cefQuery) {
            window.cefQuery({ request: `set_target:${id}:${target}` });
        }
        fetch(`/api/macro/target?id=${encodeURIComponent(id)}&target=${encodeURIComponent(target)}`).catch(() => {});
    }

    function saveConfig() {
        playUiSound();
        if (window.cefQuery) {
            window.cefQuery({ request: 'save_config' });
        }
        fetch('/api/config/save', { method: 'POST' }).catch(() => {});
    }

    function closeGui() {
        saveConfig();
        isGuiOpen = false;
        if (window.cefQuery) {
            window.cefQuery({ request: 'close_gui' });
        }
        fetch('/api/gui/close', { method: 'POST' }).catch(() => {});
    }

    $: filteredMacros = macros.filter(m => {
        const matchesTab = (currentTab === 'dashboard') ? true : (m.category === currentTab);
        const matchesSearch = !searchQuery || m.title.toLowerCase().includes(searchQuery.toLowerCase()) || m.desc.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesTab && matchesSearch;
    });

    let activeKeybindListening = null;
    const glfwNames = { 342: 'L-ALT', 346: 'R-ALT', 340: 'L-SHIFT', 344: 'R-SHIFT', 341: 'L-CTRL', 345: 'R-CTRL', 32: 'SPACE', 256: 'NONE', 257: 'ENTER', 258: 'TAB', 259: 'BACKSPACE' };

    function getKeyName(code) {
        if (!code || code === 0 || code === -1) return 'NONE';
        if (glfwNames[code]) return glfwNames[code];
        if (code >= 65 && code <= 90) return String.fromCharCode(code);
        if (code >= 48 && code <= 57) return String.fromCharCode(code);
        return `KEY-${code}`;
    }

    function startKeybindListen(catId, setting) {
        activeKeybindListening = { catId, settingId: setting.id };
        const handler = (e) => {
            e.preventDefault();
            e.stopPropagation();
            let keyCode = 0;
            if (e.code === 'AltLeft') keyCode = 342;
            else if (e.code === 'AltRight') keyCode = 346;
            else if (e.code === 'ShiftLeft') keyCode = 340;
            else if (e.code === 'ShiftRight') keyCode = 344;
            else if (e.code === 'ControlLeft') keyCode = 341;
            else if (e.code === 'ControlRight') keyCode = 345;
            else if (e.code === 'Space') keyCode = 32;
            else if (e.code === 'Escape') keyCode = -1;
            else if (e.code.startsWith('Key')) keyCode = e.code.replace('Key', '').charCodeAt(0);
            else if (e.code.startsWith('Digit')) keyCode = e.code.replace('Digit', '').charCodeAt(0);
            else keyCode = e.keyCode || 0;

            setting.value = keyCode;
            updateConfigValue(catId, setting.id, keyCode);
            window.removeEventListener('keydown', handler, true);
            activeKeybindListening = null;
        };
        window.addEventListener('keydown', handler, true);
    }
</script>

<div style="font-family: {selectedFont}, Inter, sans-serif;" class="w-screen h-screen select-none overflow-hidden relative">
    
    {#if isGuiOpen}
        <!-- Full Vertex Config Dashboard Screen -->
        <div class="w-full h-full flex items-center justify-center bg-slate-950/80 backdrop-blur-md">
            <div style="transform: scale({uiScale}); transform-origin: center; transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);" class="w-[940px] h-[580px] bg-[#0c101d]/95 border border-slate-800/80 rounded-3xl shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9),0_0_40px_rgba(56,189,248,0.15)] flex overflow-hidden relative z-10">
                
                <!-- Left Sidebar Rail -->
                <aside class="w-[220px] bg-[#080b14] border-r border-slate-800/80 p-5 flex flex-col justify-between shrink-0 select-none">
                    <div>
                        <!-- Brand Title -->
                        <div class="flex items-center gap-3 mb-6">
                            <div class="w-8 h-8 rounded-xl bg-gradient-to-tr from-sky-500 to-sky-400 flex items-center justify-center font-bold text-white shadow-lg shadow-sky-500/20 text-sm">
                                V
                            </div>
                            <div>
                                <h1 class="text-sm font-bold tracking-wider text-white">VERTEX</h1>
                                <span class="text-[10px] text-sky-400 font-medium tracking-widest uppercase block">Fabric v1.21.11</span>
                            </div>
                        </div>

                        <!-- Navigation List -->
                        <nav class="flex flex-col gap-1.5 overflow-y-auto max-h-[400px] pr-1 custom-scrollbar">
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'dashboard' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'dashboard'; }}>
                                <span>📊</span> Dashboard
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'farming' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'farming'; }}>
                                <span>🌾</span> Garden & Farming
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'mining' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'mining'; }}>
                                <span>⛏️</span> Mining & Hollows
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'slayer' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'slayer'; }}>
                                <span>⚔️</span> Slayer & Combat
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'fishing' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'fishing'; }}>
                                <span>🎣</span> Fishing
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'foraging' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'foraging'; }}>
                                <span>🪓</span> Foraging
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'alchemy' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'alchemy'; }}>
                                <span>🧪</span> Economy & Alchemy
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'diana' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'diana'; }}>
                                <span>🦅</span> Diana Mythological
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'config' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'config'; }}>
                                <span>⚙️</span> Config & Settings
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'failsafe' ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'failsafe'; }}>
                                <span>🛡️</span> Security & Failsafes
                            </button>
                            <button class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 {currentTab === 'themes' ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30' : 'text-slate-400 hover:bg-slate-800/40 hover:text-white'}" on:click={() => { playUiSound(); currentTab = 'themes'; }}>
                                <span>🎨</span> Themes & Style
                            </button>
                        </nav>
                    </div>

                    <!-- Player Status Footer Card -->
                    <div class="bg-slate-900/60 border border-slate-800/80 p-3 rounded-2xl flex items-center justify-between">
                        <div>
                            <h4 class="text-xs font-bold text-slate-200">{playerName}</h4>
                            <span class="text-[10px] text-emerald-400 font-medium">● Online {#if liveFps > 0}• {liveFps} FPS{/if}</span>
                        </div>
                        <button on:click={closeGui} class="w-7 h-7 rounded-xl bg-slate-800 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 flex items-center justify-center transition-all">
                            ✕
                        </button>
                    </div>
                </aside>

                <!-- Right Content Workspace -->
                <main class="flex-1 flex flex-col overflow-hidden bg-[#0a0d18]">
                    <header class="h-16 border-b border-slate-800/80 px-6 flex items-center justify-between shrink-0">
                        <div class="relative w-72">
                            <input 
                                type="text" 
                                bind:value={searchQuery} 
                                placeholder="Search macros or settings..." 
                                class="w-full bg-slate-900/80 border border-slate-800 text-xs text-slate-200 placeholder-slate-500 px-4 py-2 rounded-xl focus:outline-none focus:border-sky-500/50 transition-all"
                            />
                        </div>

                        <div class="flex items-center gap-4 text-xs font-medium text-slate-400">
                            <div>Active: <span class="text-sky-400 font-semibold">{activeMacroName}</span></div>
                            {#if farmingSpeed !== '0.0 BPS'}
                                <div>Speed: <span class="text-emerald-400 font-semibold">{farmingSpeed}</span></div>
                            {/if}
                            <button on:click={saveConfig} class="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold text-xs transition-all shadow-md shadow-sky-500/20">
                                Save Config
                            </button>
                        </div>
                    </header>

                    <div class="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
                        {#if currentTab === 'dashboard'}
                            <div class="grid grid-cols-3 gap-4">
                                <div class="bg-slate-900/40 border border-slate-800 p-5 rounded-2xl">
                                    <h4 class="text-xs text-slate-400 font-medium">ACTIVE MACRO</h4>
                                    <p class="text-lg font-bold text-sky-400 mt-1">{activeMacroName}</p>
                                </div>
                                <div class="bg-slate-900/40 border border-slate-800 p-5 rounded-2xl">
                                    <h4 class="text-xs text-slate-400 font-medium">LIVE BPS</h4>
                                    <p class="text-lg font-bold text-emerald-400 mt-1">{farmingSpeed}</p>
                                </div>
                                <div class="bg-slate-900/40 border border-slate-800 p-5 rounded-2xl">
                                    <h4 class="text-xs text-slate-400 font-medium">ESTIMATED PROFIT</h4>
                                    <p class="text-lg font-bold text-amber-400 mt-1">{estProfit}</p>
                                </div>
                            </div>
                        {/if}

                        {#if filteredMacros.length > 0}
                            <div>
                                <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">Available Features</h3>
                                <div class="grid grid-cols-2 gap-4">
                                    {#each filteredMacros as macro}
                                        <div class="bg-slate-900/50 border border-slate-800 hover:border-sky-500/30 p-5 rounded-2xl transition-all duration-200 flex flex-col justify-between">
                                            <div>
                                                <div class="flex items-center justify-between mb-2">
                                                    <h4 class="text-sm font-bold text-white">{macro.title}</h4>
                                                    <button 
                                                        on:click={() => toggleMacro(macro.id)}
                                                        class="px-4 py-1.5 rounded-xl text-xs font-semibold transition-all {macro.running ? 'bg-rose-500/20 border border-rose-500/40 text-rose-400 hover:bg-rose-500/30' : 'bg-sky-500/20 border border-sky-500/40 text-sky-400 hover:bg-sky-500/30'}"
                                                    >
                                                        {macro.running ? 'STOP' : 'START'}
                                                    </button>
                                                </div>
                                                <p class="text-xs text-slate-400 leading-relaxed mb-4">{macro.desc}</p>
                                            </div>

                                            {#if macro.options && macro.options.length > 0}
                                                <div class="flex items-center justify-between pt-3 border-t border-slate-800/80">
                                                    <span class="text-xs text-slate-500 font-medium">Target Mode:</span>
                                                    <select 
                                                        value={macro.target} 
                                                        on:change={(e) => onTargetChange(macro.id, e.target.value)}
                                                        class="bg-slate-950 border border-slate-800 text-xs text-slate-300 rounded-lg px-3 py-1 focus:outline-none focus:border-sky-500"
                                                    >
                                                        {#each macro.options as opt}
                                                            <option value={opt}>{opt}</option>
                                                        {/each}
                                                    </select>
                                                </div>
                                            {/if}
                                        </div>
                                    {/each}
                                </div>
                            </div>
                        {/if}

                        {#if currentTab === 'config' || currentTab === 'failsafe'}
                            {#each Object.entries(dynamicConfigSchema) as [catId, catObj]}
                                <div class="bg-slate-900/40 border border-slate-800/80 p-6 rounded-2xl">
                                    <h3 class="text-sm font-bold text-white mb-4 flex items-center gap-2">
                                        <span class="w-2 h-2 rounded-full bg-sky-400"></span>
                                        {catObj.name || catId}
                                    </h3>

                                    <div class="grid grid-cols-2 gap-4">
                                        {#each catObj.settings || [] as setting}
                                            <div class="bg-slate-950/60 border border-slate-800/80 p-4 rounded-xl flex items-center justify-between gap-4">
                                                <div class="flex-1 pr-2">
                                                    <h4 class="text-xs font-semibold text-slate-200">{setting.name}</h4>
                                                    <p class="text-[11px] text-slate-500 leading-tight mt-0.5">{setting.desc}</p>
                                                </div>

                                                <div>
                                                    {#if setting.type === 'boolean'}
                                                        <label class="relative inline-flex items-center cursor-pointer">
                                                            <input 
                                                                type="checkbox" 
                                                                checked={setting.value} 
                                                                on:change={(e) => updateConfigValue(catId, setting.id, e.target.checked)}
                                                                class="sr-only peer"
                                                            />
                                                            <div class="w-9 h-5 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-sky-500"></div>
                                                        </label>
                                                    {:else if setting.type === 'slider'}
                                                        <div class="flex items-center gap-2">
                                                            <input 
                                                                type="range" 
                                                                min={setting.min || 0} 
                                                                max={setting.max || 100} 
                                                                step={setting.step || 1} 
                                                                value={setting.value} 
                                                                on:input={(e) => updateConfigValue(catId, setting.id, parseFloat(e.target.value))}
                                                                class="w-24 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-sky-500"
                                                            />
                                                            <span class="text-xs font-semibold text-sky-400 w-8 text-right">{setting.value}</span>
                                                        </div>
                                                    {:else if setting.type === 'dropdown'}
                                                        <select 
                                                            value={setting.value} 
                                                            on:change={(e) => updateConfigValue(catId, setting.id, parseInt(e.target.value))}
                                                            class="bg-slate-900 border border-slate-800 text-xs text-slate-300 rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-sky-500"
                                                        >
                                                            {#each setting.options || [] as opt, i}
                                                                <option value={i}>{opt}</option>
                                                            {/each}
                                                        </select>
                                                    {:else if setting.type === 'keybind'}
                                                        <button 
                                                            on:click={() => startKeybindListen(catId, setting)}
                                                            class="px-3 py-1 rounded-lg text-xs font-bold transition-all {activeKeybindListening && activeKeybindListening.settingId === setting.id ? 'bg-rose-500 text-white animate-pulse' : 'bg-slate-800 text-sky-400 hover:bg-slate-700'}"
                                                        >
                                                            {activeKeybindListening && activeKeybindListening.settingId === setting.id ? 'PRESS KEY' : getKeyName(setting.value)}
                                                        </button>
                                                    {:else}
                                                        <div class="flex items-center gap-2">
                                                            <input 
                                                                type="text" 
                                                                value={setting.value || ''} 
                                                                on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)}
                                                                class="w-28 bg-slate-900 border border-slate-800 text-xs text-slate-200 px-2.5 py-1.5 rounded-lg focus:outline-none focus:border-sky-500"
                                                            />
                                                            <button 
                                                                on:click={() => executeButtonAction(catId, setting.id + 'Button')}
                                                                title="Set from current held item"
                                                                class="px-2 py-1 bg-sky-500/20 hover:bg-sky-500/30 text-sky-400 border border-sky-500/40 rounded-lg text-xs font-semibold transition-all"
                                                            >
                                                                Hand
                                                            </button>
                                                        </div>
                                                    {/if}
                                                </div>
                                            </div>
                                        {/each}
                                    </div>
                                </div>
                            {/each}
                        {/if}

                        {#if currentTab === 'themes'}
                            <div class="space-y-4">
                                <h3 class="text-sm font-bold text-white mb-2">Select Accent Theme</h3>
                                <div class="grid grid-cols-3 gap-4">
                                    {#each themesList as theme}
                                        <button 
                                            on:click={() => applyTheme(theme.id)}
                                            class="bg-slate-900/50 border p-4 rounded-2xl text-left transition-all duration-200 {selectedTheme === theme.id ? 'border-sky-500 bg-sky-500/10' : 'border-slate-800 hover:border-slate-700'}"
                                        >
                                            <div class="flex items-center gap-2 mb-2">
                                                <div class="w-4 h-4 rounded-full" style="background-color: {theme.primaryColor};"></div>
                                                <h4 class="text-xs font-bold text-white">{theme.name}</h4>
                                            </div>
                                            <p class="text-[11px] text-slate-400">{theme.desc}</p>
                                        </button>
                                    {/each}
                                </div>
                            </div>
                        {/if}
                    </div>
                </main>
            </div>
        </div>
    {:else}
        <!-- MCEF Transparent Blurred Theme-Outlined Status HUD Overlay -->
        <div class="fixed top-5 left-5 z-50 p-4 rounded-2xl bg-slate-950/40 backdrop-blur-xl border-2 border-[var(--theme-500)] shadow-[0_0_25px_var(--theme-500)] text-white w-64 select-none transition-all duration-300">
            <div class="flex items-center justify-between mb-2.5 pb-2 border-b border-white/10">
                <div class="flex items-center gap-2">
                    <div class="w-2.5 h-2.5 rounded-full bg-[var(--theme-400)] animate-pulse"></div>
                    <h3 class="text-xs font-extrabold tracking-wider bg-gradient-to-r from-white to-[var(--theme-400)] bg-clip-text text-transparent uppercase">VERTEX STATUS</h3>
                </div>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full border {isRunning ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40' : 'bg-slate-800 text-slate-400 border-slate-700'}">
                    {isRunning ? 'ACTIVE' : 'IDLE'}
                </span>
            </div>
            
            <div class="space-y-2 text-xs font-medium">
                <div class="flex items-center justify-between text-slate-300">
                    <span class="text-slate-400">State:</span>
                    <span class="text-[var(--theme-400)] font-semibold">{activeMacroName} {#if subState}<span class="text-slate-300 font-normal">({subState})</span>{/if}</span>
                </div>
                <div class="flex items-center justify-between text-slate-300">
                    <span class="text-slate-400">Runtime:</span>
                    <span class="text-slate-100 font-mono">{macroRuntime}</span>
                </div>
                <div class="flex items-center justify-between text-slate-300">
                    <span class="text-slate-400">Profit:</span>
                    <span class="text-emerald-400 font-semibold">{estProfit}</span>
                </div>
                {#if farmingSpeed !== '0.0 BPS'}
                    <div class="flex items-center justify-between text-slate-300">
                        <span class="text-slate-400">Speed:</span>
                        <span class="text-amber-400 font-semibold">{farmingSpeed}</span>
                    </div>
                {/if}
            </div>
        </div>
    {/if}

</div>
