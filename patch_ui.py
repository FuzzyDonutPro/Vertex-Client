import os
import re

app_svelte_path = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\ui\src\App.svelte'

with open(app_svelte_path, 'r', encoding='utf-8') as f:
    app_svelte = f.read()

# 1. Imports and variables
if 'import MiningHUD from' not in app_svelte:
    app_svelte = app_svelte.replace(
        "    import SpotifyWidget from './lib/components/SpotifyWidget.svelte';",
        "    import SpotifyWidget from './lib/components/SpotifyWidget.svelte';\n    import MiningHUD from './lib/components/MiningHUD.svelte';"
    )

if 'let isConfigOpen = false;' not in app_svelte:
    app_svelte = app_svelte.replace(
        "    let currentTab = 'farming';",
        "    let isConfigOpen = false;\n    let miningTarget = 'Searching...';\n    let miningSpeed = 2000;\n    let miningHudEnabled = false;\n\n    let currentTab = 'farming';"
    )

# 2. Window bindings
if 'window.setConfigState' not in app_svelte:
    app_svelte = app_svelte.replace(
        "        if (savedScale) uiScale = parseFloat(savedScale);",
        "        if (savedScale) uiScale = parseFloat(savedScale);\n\n        window.setConfigState = (state) => {\n            isConfigOpen = state;\n        };\n"
    )

# 3. get_status payload
if 'data.isConfigScreenOpen !== undefined' not in app_svelte:
    app_svelte = app_svelte.replace(
        "if(data.fps !== undefined) liveFps = data.fps;",
        "if(data.fps !== undefined) liveFps = data.fps;\n                              if(data.miningTarget !== undefined) miningTarget = data.miningTarget;\n                              if(data.miningSpeed !== undefined) miningSpeed = data.miningSpeed;\n                              if(data.miningHudEnabled !== undefined) miningHudEnabled = data.miningHudEnabled;\n                              if(data.isConfigScreenOpen !== undefined) isConfigOpen = data.isConfigScreenOpen;"
    )

# 4. Wrap the HTML
if 'bg-black/60' in app_svelte and 'isConfigOpen ?' not in app_svelte:
    app_svelte = app_svelte.replace(
        '''<div style="font-family: {selectedFont}, Inter, Roboto, sans-serif;" class="w-screen h-screen flex items-center justify-center bg-black/60 select-none overflow-hidden relative">''',
        '''<div style="font-family: {selectedFont}, Inter, Roboto, sans-serif;" class="w-screen h-screen flex items-center justify-center {isConfigOpen ? 'bg-black/60' : 'bg-transparent pointer-events-none'} select-none overflow-hidden relative">\n{#if isConfigOpen}'''
    )

if '{#if !isConfigOpen && miningHudEnabled}' not in app_svelte:
    # Add closing tags safely
    if '    {/if}\n</div>' in app_svelte:
        app_svelte = app_svelte.replace(
            '''    {/if}\n</div>''',
            '''    {/if}\n    </div>\n{/if}\n{#if !isConfigOpen && miningHudEnabled}\n    <MiningHUD isRunning={activeMacroName === 'Mining Macro' && isRunning} runtime={runtime} target={miningTarget} speed={miningSpeed} themeColorClass={themeMap[selectedTheme]?.borderColor || 'border-sky-400'} themeGradientClass={themeMap[selectedTheme]?.buttonGradient || 'from-sky-400 to-sky-600'} />\n{/if}\n</div>'''
        )

with open(app_svelte_path, 'w', encoding='utf-8') as f:
    f.write(app_svelte)

print('App.svelte modified successfully.')
