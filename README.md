# 💣 Mayın Tarlası

Klasik mayın tarlası oyununun üç farklı modda sunulduğu, JavaFX tabanlı masaüstü oyunu.

---

## 🎮 Oyun Modları

### ⛏ Klasik Mayın Tarlası
Herkesin bildiği klasik mayın tarlası deneyimi. Kronometre veya geri sayımlı zamanlı mod seçenekleri mevcut.

- 6 hazır zorluk seviyesi (Kolay / Orta / Zor, her biri zamanlı versiyonuyla birlikte)
- Özel ızgara boyutu ve mayın sayısı ayarlanabilir (7×7 ile 30×30 arası)
- İlk tıklamada mayına basılmaması garantili güvenli bölge sistemi
- Zamanlı modda skor tablosuna kayıt

### ♟ Satranç Mayın Tarlası
Her hamlenin ardından taşların hareket ettiği dinamik bir mayın tarlası modu.

- 9×9 tahta, 3 zorluk seviyesi
- **Kolay:** Piyon (♟) + At (♞)
- **Orta:** Fil (♝) + Kale (♜)
- **Zor:** Yalnızca Vezir (♛)
- Her güvenli açışın ardından tüm taşlar satranç kurallarına göre hareket eder
- Tehdit sayıları anlık olarak güncellenir
- Fildişi ve koyu kahve satranç tahtası görseli

### 🫘 Mehmet Emmi'nin Leblebi Tarlası *(Gizli Mod)*
Oyun ana ekranında `1837837` kodunu klavyeden girerek açılır.

- 3 bölüm: Evin Arka Bahçesi → Ana Tarla → Bereketli Topraklar
- Can sistemi: Yılana basmak oyunu bitirmez, can düşürür
- Altın para birimi ve market sistemi
- **Market ürünleri:**
  - 🐦 **Karga** (20 💰) — Rastgele yılan konumlarını gösterir; seviye arttıkça daha fazlasını gösterir
  - ⏰ **Emmi'nin Saati** (30 💰) — 30 saniye ekler
  - 🧪 **Zirai İlaç** (50 💰) — Bir sonraki tıklamada 3×3 (veya daha geniş) alanı temizler
  - ❤️ **Ekstra Kalp** (100 💰) — +1 can
- Görev sistemi (hızlı bitir, karga kullanma, çoklu hücre aç vb.)
- Gizli başarımlar (Körü Körüne, Aceleci Emmi, Sağlam Yürek, Leblebi Ustası)
- Hasat raporu ve Mehmet Emmi'nin konuşma balonları
- Gece modu (3. bölümde fare konumuna göre fener efekti)

---

## 🏆 Skor Tablosu

Tamamlanan oyunlar `scores.json` dosyasına kaydedilir. Skor tablosunda üç ayrı sekme bulunur:

| Sekme | Açıklama |
|---|---|
| ⏱ Klasik (Zamanlı) | Geri sayımlı klasik mod sonuçları |
| ♟ Satranç Modu | Satranç moduna özel sıralama |
| 🫘 Leblebi Tarlası | Gizli mod aktifken görünür |

---

## 🛠 Gereksinimler

- **Java 17** veya üzeri
- **JavaFX 17** veya üzeri

---

## 🚀 Çalıştırma

```bash
# JavaFX modül yolunu ayarlayarak derle
javac --module-path /path/to/javafx/lib \
      --add-modules javafx.controls,javafx.media \
      *.java

# Çalıştır
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.media \
     MinesweeperApp
```

---

## 🎨 Tema & Arayüz

- **Karanlık tema** — Midnight Indigo (varsayılan)
- **Açık tema** — Soft Lavender
- **Leblebi teması** — Golden Harvest (otomatik, gizli mod aktifken)
- **Satranç teması** — Ivory & Obsidian (otomatik, satranç modunda)

Klasik modda sağ üstteki ☀/★ butonu ile temalar arasında geçiş yapılabilir.

---

## 🕹 Kontroller

| Eylem | Tuş / Tıklama |
|---|---|
| Hücre aç | Sol tık |
| Bayrak dik / kaldır | Sağ tık |
| Oyunu sıfırla | 🌾 / ♟ / 😊 butonu |
| Menüye dön | ← Menü butonu |
| Gizli modu aç | `1837837` (menü ekranında) |

---

## 📜 Lisans

Bu proje eğitim amaçlı geliştirilmiştir.
