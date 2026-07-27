package com.vertexai.macro.impl.FarmBuilderMacro.config;

import java.util.List;

public class FarmPatternConfig {
    public String name;
    public int patternWidth;
    public boolean s_shape_turns;
    public List<ColumnConfig> columns;

    public static class ColumnConfig {
        public int offset;
        public List<String> tools;
    }
}
