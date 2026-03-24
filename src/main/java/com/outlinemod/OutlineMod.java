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
        LOGGER.info("[VulkanOutline] Loaded untuk Minecraft 1.21.11 + VulkanMod");

        // WorldRenderEvents.AFTER_TRANSLUCENT masih tersedia di 1.21.11
        // tapi context-nya pakai tickCounter bukan tickDelta
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            OutlinePostChain.getInstance().renderIfActive(
                context.tickCounter().getGameTimeDeltaPartialTick(true)
            );
        });
    }
}
