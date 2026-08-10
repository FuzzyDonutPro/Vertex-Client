import os
import re

bridge_path = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\src\main\java\com\vertexai\gui\web\MCEFBridge.java'
screen_path = r'C:\Users\jerem\CLionProjects\Vertex-Client\Vertex-Client-1.0.6\src\main\java\com\vertexai\gui\VertexAIScreen.java'

with open(bridge_path, 'r', encoding='utf-8') as f:
    bridge = f.read()

# Add isConfigScreenOpen state variable
if 'public static boolean isConfigScreenOpen' not in bridge:
    bridge = bridge.replace(
        'public class MCEFBridge {',
        'public class MCEFBridge {\n\n    public static boolean isConfigScreenOpen = false;'
    )

# Safely inject mining stats into get_status payload
if 'miningTarget =' not in bridge:
    bridge = bridge.replace(
        'long s = elapsedSec % 60;\n                        runtimeStr = String.format("%02d:%02d:%02d", h, m, s);\n                    }\n                }',
        '''long s = elapsedSec % 60;\n                        runtimeStr = String.format("%02d:%02d:%02d", h, m, s);\n                    }\n                }
                
                String miningTarget = "Searching...";
                int miningSpeed = 2000;
                try {
                    com.vertexai.feature.impl.BlockMiner.BlockMiner miner = com.vertexai.feature.impl.BlockMiner.BlockMiner.getInstance();
                    if (miner != null) {
                        miningSpeed = miner.getMiningSpeed();
                        if (miner.getTargetBlockPos() != null && net.minecraft.client.Minecraft.getInstance().level != null) {
                            miningTarget = net.minecraft.client.Minecraft.getInstance().level.getBlockState(miner.getTargetBlockPos()).getBlock().getName().getString();
                        }
                    }
                } catch (Throwable t) {
                    miningTarget = "Error";
                }
                
                boolean miningHudEnabled = false;
                try {
                    miningHudEnabled = VertexClient.config != null && VertexClient.config.hud != null && VertexClient.config.hud.enableMiningHud;
                } catch (Throwable t) {}
'''
    )

if 'miningTarget' in bridge and 'miningSpeed' in bridge and 'isConfigScreenOpen' not in bridge.split('return String.format(')[-1]:
    # Replace the JSON payload
    bridge = re.sub(
        r'return String\.format\("\{\\"status\\":\\"ok\\",\\"playerName\\":\\"%s\\",\\"activeMacro\\":\\"%s\\",\\"subState\\":\\"%s\\",\\"runtime\\":\\"%s\\",\\"isRunning\\":%b,\\"bps\\":\\"%s\\",\\"estProfit\\":\\"%s\\",\\"fps\\":%d\}",\s*playerName, macroName, subState, runtimeStr, isRunning, bpsStr, estProfitStr, fps\);',
        '''return String.format("{\\"status\\":\\"ok\\",\\"playerName\\":\\"%s\\",\\"activeMacro\\":\\"%s\\",\\"subState\\":\\"%s\\",\\"runtime\\":\\"%s\\",\\"isRunning\\":%b,\\"bps\\":\\"%s\\",\\"estProfit\\":\\"%s\\",\\"fps\\":%d, \\"miningTarget\\":\\"%s\\", \\"miningSpeed\\":%d, \\"miningHudEnabled\\":%b, \\"isConfigScreenOpen\\":%b}",
                        playerName, macroName, subState, runtimeStr, isRunning, bpsStr, estProfitStr, fps, miningTarget, miningSpeed, miningHudEnabled, isConfigScreenOpen);''',
        bridge
    )

with open(bridge_path, 'w', encoding='utf-8') as f:
    f.write(bridge)

with open(screen_path, 'r', encoding='utf-8') as f:
    screen = f.read()

if 'MCEFBridge.isConfigScreenOpen = true;' not in screen:
    screen = screen.replace(
        'cefBrowser.resize(this.width, this.height);',
        'cefBrowser.resize(this.width, this.height);\n        com.vertexai.gui.web.MCEFBridge.isConfigScreenOpen = true;\n        if (cefBrowser != null && cefBrowser.getBrowser() != null) {\n            try {\n                cefBrowser.getBrowser().executeJavaScript("if(window.setConfigState) window.setConfigState(true);", "", 0);\n            } catch (Throwable t) {}\n        }'
    )

if 'MCEFBridge.isConfigScreenOpen = false;' not in screen:
    screen = screen.replace(
        'super.removed();',
        'super.removed();\n        com.vertexai.gui.web.MCEFBridge.isConfigScreenOpen = false;\n        if (cefBrowser != null && cefBrowser.getBrowser() != null) {\n            try {\n                cefBrowser.getBrowser().executeJavaScript("if(window.setConfigState) window.setConfigState(false);", "", 0);\n            } catch (Throwable t) {}\n        }'
    )

with open(screen_path, 'w', encoding='utf-8') as f:
    f.write(screen)

print('MCEFBridge and VertexAIScreen modified successfully.')
