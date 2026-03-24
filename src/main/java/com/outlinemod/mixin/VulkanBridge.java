package com.outlinemod.mixin;

import com.outlinemod.OutlineMod;
import net.minecraft.client.renderer.PostPass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Bridge ke VulkanMod internal menggunakan reflection.
 *
 * Kenapa reflection? Karena kita compile dengan VulkanMod sebagai
 * compileOnly dependency — artinya class VulkanMod ada saat compile,
 * tapi kita tetap butuh null-safety saat runtime kalau versiFnya berubah.
 *
 * Dua operasi utama yang kita lakukan:
 *
 * 1. endRenderPass()
 *    VulkanMod menggunakan "active render pass" yang harus diakhiri
 *    secara eksplisit sebelum kita bisa sampling texture yang baru saja
 *    digunakan sebagai color attachment.
 *
 * 2. readOnlyLayout() pada setiap input texture
 *    Vulkan image harus ada di layout SHADER_READ_ONLY_OPTIMAL
 *    sebelum bisa dipakai sebagai sampler input.
 *    VulkanMod punya method ini di VulkanImage.
 */
public class VulkanBridge {

    // Cache reflection references supaya tidak lookup tiap frame
    private static Method endRenderPassMethod = null;
    private static Method getInstanceMethod = null;
    private static Field inputsField = null;
    private static boolean reflectionFailed = false;

    private static void initReflection() {
        if (reflectionFailed || endRenderPassMethod != null) return;

        try {
            // Renderer.getInstance().endRenderPass()
            Class<?> rendererClass = Class.forName("net.vulkanmod.vulkan.Renderer");
            getInstanceMethod = rendererClass.getMethod("getInstance");
            endRenderPassMethod = rendererClass.getMethod("endRenderPass");

            OutlineMod.LOGGER.info("[VulkanOutline] VulkanBridge reflection berhasil diinisialisasi");
        } catch (Exception e) {
            OutlineMod.LOGGER.error("[VulkanOutline] VulkanBridge reflection gagal: {}", e.getMessage());
            reflectionFailed = true;
        }
    }

    /**
     * End active Vulkan render pass, lalu transisi semua input
     * texture dari PostPass ke layout SHADER_READ_ONLY.
     */
    public static void endCurrentRenderPassAndTransitionInputs(PostPass pass) {
        initReflection();
        if (reflectionFailed || endRenderPassMethod == null) return;

        try {
            // 1. End active render pass
            Object rendererInstance = getInstanceMethod.invoke(null);
            if (rendererInstance != null) {
                endRenderPassMethod.invoke(rendererInstance);
            }

            // 2. Transisi input textures ke SHADER_READ_ONLY
            // PostPass.inputs adalah list of RenderTarget yang menjadi sampler inputs
            transitionInputTextures(pass);

        } catch (Exception e) {
            OutlineMod.LOGGER.warn("[VulkanOutline] endRenderPass gagal: {}", e.getMessage());
        }
    }

    /**
     * Transisi semua input texture ke SHADER_READ_ONLY layout.
     * Ini diperlukan karena sebelum pass dijalankan, texture ini
     * mungkin masih ada di COLOR_ATTACHMENT layout.
     */
    private static void transitionInputTextures(PostPass pass) {
        try {
            // Dapatkan daftar inputs dari PostPass
            if (inputsField == null) {
                // Field 'inputs' di PostPass — nama bisa berbeda tergantung mappings
                // Coba beberapa kemungkinan nama
                for (Field f : pass.getClass().getDeclaredFields()) {
                    if (f.getType() == List.class) {
                        f.setAccessible(true);
                        inputsField = f;
                        break;
                    }
                }
            }

            if (inputsField == null) return;

            List<?> inputs = (List<?>) inputsField.get(pass);
            if (inputs == null) return;

            for (Object input : inputs) {
                transitionSingleTexture(input);
            }

        } catch (Exception e) {
            // Non-fatal, hanya log debug level
            OutlineMod.LOGGER.debug("[VulkanOutline] transitionInputTextures: {}", e.getMessage());
        }
    }

    /**
     * Transisi satu texture input ke SHADER_READ_ONLY via VulkanImage.readOnlyLayout()
     */
    private static void transitionSingleTexture(Object renderTargetInput) {
        try {
            // Input adalah RenderTarget atau wrapper-nya
            // Coba dapatkan VulkanImage dari chain:
            // renderTargetInput -> colorTexture -> getVulkanImage() -> readOnlyLayout()

            // Step 1: dapatkan colorTexture (GpuTexture)
            Object colorTexture = null;
            for (Field f : renderTargetInput.getClass().getDeclaredFields()) {
                if (f.getName().contains("color") || f.getName().contains("Color")) {
                    f.setAccessible(true);
                    colorTexture = f.get(renderTargetInput);
                    if (colorTexture != null) break;
                }
            }

            if (colorTexture == null) return;

            // Step 2: coba cast ke VkGpuTexture dan panggil getVulkanImage()
            Method getVulkanImage = colorTexture.getClass().getMethod("getVulkanImage");
            Object vulkanImage = getVulkanImage.invoke(colorTexture);

            if (vulkanImage == null) return;

            // Step 3: panggil readOnlyLayout() — ini yang melakukan layout barrier
            Method readOnlyLayout = vulkanImage.getClass().getMethod("readOnlyLayout");
            readOnlyLayout.invoke(vulkanImage);

        } catch (Exception e) {
            // Banyak input mungkin bukan VulkanImage — ini normal, abaikan
        }
    }

    /**
     * Dipanggil setelah pass selesai. Saat ini tidak ada yang perlu dilakukan,
     * tapi kita sisakan untuk extensibility.
     */
    public static void onPassComplete() {
        // Reserved untuk future use
    }
}
