package com.vertexai.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent UI_CLICK_1 = registerSoundEvent("ui.click.1");
    public static final SoundEvent UI_CLICK_2 = registerSoundEvent("ui.click.2");
    public static final SoundEvent UI_CLICK_3 = registerSoundEvent("ui.click.3");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("vertexai", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        // Calling this loads the class and registers the sounds
    }
}
