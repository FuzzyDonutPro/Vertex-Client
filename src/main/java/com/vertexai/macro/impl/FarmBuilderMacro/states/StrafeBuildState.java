package com.vertexai.macro.impl.FarmBuilderMacro.states;

import com.vertexai.Vertex;
import com.vertexai.macro.AbstractMacro;
import com.vertexai.macro.impl.FarmBuilderMacro.BuilderToolUtil;
import com.vertexai.macro.impl.FarmBuilderMacro.config.FarmPatternConfig;
import com.vertexai.macro.impl.FarmBuilderMacro.config.FarmPatternManager;
import com.vertexai.util.AngleUtil;
import com.vertexai.util.KeyBindUtil;
import com.vertexai.util.Logger;
import com.vertexai.util.helper.Clock;
import net.minecraft.core.BlockPos;

import java.util.List;

public class StrafeBuildState extends FarmBuilderState {

    private final Clock delayClock = new Clock();
    private FarmPatternConfig pattern;
    
    private BlockPos startPos;
    private int lastBlockX;
    private int distanceStrafed = 0;
    
    private boolean isExecutingColumn = false;
    private int currentToolIndex = 0;
    private List<String> currentTools = null;

    public StrafeBuildState(AbstractMacro macro) {
        super(macro);
    }

    @Override
    public String getName() {
        return "StrafeBuild";
    }

    @Override
    public void onEnable() {
        String patternName = Vertex.config().farmBuilder.patternName;
        // Fallback or exact match loading
        if (patternName.endsWith(".json")) {
            // Find by filename mapping? FarmPatternManager uses "name" field from JSON.
            // Let's just find the pattern by iterating or looking it up.
            // For now, if the user types a filename, we can search by checking if name matches.
            // To keep it simple, we load by name or just grab the first one if not found.
            // We should reload config first just in case.
            FarmPatternManager.load();
        }
        
        // Search for matching name, or just grab the exact matching JSON.
        // Wait, the map key in FarmPatternManager is `config.name` not the filename.
        // The user config types filename. Let's just grab the first pattern if name doesn't match perfectly.
        this.pattern = FarmPatternManager.patterns.values().stream()
            .filter(p -> p.name.equalsIgnoreCase(patternName) || (patternName.contains("cane") && p.name.toLowerCase().contains("cane")))
            .findFirst()
            .orElse(FarmPatternManager.patterns.values().iterator().next());
            
        if (this.pattern == null) {
            Logger.sendError("§c[FarmBuilder] No JSON pattern found! Macro stopping.");
            macro.toggle();
            return;
        }
        
        Logger.sendMessage("§a[FarmBuilder] Starting Strafe Build using pattern: " + this.pattern.name);
        startPos = mc.player.blockPosition();
        lastBlockX = startPos.getX();
        distanceStrafed = 0;
        
        // Lock camera straight down the Z axis, slightly angled down for placements
        if (mc.player != null) {
            mc.player.setYRot(0f);
            mc.player.setXRot(15f);
        }
        delayClock.schedule(500);
    }

    @Override
    public void onDisable() {
        KeyBindUtil.stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        // Maintain exact camera angle
        mc.player.setYRot(0f);
        mc.player.setXRot(15f);

        if (!delayClock.passed()) return;

        if (isExecutingColumn) {
            executeColumnTools();
            return;
        }

        // We are moving to the next block
        int currentX = mc.player.getBlockX();
        if (currentX != lastBlockX) {
            distanceStrafed += Math.abs(currentX - lastBlockX);
            lastBlockX = currentX;
            
            // Stop to execute tools
            KeyBindUtil.stopMovement();
            
            int offset = distanceStrafed % pattern.patternWidth;
            FarmPatternConfig.ColumnConfig colConfig = getColumnConfig(offset);
            
            if (colConfig != null && colConfig.tools != null && !colConfig.tools.isEmpty()) {
                currentTools = colConfig.tools;
                currentToolIndex = 0;
                isExecutingColumn = true;
                delayClock.schedule(Vertex.config().farmBuilder.buildDelay);
                return;
            }
        }

        // Have we reached 96 blocks?
        if (distanceStrafed >= 95) { // 96 width means 0 to 95
            KeyBindUtil.stopMovement();
            Logger.sendMessage("§a[FarmBuilder] Finished full plot width!");
            if (pattern.s_shape_turns) {
                macro.getStateMachine().transitionTo(new CarveTurnsState(macro, startPos, distanceStrafed));
            } else {
                macro.toggle();
            }
            return;
        }

        // Continue strafing right
        KeyBindUtil.setKeyBindState(mc.options.keyRight, true);
    }
    
    private void executeColumnTools() {
        if (currentToolIndex >= currentTools.size()) {
            // Done with this column
            isExecutingColumn = false;
            delayClock.schedule(Vertex.config().farmBuilder.buildDelay);
            return;
        }
        
        String tool = currentTools.get(currentToolIndex);
        if (BuilderToolUtil.equipTool(tool)) {
            // Send right click
            KeyBindUtil.rightClick();
            Logger.sendMessage("§b[FarmBuilder] Used " + tool + " at offset " + (distanceStrafed % pattern.patternWidth));
        } else {
            Logger.sendError("§c[FarmBuilder] Could not find tool: " + tool);
        }
        
        currentToolIndex++;
        delayClock.schedule(Vertex.config().farmBuilder.buildDelay + 100);
    }
    
    private FarmPatternConfig.ColumnConfig getColumnConfig(int offset) {
        for (FarmPatternConfig.ColumnConfig c : pattern.columns) {
            if (c.offset == offset) return c;
        }
        return null;
    }
}
