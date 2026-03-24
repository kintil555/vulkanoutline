# Vulkan Outline Mod
### Outline post-processing shader untuk VulkanMod di Minecraft 1.21.11

---

## Cara Install (Tidak Perlu Coding!)

### Yang kamu butuhkan:
1. **Minecraft Java Edition 1.21.11**
2. **Fabric Loader 0.18.1** → https://fabricmc.net/use/installer/
3. **VulkanMod 0.6.1** → https://www.curseforge.com/minecraft/mc-mods/vulkanmod
4. **Fabric API** untuk 1.21.11 → https://modrinth.com/mod/fabric-api
5. **File `vulkan-outline-1.0.0.jar`** (dari folder ini setelah di-compile)

### Langkah install:
1. Install Fabric Loader dulu via installer-nya
2. Masuk ke folder `.minecraft/mods/`
   - Windows: `%appdata%\.minecraft\mods\`
   - Linux/Mac: `~/.minecraft/mods/`
3. Taruh semua `.jar` di situ:
   - `VulkanMod_1.21.11-0.6.1.jar`
   - `fabric-api-xxx.jar`
   - `vulkan-outline-1.0.0.jar` ← mod ini
4. Jalankan Minecraft dengan profil **Fabric 1.21.11**
5. Di game: **Options → Video Settings → Graphics: Fast** (jangan Fancy/Fabulous)

---

## Cara Compile Sendiri

Butuh: **Java JDK 21** dan **internet** (Gradle download otomatis)

```bash
# Clone atau ekstrak folder ini, lalu:
./gradlew build

# File .jar akan muncul di:
build/libs/vulkan-outline-1.0.0.jar
```

Windows: ganti `./gradlew` dengan `gradlew.bat`

---

## Kustomisasi Warna Outline

Edit file: `src/main/resources/assets/outlinemod/shaders/post/outline.json`

Cari bagian `OutlineColor`:
```json
"value": [0.0, 0.0, 0.0, 1.0]
```
Format: `[R, G, B, Alpha]` — nilai 0.0 sampai 1.0

Contoh warna:
- Hitam: `[0.0, 0.0, 0.0, 1.0]`
- Putih: `[1.0, 1.0, 1.0, 1.0]`
- Merah: `[1.0, 0.0, 0.0, 1.0]`
- Oranye: `[1.0, 0.5, 0.0, 1.0]`

Untuk ketebalan outline, edit `OutlineThickness` (default: 1.5):
```json
"value": [1.5]
```

---

## Troubleshooting

**Crash saat load:**
- Pastikan VulkanMod 0.6.1 sudah terpasang
- Pastikan Fabric API sudah terpasang
- Cek versi Minecraft benar-benar 1.21.11

**Outline tidak muncul:**
- Graphics setting harus **Fast**, bukan Fabulous
- GPU harus support Vulkan 1.2+

**Performa drop:**
- Kurangi `OutlineThickness` ke 1.0
- Naikkan `DepthThreshold` ke 0.0005
