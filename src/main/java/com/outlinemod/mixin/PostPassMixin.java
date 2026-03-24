package com.outlinemod.mixin;

import com.outlinemod.OutlineMod;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin ini menyelesaikan masalah utama VulkanMod + post-processing:
 *
 * Di OpenGL:  driver handle sinkronisasi texture secara implisit — tidak perlu apa-apa.
 * Di Vulkan:  semua sinkronisasi HARUS eksplisit:
 *   1. Active render pass WAJIB di-end sebelum mulai pass baru
 *   2. Image layout WAJIB di-transisi dari ATTACHMENT ke SHADER_READ_ONLY
 *      sebelum bisa di-sample sebagai texture input
 *
 * VulkanMod sudah punya PostPassM di internal mereka untuk passes vanilla,
 * tapi tidak mencakup custom passes dari resource pack/mod.
 * Mixin ini menambal celah tersebut.
 *
 * Catatan: kalau VulkanMod tidak terpasang, mixin ini tetap aman —
 * VulkanRendererCheck memastikan kode Vulkan hanya jalan kalau VulkanMod ada.
 */
@Mixin(PostPass.class)
public class PostPassMixin {

    /**
     * Inject tepat sebelum PostPass membuat command encoder
     * (yaitu sesaat sebelum pass mulai menggambar).
     *
     * Di sinilah kita perlu:
     * - End active Vulkan render pass
     * - Transisi semua input texture ke layout SHADER_READ_ONLY
     */
    @Inject(
        method = "process",
        at = @At("HEAD")
    )
    private void onProcessHead(float partialTick, CallbackInfo ci) {
        if (!VulkanRendererCheck.isVulkanActive()) return;

        try {
            VulkanBridge.endCurrentRenderPassAndTransitionInputs((PostPass)(Object)this);
        } catch (Exception e) {
            OutlineMod.LOGGER.warn("[VulkanOutline] PostPassMixin warning: {}", e.getMessage());
        }
    }

    /**
     * Inject setelah pass selesai render untuk restore state.
     */
    @Inject(
        method = "process",
        at = @At("RETURN")
    )
    private void onProcessReturn(float partialTick, CallbackInfo ci) {
        if (!VulkanRendererCheck.isVulkanActive()) return;

        try {
            VulkanBridge.onPassComplete();
        } catch (Exception e) {
            // Ignore — return path tidak kritis
        }
    }
}
