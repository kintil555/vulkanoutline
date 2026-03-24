package com.outlinemod.render;

import com.outlinemod.OutlineMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/**
 * Mengelola outline post-processing chain.
 *
 * Cara kerja:
 * 1. Load PostChain dari file JSON di assets/outlinemod/shaders/post/outline.json
 * 2. Tiap frame setelah translucent render, panggil renderIfActive()
 * 3. PostPassMixin akan intercept tiap pass dan pastikan
 *    Vulkan render pass di-end + image layout di-transition sebelum sampling
 */
public class OutlinePostChain {

    private static OutlinePostChain INSTANCE;

    private PostChain chain = null;
    private boolean loadAttempted = false;
    private int screenWidth = 0;
    private int screenHeight = 0;

    private OutlinePostChain() {}

    public static OutlinePostChain getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OutlinePostChain();
        }
        return INSTANCE;
    }

    /**
     * Load (atau reload) chain jika belum ada atau resolusi layar berubah.
     */
    private void ensureLoaded() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        // Reload kalau resolusi berubah
        if (chain != null && (w != screenWidth || h != screenHeight)) {
            chain.close();
            chain = null;
            loadAttempted = false;
        }

        if (loadAttempted) return;
        loadAttempted = true;

        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                OutlineMod.MOD_ID,
                "shaders/post/outline.json"
            );
            chain = PostChain.load(
                mc.getResourceManager(),
                mc.getMainRenderTarget(),
                loc
            );
            chain.resize(w, h);
            screenWidth = w;
            screenHeight = h;
            OutlineMod.LOGGER.info("[VulkanOutline] Outline post-chain berhasil diload ({}x{})", w, h);
        } catch (Exception e) {
            OutlineMod.LOGGER.error("[VulkanOutline] Gagal load outline post-chain: {}", e.getMessage());
            chain = null;
        }
    }

    /**
     * Dipanggil tiap frame. Jalankan outline chain kalau sudah siap.
     */
    public void renderIfActive() {
        if (!RenderSystem.isOnRenderThread()) return;

        ensureLoaded();

        if (chain == null) return;

        try {
            chain.process(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
            // Kembalikan render target ke main framebuffer
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        } catch (Exception e) {
            OutlineMod.LOGGER.error("[VulkanOutline] Error saat render outline: {}", e.getMessage());
        }
    }

    /**
     * Reload chain (dipanggil kalau window resize).
     */
    public void reload() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        loadAttempted = false;
    }

    /**
     * Bersihkan resources saat mod di-unload.
     */
    public void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
    }
}
