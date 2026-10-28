# 🍽️ YTÜ Menü (Yemekhane Widget & Yardımcı Uygulama)

Samsung Galaxy One UI ve Android cihazlar için modern, sade YTÜ Yemekhane Menüsü widget'ı ve yardımcı uygulaması.

## ✨ Özellikler

- **One UI Uyumlu Jetpack Glance Widget:** Dinamik Material You renk desteği, yuvarlatılmış köşeler.
- **Akıllı Öğün Gösterimi:**
  - `00:00 – 11:30`: Öğle ve Akşam menüleri bir arada.
  - `11:30 – 14:30`: Öğle servisi açık etiketiyle birlikte menüler.
  - `14:30 – 17:00`: Öğle bittiğinde otomatik olarak **sadece Akşam** menüsü.
  - `17:00 – 19:15`: Akşam servisi açık etiketi.
  - `19:15 – 24:00`: Akşam servisi bittiğinde **yarının menüsü** ("Yarın" modu).
- **Hafta Sonu ve Resmi Tatil Farkındalığı:** Hafta sonları ve resmi tatillerde (2026–2027 sabit ve dini bayram takvimi) gereksiz ağ isteği atılmaz; kapalı bilgisi ve bir sonraki açılış tarihi gösterilir.
- **Çift Menü (Normal & Vejetaryen):** Normal menünün yanı sıra vejetaryen alternatifler daha küçük fontla widget üzerinde yer alır.
- **Aylık Menü Kontrolü ve Akıllı Önbellek (Room & DataStore):** Menü ay başında çekilip yerelde saklanır; yayınlanmamış aylar için günde en fazla 1 kez sorgu atılır.
- **Uygulama İçi Kendi Kendini Güncelleme (Self-Update):** GitHub Releases üzerinden yeni sürüm çıktığında doğrudan APK indirilir ve sistem paket yükleyicisi açılır.

## 🚀 GitHub Actions ile Otomatik Sürüm Yayınlama

Yeni bir sürüm yayınlamak için GitHub'da bir tag (örn. `v1.0.1`) oluşturup pushlamanız yeterlidir:

```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions arka planda APK'yı derler ve GitHub Releases sekmesine yükler. Cihazınızdaki uygulama yeni sürümü otomatik olarak algılayacaktır.
