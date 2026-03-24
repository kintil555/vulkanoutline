package com.outlinemod;

import com.outlinemod.render.OutlinePostChain;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutlineMod implements ClientModInitializer {

    public static final String MOD_ID = "vulkan-outline";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[VulkanOutline] Mod loaded untuk Minecraft 1.21.11 + VulkanMod 0.6.1");

        // Daftarkan event setelah world render selesai
        // supaya outline pass bisa dijalankan di atasnya
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            OutlinePostChain.getInstance().renderIfActive();
        });
    }
}
