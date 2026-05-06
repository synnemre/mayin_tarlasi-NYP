# 🫘 Mayın Tarlası — Leblebi Tarlası

> Mehmet Emmi'nin leblebi tarlasını yılanlardan temizle! Klasik Mayın Tarlası oyununun Türkçe, köy-temalı ve çok katmanlı indie yorumu.

---

## 📸 Ekran Görüntüleri

> Oyunu çalıştırıp bir ekran görüntüsü ekleyebilirsin buraya.

---

## 🎮 Oyun Modları

### 🟫 Klasik Mod
Standart Mayın Tarlası deneyimi. Bayrakları yerleştir, mayınlardan kaçın, alanı temizle.  
- **Zamanlı mod:** Geri sayım başlar, süre bitince tüm mayınlar patlar.  
- **Kronometreli mod:** Süren ölçülür ve skor tablosuna kaydedilir.

### 🫘 Leblebi Tarlası Modu
Mehmet Emmi'nin yılan dolu leblebi tarlasını temizlediğin özel mod. Mayınlar = Yılanlar, Bayraklar = Çubuklar.

**Özellikler:**
- 🫀 Can sistemi — Yılana bastığında can kaybedersin, oyun devam eder
- ⏳ Geri sayım süresi — Her bölümün sınırlı süresi var
- 💰 Puan & Altın ayrımı — Altın bölümler arasında kalıcıdır
- 🌟 Altın Leblebi — Gizli altın leblebiler +10 altın kazandırır

---

## 🗺️ Bölümler

| # | İsim | Izgara | Yılan | Süre |
|---|------|--------|-------|------|
| 1 | Evin Arka Bahçesi | 8×8 | 9 | 120s |
| 2 | Ana Tarla | 10×10 | 15 | 180s |
| 3 | Bereketli Topraklar 🌙 | 15×15 | 35 | 240s |

> **🌙 Gece Modu:** 3. bölümde (ve ilerleyen her 4. bölümde) gece karanlığı çöker. Fare imlecin etrafında fener ışığı yanar, diğer her yer sisin altında kalır.

---

## 🛒 Market — Mehmet Emmi'nin Dükkanı

Market itemleri **altınla** alınır. Altın bölümler arasında **kalıcıdır**.

| İtem | Fiyat | Efekt |
|------|-------|-------|
| 🐦 Karga | 20 💰 | Yılan konumlarını gösterir |
| ⏰ Emmi'nin Saati | 30 💰 | +30 saniye ekler |
| 🧪 Zirai İlaç | 50 💰 | Bir alandaki yılanları yok eder |
| ❤️ Ekstra Kalp | 100 💰 | +1 can ekler |
| 🎕 Fener Genişlet | 15 💰 | Gece modunda fener yarıçapını artırır *(sadece gece bölümlerinde)* |

### ⬆️ Item Upgrade Sistemi
Aynı item ne kadar çok kullanılırsa o kadar güçlenir:

**Zirai İlaç:**
- Seviye 1 → 3×3 alan temizler
- Seviye 2 → 5×5 alan temizler
- Seviye 3 → Çapraz (Cross) tüm satır/sütun temizler

**Karga:**
- Seviye 1 → 1 rastgele yılan gösterir
- Seviye 2 → 3 rastgele yılan gösterir
- Seviye 3 → En yakın yılanı gösterir

---

## 📋 Görev Sistemi

Her bölümde 3 rastgele görev verilir. Tamamlayanlar **puan + altın** kazanır:

| Görev | Ödül |
|-------|------|
| 10 Hücre Aç | +50 Puan, +5 Altın |
| İlaçla 3 Yılan Avla | +80 Puan, +8 Altın |
| Karga Kullanma | +100 Puan, +10 Altın |
| Sürenin Yarısında Bitir | +150 Puan, +15 Altın |

---

## 🏆 Gizli Başarımlar

Bölüm sonunda otomatik kontrol edilir:

| Başarım | Koşul |
|---------|-------|
| **Körü Körüne** | Hiç item kullanmadan kazan |
| **Aceleci Emmi** | 30 saniye veya daha az sürede kazan |
| **Sağlam Yürek** | 3 can kaybedip yine de kazan |
| **Leblebi Ustası** | 200+ puan ile bitir |

---

## 📊 Hasat Raporu

Bölüm sonunda animasyonlu bir "fiş" ekranı açılır:
- Zaman bonusu, açılan hücreler, yok edilen yılanlar ayrı ayrı gösterilir
- Tamamlanan görevler listelenir
- Kazanılan gizli başarımlar gösterilir
- Mehmet Emmi puanına göre yorum yapar 😄

---

## 🏗️ Teknik Yapı

```
src/
├── MinesweeperApp.java     # Ana JavaFX uygulaması (UI, event handling)
├── Board.java              # Oyun tahtası mantığı (hücre açma, flood-fill)
├── Cell.java               # Tek hücre veri modeli
├── KlasikBoardMode.java    # Klasik mod yöneticisi
├── LeblebiBoardMode.java   # Leblebi modu yöneticisi (can, puan, görev, diyalog)
├── Seviye.java             # Bölüm tanımları
└── SkorTablosu.java        # Skor kayıt/yükleme sistemi (JSON)
```

**Platform:** Java + JavaFX  
**Derleme:** OpenJFX (modüler)

---

## 🔧 Kurulum & Çalıştırma

### Gereksinimler
- Java 17+ (BellSoft Liberica veya OpenJDK)
- JavaFX (`/usr/share/openjfx/lib` dizininde kurulu)

### Derleme
```bash
javac --module-path /usr/share/openjfx/lib \
      --add-modules javafx.controls,javafx.media \
      -d bin src/*.java
```

### Çalıştırma
```bash
java --module-path /usr/share/openjfx/lib \
     --add-modules javafx.controls,javafx.media \
     -cp bin MinesweeperApp
```

---

## 💡 Notlar

- Skor tablosu `~/.local/share/mayintarlasi/` klasörüne JSON olarak kaydedilir
- Leblebi modunda kazanılan altınlar bölümler arasında **kalıcıdır**, oyunu kapatsanız da bir sonraki oyun başlayışına kadar korunur
- Gece modu otomatik olarak 3. bölümde aktif olur (ve her 4. bölümde tekrar)

---

## 👴 Mehmet Emmi Kimdir?

Leblebi tarlasının sahibi, eski usul ama çok bilge bir köylü. Oyun boyunca köşede belirir ve ne yapmanız gerektiğini size söyler — iyi ya da kötü günde. Çok fazla can kaybederseniz biraz kızgın olabilir, ama kazandığınızda gözleri dolar. 🥹

---

*Made with ❤️ and lots of leblebi.*