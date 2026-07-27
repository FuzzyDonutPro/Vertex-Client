package com.vertexai.macro.impl.FarmBuilderMacro;

public enum CropType {
    FLAT("Wheat/Carrot/Potato", 0),
    SUGAR_CANE("Sugar Cane", 1),
    MELON_PUMPKIN("Melon/Pumpkin", 2),
    CACTUS("Cactus", 3),
    COCOA("Cocoa Beans", 4);

    private final String name;
    private final int configId;

    CropType(String name, int configId) {
        this.name = name;
        this.configId = configId;
    }

    public String getName() {
        return name;
    }

    public int getConfigId() {
        return configId;
    }

    public static CropType fromId(int id) {
        for (CropType type : values()) {
            if (type.configId == id) {
                return type;
            }
        }
        return FLAT;
    }
}
