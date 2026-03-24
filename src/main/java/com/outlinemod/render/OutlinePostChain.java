package com.outlinemod.render;

import com.outlinemod.OutlineMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/**
 * Mengelola outline post-processing chain untuk 1.21.11.
 *
 * API yang berubah dari versi lama:
 * - PostChain.load() sekarang terima PostChainConfig bukan ResourceManager
 * - chain.resize() dihapus — resize otomatis
 * - chain.process() sekarang terima RenderPass consumer
 * - Minecraft.getTimer() → tidak ada, pakai partialTick dari event context
 * - RenderTarget.bindWrite() → bindWrite() masih ada tapi lewat getMainRenderTarget()
 */
public class OutlinePostChain {

    private static OutlinePostChain INSTANCE;

    private PostChain chain = null;
    private boolean loadAttempted = false;

    private OutlinePostChain() {}

    public static OutlinePostChain getInstance() {
        if (INSTANCE == null) INSTANCE = new OutlinePostChain();
        return INSTANCE;
    }

    private void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;

        try {
            Minecraft mc = Minecraft.getInstance();

            // 1.21.11: PostChain.load(ResourceLocation) — tidak perlu ResourceManager lagi
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                OutlineMod.MOD_ID,
                "shaders/post/outline"  // tanpa ekstensi .json
            );

            chain = PostChain.load(loc, mc.getMainRenderTarget());
            OutlineMod.LOGGER.info("[VulkanOutline] Outline post-chain berhasil diload");

        } catch (Exception e) {
            OutlineMod.LOGGER.error("[VulkanOutline] Gagal load outline post-chain: {}", e.getMessage());
            chain = null;
        }
    }

    /**
     * Dipanggil tiap frame dari WorldRenderEvents.AFTER_TRANSLUCENT.
     * partialTick dikirim dari context.tickCounter().getGameTimeDeltaPartialTick(true)
     */
    public void renderIfActive(float partialTick) {
        if (!RenderSystem.isOnRenderThread()) return;

        ensureLoaded();
        if (chain == null) return;

        try {
            // 1.21.11: process() terima RenderPass consumer
            chain.process(renderPass -> {});

            // Kembalikan ke main framebuffer setelah post-processing
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

        } catch (Exception e) {
            OutlineMod.LOGGER.error("[VulkanOutline] Error saat render outline: {}", e.getMessage());
        }
    }

    public void reload() {
        if (chain != null) { chain.close(); chain = null; }
        loadAttempted = false;
    }

    public void close() {
        if (chain != null) { chain.close(); chain = null; }
    }
}
