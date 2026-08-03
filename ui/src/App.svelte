<script>
    import { onMount } from 'svelte';
    import './app.css';
    import SpotifyWidget from './lib/components/SpotifyWidget.svelte';

    let currentTab = 'farming';
    let searchQuery = '';
    let playerName = 'Player';
    let isConnected = true;
    let activeMacroName = 'None';
    let farmingSpeed = '0.0 BPS';
    let estProfit = '0 / hr';
    let liveFps = 0;
    let selectedFont = 'Outfit';
    let uiSoundStyle = 1;
    let selectedTheme = 'cyber';
    let uiScale = 1.0;
    
    // Failsafe State Variables (UI Mockup)
    let webhookUrl = '';
    let adminDetection = true;
    let autoDisconnect = true;
    let captchaAlert = true;
    let failsafeVolume = 80;
    
    // Allow Java to pass the native GUI scale so it defaults perfectly,
    // but respect user's saved preferences if they adjusted the slider
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
    }
    
    // Save preferences automatically when they change
    $: if (typeof window !== 'undefined') {
        localStorage.setItem('vertex_uiTheme', selectedTheme);
        localStorage.setItem('vertex_uiScale', uiScale.toString());
    }
    
    const fontsList = ['Inter', 'Outfit', 'Roboto', 'JetBrains Mono', 'Minecraft'];

    const themesList = [
        { id: 'cyber', name: 'Cyber Sky', accent: 'sky', bgGradient: 'from-sky-400 to-sky-600', primaryColor: '#38bdf8', desc: 'Futuristic slate dashboard with neon cyan & sky blue accents.' },
        { id: 'purple', name: 'Neon Purple', accent: 'purple', bgGradient: 'from-purple-500 to-violet-600', primaryColor: '#a855f7', desc: 'Vibrant dark synthwave layout with glowing violet highlights.' },
        { id: 'emerald', name: 'Emerald Garden', accent: 'emerald', bgGradient: 'from-emerald-400 to-teal-600', primaryColor: '#34d399', desc: 'Fresh green aesthetic tuned for Garden & Farming enthusiasts.' },
        { id: 'crimson', name: 'Crimson Peak', accent: 'rose', bgGradient: 'from-rose-500 to-red-600', primaryColor: '#f43f5e', desc: 'Aggressive high-contrast red & rose theme for Combat & Slayer.' },
        { id: 'gold', name: 'Gold Luxe', accent: 'amber', bgGradient: 'from-amber-400 to-yellow-600', primaryColor: '#fbbf24', desc: 'Premium gold & amber styling for Economy & Bazaar flippers.' },
        { id: 'rainbow', name: 'Chroma RGB', accent: 'rainbow', bgGradient: 'bg-gradient-to-r from-red-500 via-yellow-400 via-green-500 via-blue-500 to-purple-500', primaryColor: '#a855f7', desc: 'Dynamic RGB gamer theme with vibrant colors across the spectrum.' }
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
                let t400, t500, t600;
                if (theme.accent === 'sky') { t400 = '#38bdf8'; t500 = '#0ea5e9'; t600 = '#0284c7'; }
                else if (theme.accent === 'purple') { t400 = '#c084fc'; t500 = '#a855f7'; t600 = '#9333ea'; }
                else if (theme.accent === 'emerald') { t400 = '#34d399'; t500 = '#10b981'; t600 = '#059669'; }
                else if (theme.accent === 'rose') { t400 = '#fb7185'; t500 = '#f43f5e'; t600 = '#e11d48'; }
                else if (theme.accent === 'amber') { t400 = '#fbbf24'; t500 = '#f59e0b'; t600 = '#d97706'; }
                
                document.documentElement.style.setProperty('--theme-400', t400);
                document.documentElement.style.setProperty('--theme-500', t500);
                document.documentElement.style.setProperty('--theme-600', t600);
            }
        }
    }

    $: if (dynamicConfigSchema && dynamicConfigSchema.gui && dynamicConfigSchema.gui.settings) {
        const fontSetting = dynamicConfigSchema.gui.settings.find(s => s.id === 'guiFont');
        if (fontSetting && fontSetting.value !== undefined) {
            selectedFont = fontsList[parseInt(fontSetting.value)] || 'Outfit';
        }
        const soundSetting = dynamicConfigSchema.gui.settings.find(s => s.id === 'uiSoundStyle');
        if (soundSetting && soundSetting.value !== undefined) {
            uiSoundStyle = parseInt(soundSetting.value) || 0;
        }
    }

    function playUiSound() {
        if (uiSoundStyle === 0) return; // Muted
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            
            osc.connect(gain);
            gain.connect(ctx.destination);
            
            const now = ctx.currentTime;
            
            if (uiSoundStyle === 1) { // Mechanical Click
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(1200, now);
                osc.frequency.exponentialRampToValueAtTime(300, now + 0.02);
                gain.gain.setValueAtTime(0.15, now);
                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.02);
                osc.start(now);
                osc.stop(now + 0.02);
            } else if (uiSoundStyle === 2) { // Bubbly Pop
                osc.type = 'sine';
                osc.frequency.setValueAtTime(400, now);
                osc.frequency.exponentialRampToValueAtTime(900, now + 0.03);
                gain.gain.setValueAtTime(0.2, now);
                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.03);
                osc.start(now);
                osc.stop(now + 0.03);
            } else if (uiSoundStyle === 3) { // Subtle Chime
                osc.type = 'sine';
                osc.frequency.setValueAtTime(1400, now);
                gain.gain.setValueAtTime(0.08, now);
                gain.gain.exponentialRampToValueAtTime(0.001, now + 0.025);
                osc.start(now);
                osc.stop(now + 0.025);
            }
        } catch(e) {}
    }

    let activeMacroSettingsModal = null;

    // Map Svelte macro categories to Java config category IDs
    const categoryMapping = {
        'farming': ['farming', 'melonPumpkin', 'farmBuilder'],
        'mining': ['miningMacro', 'powderMacro', 'routeMiner', 'commission'],
        'fishing': ['fishing'],
        'slayer': ['combat'],
        'dungeons': ['dungeons'],
        'foraging': ['foraging'],
        'alchemy': ['bazaarFlipper', 'misc'],
        'diana': ['combat', 'misc']
    };

    function getMacroCategoryIds(macro) {
        if (!macro) return ['general'];
        if (categoryMapping[macro.category]) return categoryMapping[macro.category];
        if (categoryMapping[macro.id]) return categoryMapping[macro.id];
        return [macro.category, 'general'];
    }

    const macroSettingFilters = {
        'crop': { catIds: ['farming'], allowedFieldIds: ['farmingCrop', 'laneSpeed', 'autoTeleport'] },
        'sugarcane': { catIds: ['farming'], allowedFieldIds: ['farmingCrop', 'laneSpeed', 'autoTeleport'] },
        'melon_pumpkin': { catIds: ['melonPumpkin', 'farming'], allowedFieldIds: ['melonPumpkinCrop', 'laneSpeed'] },
        'farm_builder': { catIds: ['farmBuilder'], allowedFieldIds: ['presetLayout', 'autoClearPlots'] },
        'visitor': { catIds: ['farming'], allowedFieldIds: ['autoAcceptTrades', 'refuseUnprofitable'] },
        'pest_hunter': { catIds: ['farming'], allowedFieldIds: ['pestVacuum', 'pestBrokerTrade'] },

        'commission': { catIds: ['commission'], allowedFieldIds: ['commissionLocation', 'etherwarpPath'] },
        'gemstone': { catIds: ['routeMiner'], allowedFieldIds: ['routeFile', 'pickaxeSwap'] },
        'mining_general': { catIds: ['miningMacro'], allowedFieldIds: ['mineTarget', 'rotationSpeed', 'autoPickaxeAbility'] },
        'powder': { catIds: ['powderMacro'], allowedFieldIds: ['chestSolver', 'powderMining'] },
        'glacial': { catIds: ['miningMacro'], allowedFieldIds: ['glacialIce', 'shaftPathfinder'] },
        'nuker': { catIds: ['miningMacro'], allowedFieldIds: ['nukerRange', 'nukerFov'] },

        'slayer': { catIds: ['combat'], allowedFieldIds: ['slayerTarget', 'autoHealEnabled', 'healingItem', 'autoHealThreshold'] },
        'mob_killer': { catIds: ['combat'], allowedFieldIds: ['mobKillerTarget', 'autoHealEnabled', 'healingItem', 'autoHealThreshold'] },
        'zealot': { catIds: ['combat'], allowedFieldIds: ['zealotTarget', 'eyeAlert'] },
        'dungeon': { catIds: ['dungeons'], allowedFieldIds: ['dungeonFloor', 'secretFinder'] },
        'kuudra': { catIds: ['combat'], allowedFieldIds: ['autoHealEnabled', 'healingItem', 'autoHealThreshold'] },

        'fishing': { catIds: ['fishing'], allowedFieldIds: ['rodAutoCast', 'seaCreatureKill', 'bobberSensitivity'] },
        'trophy_fishing': { catIds: ['fishing'], allowedFieldIds: ['trophyFishHook', 'obfuscatedFillet'] },

        'foraging': { catIds: ['foraging'], allowedFieldIds: ['treeType', 'treecapitatorSwap'] },
        'alchemy': { catIds: ['misc'], allowedFieldIds: ['potionRecipe', 'batchSize'] },
        'flip': { catIds: ['bazaarFlipper'], allowedFieldIds: ['bazaarMargin', 'orderAutoUpdate'] },
        'diana': { catIds: ['combat'], allowedFieldIds: ['burrowFinder', 'inquisitorHunter'] }
    };

    function getMacroSettingsList(macro) {
        if (!macro || !dynamicConfigSchema) return [];
        
        let filter = macroSettingFilters[macro.id];
        let catIds = filter ? filter.catIds : (categoryMapping[macro.category] || [macro.category]);
        
        let result = [];
        for (let catId of catIds) {
            let catObj = dynamicConfigSchema[catId];
            if (catObj && catObj.settings) {
                for (let setting of catObj.settings) {
                    if (!filter || !filter.allowedFieldIds || filter.allowedFieldIds.length === 0 || filter.allowedFieldIds.includes(setting.id)) {
                        result.push({ catId, ...setting });
                    }
                }
            }
        }
        
        if (result.length === 0 && catIds.length > 0) {
            for (let catId of catIds) {
                let catObj = dynamicConfigSchema[catId];
                if (catObj && catObj.settings) {
                    for (let setting of catObj.settings) {
                        result.push({ catId, ...setting });
                    }
                }
            }
        }
        
        return result;
    }

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
        { id: 'slayer', category: 'slayer', title: 'Slayer Boss Macro', desc: 'Auto-spawns and slays Slayer bosses (Revenant, Tarantula, Sven, Voidgloom).', running: false, target: 'Revenant Horror', options: ['Revenant Horror', 'Tarantula Broodfather', 'Sven Packmaster', 'Voidgloom Seraph'] },
        { id: 'mob_killer', category: 'slayer', title: 'General Mob Killer', desc: 'Auto-pathfinds and grinds area mobs for EXP, drops, and bestiary.', running: false, target: 'Graveyard Zombies', options: ['Zealots', 'Ghosts', 'Ice Walkers', 'Treasure Hoarders', 'Goblins', 'Glacite Walkers', 'Automotons', 'Sludge', 'Yog', 'Graveyard Zombies'] },
        { id: 'zealot', category: 'slayer', title: 'End Zealot & Bruiser Farmer', desc: 'Auto-pathfinds and kills Zealots & Special Zealot Bruisers in The End.', running: false, target: 'Special Zealots' },
        { id: 'dungeon', category: 'slayer', title: 'Dungeons & Catacombs Solver', desc: 'Auto room clear, secret finder & boss room combat helper.', running: false, target: 'Catacombs Floor 7' },
        { id: 'kuudra', category: 'slayer', title: 'Kuudra Boss Suite', desc: 'Auto-Supply Rush, Cannon Build/Fueling, Pod Head Stun, and Core DPS.', running: false, target: 'Kuudra T5', options: ['Kuudra T1', 'Kuudra T2', 'Kuudra T3', 'Kuudra T4', 'Kuudra T5'] },

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

    const defaultConfigSchema = {
        gui: { id: 'gui', name: 'Themes & Styling', settings: [{ id: 'handChams', name: 'Hand Chams', desc: 'Render the held item with a solid, opaque glowing effect', type: 'boolean', value: false }, { id: 'chamsGlowAmount', name: 'Glow Amount', desc: 'Intensity of the hand chams glow', type: 'slider', min: 0.0, max: 2.0, step: 0.1, value: 1.0 }, { id: 'chamsGlowColor', name: 'Glow Color', desc: 'Hex color for the hand chams (e.g. #00FFFF)', type: 'text', value: '#00FFFF' }] },
        farming: { id: 'farming', name: 'Farming Settings', settings: [{ id: 'farmingCrop', name: 'Target Crop', desc: 'Select target crop', type: 'dropdown', options: ['Wheat', 'Carrot', 'Potato', 'Nether Wart', 'Sugar Cane', 'Cactus', 'Melon', 'Pumpkin'], value: 0 }, { id: 'laneSpeed', name: 'Walk Speed (BPS)', desc: 'Movement speed during farming', type: 'slider', min: 1, max: 400, step: 1, value: 250 }, { id: 'autoTeleport', name: 'Auto Spawn Teleport', desc: 'Warp back to plot start when lane ends', type: 'boolean', value: true }] },
        miningMacro: { id: 'miningMacro', name: 'Mining Settings', settings: [{ id: 'mineTarget', name: 'Target Ore', desc: 'Select ore type to prioritize', type: 'dropdown', options: ['Mithril', 'Titanium', 'Gemstones', 'Glacite'], value: 0 }, { id: 'rotationSpeed', name: 'Head Rotation Speed', desc: 'Camera movement speed (ms)', type: 'slider', min: 50, max: 500, step: 10, value: 150 }, { id: 'autoPickaxeAbility', name: 'Auto Speed Boost', desc: 'Use pickaxe ability on cooldown', type: 'boolean', value: true }] },
        combat: { id: 'combat', name: 'Combat & Slayer Settings', settings: [{ id: 'bossTarget', name: 'Slayer Target', desc: 'Select Slayer Boss tier', type: 'dropdown', options: ['Revenant T4', 'Revenant T5', 'Tarantula T4', 'Sven T4', 'Voidgloom T4'], value: 0 }, { id: 'autoWeaponSwap', name: 'Auto Weapon Swap', desc: 'Swap weapon when boss spawns', type: 'boolean', value: true }, { id: 'killAuraRange', name: 'Combat Range', desc: 'Maximum hit range in blocks', type: 'slider', min: 3, max: 6, step: 0.1, value: 4.5 }] },
        fishing: { id: 'fishing', name: 'Fishing Settings', settings: [{ id: 'rodAutoCast', name: 'Auto Recast Rod', desc: 'Recast rod automatically after catch', type: 'boolean', value: true }, { id: 'seaCreatureKill', name: 'Auto Slay Creatures', desc: 'Slay sea creatures instantly', type: 'boolean', value: true }] },
        foraging: { id: 'foraging', name: 'Foraging Settings', settings: [{ id: 'treeType', name: 'Tree Type', desc: 'Target wood type', type: 'dropdown', options: ['Dark Oak', 'Acacia', 'Jungle', 'Spruce'], value: 0 }, { id: 'treecapitatorSwap', name: 'Treecapitator Swap', desc: 'Auto swap axe before chop', type: 'boolean', value: true }] },
        general: { id: 'general', name: 'General Settings', settings: [{ id: 'autoFailSafe', name: 'Failsafe Protection', desc: 'Pause macro when admin check detected', type: 'boolean', value: true }] }
    };

    let dynamicConfigSchema = defaultConfigSchema;

    onMount(() => {
        fetchStatus();
        const interval = setInterval(fetchStatus, 2000);
        
        fetch('/api/config/schema')
            .then(res => res.json())
            .then(data => {
                if (data && typeof data === 'object' && Object.keys(data).length > 0) {
                    dynamicConfigSchema = { ...defaultConfigSchema, ...data };

                    // Two-way sync card targets with loaded Java config
                    if (data.combat && data.combat.settings) {
                        let mk = data.combat.settings.find(s => s.id === 'mobKillerTarget');
                        if (mk && mk.options) {
                            let card = macros.find(m => m.id === 'mob_killer');
                            let idx = parseInt(mk.value);
                            if (card && !isNaN(idx) && mk.options[idx]) {
                                card.target = mk.options[idx];
                            }
                        }
                        let sl = data.combat.settings.find(s => s.id === 'slayerTarget');
                        if (sl && sl.options) {
                            let card = macros.find(m => m.id === 'slayer');
                            let idx = parseInt(sl.value);
                            if (card && !isNaN(idx) && sl.options[idx]) {
                                card.target = sl.options[idx];
                            }
                        }
                        macros = [...macros];
                    }
                }
            })
            .catch(e => console.error(e));
        
        return () => clearInterval(interval);
    });

    function updateConfigValue(categoryId, fieldId, value) {
        if (fieldId === 'guiFont') {
            selectedFont = fontsList[parseInt(value)] || 'Outfit';
        }
        if (fieldId === 'uiSoundStyle') {
            uiSoundStyle = parseInt(value) || 0;
            playUiSound();
        }
        
        // Sync fine-tuning modal changes back to card dropdown state
        if (fieldId === 'mobKillerTarget' && dynamicConfigSchema.combat) {
            let setting = dynamicConfigSchema.combat.settings.find(s => s.id === 'mobKillerTarget');
            let card = macros.find(m => m.id === 'mob_killer');
            if (setting && setting.options && card) {
                let idx = parseInt(value);
                if (!isNaN(idx) && setting.options[idx]) {
                    card.target = setting.options[idx];
                    setting.value = idx;
                    macros = [...macros];
                }
            }
        }
        if (fieldId === 'slayerTarget' && dynamicConfigSchema.combat) {
            let setting = dynamicConfigSchema.combat.settings.find(s => s.id === 'slayerTarget');
            let card = macros.find(m => m.id === 'slayer');
            if (setting && setting.options && card) {
                let idx = parseInt(value);
                if (!isNaN(idx) && setting.options[idx]) {
                    card.target = setting.options[idx];
                    setting.value = idx;
                    macros = [...macros];
                }
            }
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
        let macro = macros.find(m => m.id === id);
        if (macro) {
            macro.target = target;
            macros = [...macros];
        }

        // Two-way sync card selection into dynamicConfigSchema modal value
        if (id === 'mob_killer' && dynamicConfigSchema.combat) {
            let setting = dynamicConfigSchema.combat.settings.find(s => s.id === 'mobKillerTarget');
            if (setting && setting.options) {
                let idx = setting.options.indexOf(target);
                if (idx !== -1) {
                    setting.value = idx;
                    updateConfigValue('combat', 'mobKillerTarget', idx);
                }
            }
        } else if (id === 'slayer' && dynamicConfigSchema.combat) {
            let setting = dynamicConfigSchema.combat.settings.find(s => s.id === 'slayerTarget');
            if (setting && setting.options) {
                let idx = setting.options.indexOf(target);
                if (idx !== -1) {
                    setting.value = idx;
                    updateConfigValue('combat', 'slayerTarget', idx);
                }
            }
        }

        if (window.cefQuery) {
            window.cefQuery({ request: `set_target:${id}:${target}` });
        }
        fetch(`/api/macro/target?id=${encodeURIComponent(id)}&target=${encodeURIComponent(target)}`).catch(() => {});
    }

    function openMacroSettings(macro) {
        activeMacroSettingsModal = macro;
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

    let activeKeybindListening = null;

    const glfwNames = {
        342: 'L-ALT',
        346: 'R-ALT',
        340: 'L-SHIFT',
        344: 'R-SHIFT',
        341: 'L-CTRL',
        345: 'R-CTRL',
        32: 'SPACE',
        256: 'NONE',
        257: 'ENTER',
        258: 'TAB',
        259: 'BACKSPACE'
    };

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

<div style="font-family: {selectedFont}, Inter, Roboto, sans-serif;" class="w-screen h-screen flex items-center justify-center bg-black/60 select-none overflow-hidden relative">
    <div style="transform: scale({uiScale}); transform-origin: center; transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);" class="w-[820px] h-[520px] bg-slate-900/98 border border-white/10 rounded-2xl shadow-[0_20px_40px_-10px_rgba(0,0,0,0.8),0_0_30px_rgba(56,189,248,0.25)] flex overflow-hidden relative">
    
    <!-- Sidebar Navigation -->
    <aside class="w-[210px] bg-slate-900/95 border-r border-white/10 p-4 flex flex-col justify-between shrink-0 select-none overflow-y-auto max-h-full custom-scrollbar">
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
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'farming' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'farming'; }}>
                    Farming & Garden
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'mining' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'mining'; }}>
                    Mining & Commissions
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'slayer' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'slayer'; }}>
                    Slayer & Combat
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'fishing' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'fishing'; }}>
                    Fishing
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'foraging' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'foraging'; }}>
                    Foraging
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'alchemy' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'alchemy'; }}>
                    Economy & Alchemy
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'diana' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'diana'; }}>
                    Diana Mythological
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'utilities' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'utilities'; }}>
                    Misc
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'settings' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'settings'; }}>
                    Settings & Config
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'failsafes' ? 'bg-gradient-to-r from-red-500/15 to-transparent border-l-2 border-red-500 text-red-500' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'failsafes'; }}>
                    Security & Failsafes
                </li>
                <!-- svelte-ignore a11y_no_noninteractive_element_interactions a11y_click_events_have_key_events a11y-click-events-have-key-events -->
                <li class="flex items-center gap-2 px-3 py-2 rounded-lg text-[11px] font-medium cursor-pointer transition-all duration-200 {currentTab === 'themes' ? 'bg-gradient-to-r from-sky-400/15 to-transparent border-l-2 border-sky-400 text-sky-400' : 'text-slate-400 hover:bg-white/5 hover:text-white hover:translate-x-0.5'}" on:click={() => { playUiSound(); currentTab = 'themes'; }}>
                    Themes & Styling
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

        <SpotifyWidget />
    </aside>

    <!-- Main Workspace View -->
    <main class="flex-1 p-5 overflow-y-auto custom-scrollbar flex flex-col gap-4 select-none">
        
        {#if currentTab !== 'settings' && currentTab !== 'themes' && currentTab !== 'failsafes'}
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
                <!-- svelte-ignore a11y_no_static_element_interactions a11y_click_events_have_key_events -->
                <div class="bg-slate-800/70 border border-white/10 rounded-xl p-3.5 flex flex-col justify-between gap-2.5 transition-all duration-300 hover:border-sky-400/40 hover:shadow-[0_8px_20px_rgba(0,0,0,0.3)] min-w-0 relative group {macro.expanded ? 'col-span-2 border-sky-400/60 bg-slate-900/95 shadow-[0_0_25px_rgba(56,189,248,0.2)]' : ''}" 
                     on:contextmenu|preventDefault={() => { playUiSound(); macro.expanded = !macro.expanded; macros = [...macros]; }}
                     on:auxclick={(e) => { if (e.button === 2) { playUiSound(); macro.expanded = !macro.expanded; macros = [...macros]; } }}
                     on:mousedown={(e) => { if (e.button === 2) { playUiSound(); macro.expanded = !macro.expanded; macros = [...macros]; } }}>
                    <div class="flex justify-between items-start gap-2">
                        <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-1.5 justify-between">
                                <div class="text-[12px] font-semibold text-white truncate flex-1">{macro.title}</div>
                                <button title="Configure Settings" class="text-slate-400 hover:text-sky-400 text-[12px] px-1.5 py-0.5 bg-white/5 rounded transition-colors cursor-pointer shrink-0" on:click|stopPropagation={() => { playUiSound(); macro.expanded = !macro.expanded; macros = [...macros]; }}>
                                    {macro.expanded ? 'Close' : 'Config'}
                                </button>
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
                            <button class="px-3 py-1 rounded-lg text-[10px] font-semibold bg-gradient-to-br from-red-500 to-red-600 text-white shadow-[0_2px_8px_rgba(239,68,68,0.3)] transition-all cursor-pointer whitespace-nowrap shrink-0" on:click|stopPropagation={() => { playUiSound(); toggleMacro(macro.id); }}>Stop Macro</button>
                        {:else}
                            <button class="px-3 py-1 rounded-lg text-[10px] font-semibold bg-gradient-to-br from-sky-400 to-sky-600 text-white shadow-[0_2px_8px_rgba(56,189,248,0.3)] transition-all cursor-pointer whitespace-nowrap shrink-0" on:click|stopPropagation={() => { playUiSound(); toggleMacro(macro.id); }}>Start Macro</button>
                        {/if}
                    </div>

                    <!-- Embedded Inline Settings Drawer (Renders on Right Click) -->
                    {#if macro.expanded}
                        <!-- svelte-ignore a11y_click_events_have_key_events -->
                        <div class="mt-2 pt-3 border-t border-white/10 flex flex-col gap-2.5" on:click|stopPropagation>
                            <div class="flex justify-between items-center mb-0.5">
                                <span class="text-[10px] font-bold text-sky-400 uppercase tracking-wider">{macro.title} Fine-Tuning</span>
                                <button class="text-[9px] text-slate-400 hover:text-white" on:click={() => { macro.expanded = false; macros = [...macros]; }}>✕ Close</button>
                            </div>

                            {#if dynamicConfigSchema}
                                <div class="grid grid-cols-2 gap-2 max-h-[220px] overflow-y-auto pr-1 custom-scrollbar">
                                    {#each getMacroSettingsList(macro) as setting}
                                        <div class="bg-slate-950/80 p-2 rounded-lg border border-white/5 flex flex-col justify-between gap-1.5">
                                            <div class="text-[10px] font-medium text-slate-200 truncate">{setting.name}</div>
                                            
                                            {#if setting.type === 'boolean'}
                                                <div class="flex items-center gap-2">
                                                    <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.checked)} class="w-3.5 h-3.5 accent-sky-400 cursor-pointer" />
                                                    <span class="text-[9px] text-slate-300">{setting.value ? 'Enabled' : 'Disabled'}</span>
                                                </div>
                                            {:else if setting.type === 'slider'}
                                                <div class="flex items-center gap-1.5">
                                                    <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="flex-1 accent-sky-400 cursor-pointer h-1" />
                                                    <span class="text-[9px] text-sky-400 font-mono w-7 text-right">{setting.value}</span>
                                                </div>
                                            {:else if setting.type === 'text'}
                                                <input type="text" bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1 rounded text-[9px] outline-none w-full" />
                                            {:else if setting.type === 'dropdown'}
                                                <select bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-1.5 py-1 rounded text-[9px] outline-none w-full">
                                                    {#each setting.options as opt, i}
                                                        <option value={i}>{opt}</option>
                                                    {/each}
                                                </select>
                                            {:else if setting.type === 'keybind'}
                                                <button class="bg-slate-900 border border-white/10 text-slate-300 px-2 py-1 rounded text-[9px] w-full flex items-center justify-between font-mono" on:click={openConfigGui}>
                                                    <span>Rebind</span>
                                                    <span class="text-sky-400 font-bold bg-sky-500/10 px-1 rounded">{getKeyName(setting.value)}</span>
                                                </button>
                                            {/if}
                                        </div>
                                    {/each}
                                </div>
                            {/if}
                        </div>
                    {/if}
                </div>
                {/each}
            </div>
        {/if}

        {#if currentTab === 'utilities'}
            <div class="flex flex-col gap-3 pb-6">
                <div class="flex justify-between items-center mb-1">
                    <div>
                        <h2 class="text-sm font-bold text-sky-400">Misc & Client Utilities</h2>
                        <p class="text-[10px] text-slate-400 mt-0.5">Toggle and configure client utilities, auto-sprint, auto-buffs, and helper features.</p>
                    </div>
                    <button class="text-[10px] text-sky-400 font-semibold border border-sky-400/30 px-3 py-1.5 rounded-lg bg-sky-400/10 hover:bg-sky-400/20 transition-colors" on:click={openConfigGui}>Open In-Game Settings</button>
                </div>
                
                {#if dynamicConfigSchema && dynamicConfigSchema.utilities}
                    <div class="grid grid-cols-2 gap-3">
                    {#each dynamicConfigSchema.utilities.settings as setting}
                        <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex flex-col justify-between gap-3 h-full">
                            <div>
                                <h3 class="text-[12px] font-semibold text-white">{setting.name}</h3>
                                <p class="text-[10px] text-slate-400 mt-1 leading-tight">{setting.desc}</p>
                            </div>
                            <div class="flex justify-start">
                                {#if setting.type === 'boolean'}
                                    <div class="flex items-center gap-2 mt-1">
                                        <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue('utilities', setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                                        <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{setting.value ? 'Enabled' : 'Disabled'}</span>
                                    </div>
                                {/if}
                            </div>
                        </div>
                    {/each}
                    </div>
                {:else}
                    <div class="text-xs text-slate-400 text-center py-10">Loading utilities configuration schema from Java...</div>
                {/if}
            </div>
        {/if}

        {#if currentTab === 'settings'}
            <div class="flex flex-col gap-3 pb-6">
                <div class="flex justify-between items-center mb-1">
                    <h2 class="text-sm font-bold">Client & Interface Settings</h2>
                    <button class="text-[10px] text-sky-400 font-semibold border border-sky-400/30 px-3 py-1.5 rounded-lg bg-sky-400/10 hover:bg-sky-400/20 transition-colors" on:click={openConfigGui}>Open Full In-Game Config</button>
                </div>
                
                {#if dynamicConfigSchema}
                    {#each ['delays'] as catId}
                        {#if dynamicConfigSchema[catId]}
                            <div class="mt-3 mb-1 pl-1">
                                <h4 class="text-[11px] font-bold text-sky-400 uppercase tracking-wider">{dynamicConfigSchema[catId].name}</h4>
                            </div>
                            <div class="grid grid-cols-2 gap-3">
                            {#each dynamicConfigSchema[catId].settings as setting}
                                <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex flex-col justify-between gap-3 h-full">
                                    <div>
                                        <h3 class="text-[12px] font-semibold text-white">{setting.name}</h3>
                                        <p class="text-[10px] text-slate-400 mt-1 leading-tight">{setting.desc}</p>
                                    </div>
                                    <div class="flex justify-start">
                                        {#if setting.type === 'boolean'}
                                            <div class="flex items-center gap-2 mt-1">
                                                <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                                                <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{setting.value ? 'Enabled' : 'Disabled'}</span>
                                            </div>
                                        {:else if setting.type === 'slider'}
                                            <div class="flex items-center gap-3 w-full">
                                                <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="flex-1 accent-sky-400 cursor-pointer" />
                                                <span class="text-[10px] text-sky-400 font-mono w-10 text-right bg-slate-900/80 px-2 py-1 rounded border border-white/5">{setting.value}</span>
                                            </div>
                                        {:else if setting.type === 'text'}
                                            <input type="text" bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-3 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-sky-500/50 transition-colors" />
                                        {:else if setting.type === 'dropdown'}
                                            <select bind:value={setting.value} on:change={(e) => updateConfigValue(catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-sky-500/50 transition-colors">
                                                {#each setting.options as opt, i}
                                                    <option value={i}>{opt}</option>
                                                {/each}
                                            </select>
                                        {:else if setting.type === 'keybind'}
                                            <button class="bg-slate-900 border border-white/10 text-slate-300 px-3 py-1.5 rounded-lg text-[10px] w-full hover:text-white hover:border-sky-500/50 transition-colors shadow-sm font-mono flex items-center justify-between" on:click={openConfigGui}>
                                                <span>Change Keybind</span>
                                                <span class="text-sky-400 font-bold bg-sky-500/10 border border-sky-400/20 px-2 py-0.5 rounded">
                                                    {getKeyName(setting.value)}
                                                </span>
                                            </button>
                                        {/if}
                                    </div>
                                </div>
                            {/each}
                            </div>
                        {/if}
                    {/each}
                {:else}
                    <div class="text-xs text-slate-400 text-center py-10">Loading configuration schema from Java...</div>
                {/if}
            </div>
        {/if}

        {#if currentTab === 'failsafes'}
            <div class="flex flex-col gap-3 pb-6">
                <div class="flex justify-between items-center mb-1">
                    <div>
                        <h2 class="text-sm font-bold text-red-400">Security & Failsafes Hub</h2>
                        <p class="text-[10px] text-slate-400 mt-0.5">Configure advanced macro protection, webhook alerts, and auto-disconnect parameters.</p>
                    </div>
                </div>

                <!-- Webhooks -->
                <div class="bg-slate-800/70 border border-red-500/20 rounded-xl p-3.5 mt-1 flex flex-col gap-2">
                    <span class="text-xs font-bold text-white">Discord Webhook URL</span>
                    <input type="text" bind:value={webhookUrl} placeholder="https://discord.com/api/webhooks/..." class="bg-slate-900 border border-white/10 text-white px-3 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-red-500/50 transition-colors font-mono" />
                    <p class="text-[9px] text-slate-400 leading-tight">If set, Vertex will send live screenshots and ping you if a staff member teleports to your island, or if you are warped into limbo.</p>
                </div>

                <!-- Toggles -->
                <div class="grid grid-cols-2 gap-3 mt-2">
                    <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col justify-between gap-3">
                        <div>
                            <h3 class="text-[12px] font-semibold text-white">Admin Detection</h3>
                            <p class="text-[9px] text-slate-400 mt-1 leading-tight">Instantly halts all macros and pings webhook if a staff member enters render distance.</p>
                        </div>
                        <div class="flex items-center gap-2">
                            <input type="checkbox" bind:checked={adminDetection} class="w-4 h-4 accent-red-500 cursor-pointer" />
                            <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{adminDetection ? 'Enabled' : 'Disabled'}</span>
                        </div>
                    </div>
                    
                    <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col justify-between gap-3">
                        <div>
                            <h3 class="text-[12px] font-semibold text-white">Auto-Disconnect</h3>
                            <p class="text-[9px] text-slate-400 mt-1 leading-tight">Automatically disconnects from the server immediately after an admin is detected.</p>
                        </div>
                        <div class="flex items-center gap-2">
                            <input type="checkbox" bind:checked={autoDisconnect} class="w-4 h-4 accent-red-500 cursor-pointer" />
                            <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{autoDisconnect ? 'Enabled' : 'Disabled'}</span>
                        </div>
                    </div>

                    <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col justify-between gap-3">
                        <div>
                            <h3 class="text-[12px] font-semibold text-white">Captcha Alert</h3>
                            <p class="text-[9px] text-slate-400 mt-1 leading-tight">Plays a loud alarm sound and stops macroing if a Bedrock or map captcha appears.</p>
                        </div>
                        <div class="flex items-center gap-2">
                            <input type="checkbox" bind:checked={captchaAlert} class="w-4 h-4 accent-red-500 cursor-pointer" />
                            <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{captchaAlert ? 'Enabled' : 'Disabled'}</span>
                        </div>
                    </div>

                    <!-- Volume Slider -->
                    <div class="bg-slate-800/70 border border-white/10 p-3 rounded-xl flex flex-col justify-between gap-3">
                        <div>
                            <h3 class="text-[12px] font-semibold text-white">Alert Volume</h3>
                            <p class="text-[9px] text-slate-400 mt-1 leading-tight">The volume of the in-game alarm sound played during an emergency failsafe event.</p>
                        </div>
                        <div class="flex items-center gap-2 w-full">
                            <input type="range" min="0" max="100" step="1" bind:value={failsafeVolume} class="flex-1 accent-red-500 cursor-pointer h-1.5 bg-slate-900 rounded-lg appearance-none" />
                            <span class="text-[10px] text-red-400 font-mono w-8 text-right bg-slate-900/80 px-1 py-0.5 rounded border border-red-500/20">{failsafeVolume}%</span>
                        </div>
                    </div>
                </div>
            </div>
        {/if}

        {#if currentTab === 'themes'}
            <div class="flex flex-col gap-3 pb-6">
                <div class="flex justify-between items-center mb-1">
                    <div>
                        <h2 class="text-sm font-bold text-white">Color Themes & Visual Presets</h2>
                        <p class="text-[10px] text-slate-400 mt-0.5">Customize dashboard styling, accent colors, and glow effects in real time.</p>
                    </div>
                </div>

                <!-- UI Size Slider -->
                <div class="bg-slate-800/70 border border-white/10 rounded-xl p-3.5 mt-1 flex flex-col gap-2">
                    <div class="flex justify-between items-center">
                        <span class="text-xs font-bold text-white">Dashboard Scale</span>
                        <span class="text-[10px] text-sky-400 font-mono bg-sky-400/10 px-2 py-0.5 rounded border border-sky-400/20">{uiScale.toFixed(2)}x</span>
                    </div>
                    <div class="flex items-center gap-3">
                        <span class="text-[10px] text-slate-500 font-mono">0.5x</span>
                        <input type="range" min="0.5" max="2.0" step="0.05" bind:value={uiScale} class="flex-1 accent-sky-400 cursor-pointer h-1.5 bg-slate-900 rounded-lg appearance-none" />
                        <span class="text-[10px] text-slate-500 font-mono">2.0x</span>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-3 mt-2">
                    {#each themesList as theme}
                        <!-- svelte-ignore a11y_no_static_element_interactions a11y_click_events_have_key_events -->
                        <div class="bg-slate-800/70 border rounded-xl p-3.5 flex flex-col justify-between gap-3 transition-all duration-300 cursor-pointer relative group {selectedTheme === theme.id ? 'border-sky-400/80 bg-slate-800/95 shadow-[0_0_20px_rgba(56,189,248,0.25)]' : 'border-white/10 hover:border-white/20'}" on:click={() => applyTheme(theme.id)}>
                            <div class="flex justify-between items-start">
                                <div>
                                    <div class="flex items-center gap-2">
                                        <div class="w-3.5 h-3.5 rounded-full shadow-sm" style="background-color: {theme.primaryColor};"></div>
                                        <h3 class="text-xs font-bold text-white">{theme.name}</h3>
                                    </div>
                                    <p class="text-[10px] text-slate-400 mt-1 leading-snug">{theme.desc}</p>
                                </div>
                                {#if selectedTheme === theme.id}
                                    <span class="px-2 py-0.5 rounded-full text-[8px] font-bold uppercase bg-sky-500/20 text-sky-400 border border-sky-400/30 shrink-0">Active</span>
                                {/if}
                            </div>

                            <div class="flex items-center justify-between pt-2 border-t border-white/5">
                                <div class="w-full h-2 rounded-full bg-gradient-to-r {theme.bgGradient}"></div>
                            </div>
                        </div>
                    {/each}
                </div>

                <div class="flex justify-between items-center mt-4 mb-1">
                    <div>
                        <h2 class="text-sm font-bold text-white">Advanced Cosmetics</h2>
                        <p class="text-[10px] text-slate-400 mt-0.5">Customize hand chams and other visual settings.</p>
                    </div>
                </div>

                {#if dynamicConfigSchema && dynamicConfigSchema['gui'] && dynamicConfigSchema['gui'].settings}
                    <div class="grid grid-cols-2 gap-3 mt-1">
                    {#each dynamicConfigSchema['gui'].settings.filter(s => ['handChams', 'chamsGlowAmount', 'chamsGlowColor'].includes(s.id)) as setting}
                        <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex flex-col justify-between gap-3 h-full">
                            <div>
                                <h3 class="text-[12px] font-semibold text-white">{setting.name}</h3>
                                <p class="text-[10px] text-slate-400 mt-1 leading-tight">{setting.desc}</p>
                            </div>
                            <div class="flex justify-start">
                                {#if setting.type === 'boolean'}
                                    <div class="flex items-center gap-2 mt-1">
                                        <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue('gui', setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                                        <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{setting.value ? 'Enabled' : 'Disabled'}</span>
                                    </div>
                                {:else if setting.type === 'slider'}
                                    <div class="flex items-center gap-3 w-full">
                                        <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue('gui', setting.id, e.target.value)} class="flex-1 accent-sky-400 cursor-pointer" />
                                        <span class="text-[10px] text-sky-400 font-mono w-10 text-right bg-slate-900/80 px-2 py-1 rounded border border-white/5">{setting.value}</span>
                                    </div>
                                {:else if setting.type === 'dropdown'}
                                    <select bind:value={setting.value} on:change={(e) => updateConfigValue('gui', setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-sky-500/50 transition-colors">
                                        {#each setting.options as opt, i}
                                            <option value={i}>{opt}</option>
                                        {/each}
                                    </select>
                                {:else if setting.type === 'text'}
                                    <input type="text" bind:value={setting.value} on:change={(e) => updateConfigValue('gui', setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-sky-500/50 transition-colors" />
                                {/if}
                            </div>
                        </div>
                    {/each}
                    </div>
                {:else}
                    <div class="text-xs text-slate-400 text-center py-6">Loading hand chams settings from Java...</div>
                {/if}

                <div class="flex justify-between items-center mt-4 mb-1">
                    <div>
                        <h2 class="text-sm font-bold text-white">Animations</h2>
                        <p class="text-[10px] text-slate-400 mt-0.5">Customize swing animations, item positioning, and speed.</p>
                    </div>
                </div>

                {#if dynamicConfigSchema && dynamicConfigSchema['animations']}
                    <div class="grid grid-cols-2 gap-3 mt-1">
                    {#each dynamicConfigSchema['animations'].settings as setting}
                        <div class="bg-slate-800/70 border border-white/10 p-3.5 rounded-xl flex flex-col justify-between gap-3 h-full">
                            <div>
                                <h3 class="text-[12px] font-semibold text-white">{setting.name}</h3>
                                <p class="text-[10px] text-slate-400 mt-1 leading-tight">{setting.desc}</p>
                            </div>
                            <div class="flex justify-start">
                                {#if setting.type === 'boolean'}
                                    <div class="flex items-center gap-2 mt-1">
                                        <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue('animations', setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                                        <span class="text-[10px] text-slate-300 uppercase tracking-wide font-semibold">{setting.value ? 'Enabled' : 'Disabled'}</span>
                                    </div>
                                {:else if setting.type === 'slider'}
                                    <div class="flex items-center gap-3 w-full">
                                        <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue('animations', setting.id, e.target.value)} class="flex-1 accent-sky-400 cursor-pointer" />
                                        <span class="text-[10px] text-sky-400 font-mono w-10 text-right bg-slate-900/80 px-2 py-1 rounded border border-white/5">{setting.value}</span>
                                    </div>
                                {:else if setting.type === 'dropdown'}
                                    <select bind:value={setting.value} on:change={(e) => updateConfigValue('animations', setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1.5 rounded-lg text-[10px] outline-none w-full focus:border-sky-500/50 transition-colors">
                                        {#each setting.options as opt, i}
                                            <option value={i}>{opt}</option>
                                        {/each}
                                    </select>
                                {/if}
                            </div>
                        </div>
                    {/each}
                    </div>
                {:else}
                    <div class="text-xs text-slate-400 text-center py-6">Loading animation settings from Java...</div>
                {/if}
            </div>
        {/if}

    </main>

    {#if activeMacroSettingsModal}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div class="absolute inset-0 bg-slate-950/95 flex items-center justify-center p-6 z-50">
        <div class="w-[460px] bg-slate-900 border border-sky-500/30 rounded-2xl p-5 shadow-[0_20px_50px_rgba(0,0,0,0.85)] flex flex-col gap-4 relative z-50 text-white">
            <div class="flex justify-between items-start border-b border-white/10 pb-3">
                <div>
                    <h3 class="text-sm font-bold text-white">{activeMacroSettingsModal.title}</h3>
                    <p class="text-[10px] text-sky-400 font-mono mt-0.5">Per-Macro Configuration & Fine-Tuning</p>
                </div>
                <!-- svelte-ignore a11y_click_events_have_key_events -->
                <button class="text-slate-400 hover:text-white text-xs font-bold px-2 py-1 bg-white/5 rounded-lg cursor-pointer" on:click={closeMacroSettings}>✕</button>
            </div>

            <div class="flex flex-col gap-2.5 max-h-[360px] overflow-y-auto pr-2 custom-scrollbar">
                {#if dynamicConfigSchema}
                    {#each getMacroSettingsList(activeMacroSettingsModal) as setting}
                        <div class="flex justify-between items-center bg-slate-800/60 p-2.5 rounded-xl border border-white/5">
                            <div>
                                <div class="text-xs text-slate-200 font-medium">{setting.name}</div>
                                <div class="text-[9px] text-slate-400 max-w-[200px] leading-tight mt-0.5">{setting.desc}</div>
                            </div>
                            
                            {#if setting.type === 'boolean'}
                                <input type="checkbox" bind:checked={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.checked)} class="w-4 h-4 accent-sky-400 cursor-pointer" />
                            {:else if setting.type === 'slider'}
                                <div class="flex items-center gap-2">
                                    <span class="text-[10px] text-sky-400 font-mono w-8 text-right">{setting.value}</span>
                                    <input type="range" min={setting.min} max={setting.max} step={setting.step} bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="w-[100px] accent-sky-400 cursor-pointer" />
                                </div>
                            {:else if setting.type === 'text'}
                                <input type="text" bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1 rounded-lg text-[10px] outline-none w-[120px]" />
                            {:else if setting.type === 'dropdown'}
                                <select bind:value={setting.value} on:change={(e) => updateConfigValue(setting.catId, setting.id, e.target.value)} class="bg-slate-900 border border-white/10 text-white px-2 py-1 rounded-lg text-[10px] outline-none w-[120px]">
                                    {#each setting.options as opt, i}
                                        <option value={i}>{opt}</option>
                                    {/each}
                                </select>
                            {:else if setting.type === 'keybind'}
                                <button class="bg-slate-900 border border-white/10 text-slate-300 px-2 py-1 rounded-lg text-[10px] w-[140px] hover:text-white hover:border-sky-500/50 transition-colors flex items-center justify-between font-mono" on:click={openConfigGui}>
                                    <span>Change Keybind</span>
                                    <span class="text-sky-400 font-bold bg-sky-500/10 border border-sky-400/20 px-1.5 py-0.5 rounded">
                                        {getKeyName(setting.value)}
                                    </span>
                                </button>
                            {/if}
                        </div>
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
</div>
