package com.outlinemod.mixin;

import com.outlinemod.OutlineMod;

/**
 * Cek apakah VulkanMod aktif saat runtime.
 *
 * Kita pakai reflection supaya mod ini tetap bisa load
 * meskipun VulkanMod tidak terpasang (graceful degradation).
 */
public class VulkanRendererCheck {

    private static Boolean vulkanActive = null;

    public static boolean isVulkanActive() {
        if (vulkanActive != null) return vulkanActive;

        try {
            // Cek keberadaan kelas utama VulkanMod
            Class.forName("net.vulkanmod.vulkan.Vulkan");
            // Cek apakah renderer Vulkan benar-benar aktif
            Class<?> rendererClass = Class.forName("net.vulkanmod.vulkan.Renderer");
            Object instance = rendererClass.getMethod("getInstance").invoke(null);
            vulkanActive = instance != null;
        } catch (ClassNotFoundException e) {
            OutlineMod.LOGGER.info("[VulkanOutline] VulkanMod tidak ditemukan — berjalan dalam mode OpenGL biasa");
            vulkanActive = false;
        } catch (Exception e) {
            OutlineMod.LOGGER.warn("[VulkanOutline] Gagal cek VulkanMod: {}", e.getMessage());
            vulkanActive = false;
        }

        return vulkanActive;
    }

    /** Reset cache (dipanggil saat world reload) */
    public static void reset() {
        vulkanActive = null;
    }
}
