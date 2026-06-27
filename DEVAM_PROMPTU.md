# ARKEOSCAN PHONE — DEVAM PROMPTU

Bu dosya, ArkeoScan Phone projesine bir sonraki oturumda (Claude veya başka bir
asistanla) devam edebilmek için hazırlanmıştır. Yeni bir sohbette bu dosyanın
içeriğini yapıştırıp "buradan devam et" diyebilirsin.

## PROJE DURUMU (özet)

Sıfırdan, tam bir Android Gradle multi-module projesi olarak inşa edildi:
68 Kotlin dosyası, ~5700 satır kod, 10 modül (`app` + 9 `core-*`).

Tüm modüller **statik olarak** doğrulandı (brace/paren dengesi, paket/dizin
uyumu, version-catalog tutarlılığı, iç import bütünlüğü — 184 sembol).
**Gerçek `./gradlew assembleDebug` bu oturumda ÇALIŞTIRILAMADI** çünkü
kullanılan sandbox ortamı `services.gradle.org` ve `dl.google.com`'a erişime
izin vermiyor (network allowlist dışı). Yani:

- Kod yapısal olarak tutarlı, ama **derleyici hatalarına karşı garantili değil**.
- İlk açılışta Android Studio'da muhtemelen küçük tip/import düzeltmeleri gerekecek.
- `gradle/wrapper/gradle-wrapper.jar` repoda yok, ama elle eklemen gerekmiyor:
  `.github/workflows/android-ci.yml` içindeki adım, jar yoksa onu GitHub
  Actions runner'ında otomatik üretiyor. Lokal derleme için (Android Studio
  dışında) `gradle wrapper --gradle-version 8.9` çalıştırman gerekir.

## YAPILMASI GEREKEN İLK ADIMLAR (bir sonraki oturumda)

1. Projeyi Android Studio'da aç (veya Termux'a `git clone` + `gradle wrapper`).
2. Gradle Sync çalıştır, çıkan hataları tek tek düzelt (büyük ihtimalle: Compose
   BOM versiyon uyumsuzlukları, Maps Compose API imza farklılıkları — bu
   kütüphaneler hızlı güncellenir, `libs.versions.toml`'daki versiyonlar
   yazıldığı tarihte (bu oturumda) doğruydu ama kontrol edilmeli).
3. `app/src/main/AndroidManifest.xml`'deki `YOUR_GOOGLE_MAPS_API_KEY_HERE`
   placeholder'ını gerçek bir key ile değiştir.
4. Gerçek cihazda (Xiaomi/HyperOS, Termux ADB workflow'un zaten var) Walk Scan
   akışını test et: START → yürü → STOP → Polygon oluşuyor mu kontrol et.

## MİMARİ KARARLAR (neden böyle yapıldı)

- **Modüler yapı**: Her sensör/analiz/render katmanı kendi Gradle modülünde,
  böylece ileride "harici manyetometre desteği" veya "harici GPR desteği"
  eklemek (orijinal promptun "gelecekte eklenebilecek modüller" listesi)
  yalnızca yeni bir `core-*` modülü + DI binding gerektirir, mevcut kodu
  bozmaz.
- **InterpolationStrategy arayüzü**: IDW şu an tek implementasyon, ama Kriging
  eklenmek istendiğinde sadece `KrigingInterpolation : InterpolationStrategy`
  yazmak yeterli; `GridGenerator` değişmez.
- **AreaAnalysisViewModel Activity-scoped**: Heatmap/Contour/3D Surface
  ekranları aynı grid'i görsün diye `hiltViewModel(activity)` ile paylaşılıyor.
  Bu, "Area Analysis" ekranında bir kez analiz çalıştırılıp sonucun tüm
  görselleştirme ekranlarına yansımasını sağlıyor.
- **Polygon-maskeleme her katmanda tekrar kontrol ediliyor**: GridGenerator
  (`isInsidePolygon` flag), HeatmapMeshBuilder (mesh'e dahil etmeme),
  ContourBuilder (4 köşesi de polygon içinde olmayan kareyi atlama),
  Surface3DMeshBuilder (tamamen dışarıdaysa quad atlama) — "Polygon dışındaki
  hiçbir veri analiz edilmeyecek / Polygon dışı görünmesin" gereksinimi
  birden fazla katmanda savunmacı şekilde uygulandı.
- **Bilimsel dürüstlük / etik kısıt**: `AnomalyResult.DISCLAIMER` sabiti ve
  `AnomalyShape` enum'u kasıtlı olarak "oda/tünel/lahit" gibi kesin yapısal
  iddialar İÇERMİYOR — orijinal promptun ÖNEMLİ KISITLAR bölümü kod seviyesinde
  (veri modeli + UI + PDF/KML export) tutarlı şekilde uygulandı. Bu davranışı
  değiştirme (örn. "kesin oda tespit edildi" gibi bir etiket ekleme) önerilmez.

## BİLİNEN AÇIKLAR / SONRAKİ GENİŞLEME NOKTALARI

| Konu | Şu an durum | Genişletme noktası |
|---|---|---|
| Kriging | Yok | `InterpolationStrategy` implement et |
| Ayarların kalıcılığı | In-memory | Jetpack DataStore ekle (`AppSettings` zaten hazır) |
| Renkli contour | Tek renk çizgi | `ContourRenderer`'da isovalue→renk map'le |
| 3D Surface algoritması | Height-mapped grid triangulation | Gerçek hacimsel veri gelirse (örn. GPR derinlik katmanları) marching cubes'a geçilebilir — Kayser AreaScan'deki implementasyon referans alınabilir |
| Step-based kayıt | Zaman/mesafe bazlı (1s / 1m) | `core-motion/StepDetector` zaten var, WalkScanViewModel'e entegre edilip "her adımda kayıt" tam literal olarak uygulanabilir |
| Runtime izin akışı | MainActivity'de toplu istek | Ekran-bazlı (örn. Walk Scan START'a basılınca GPS izni kontrolü) akışa geçilebilir |
| Bulut senkronizasyonu, çoklu kullanıcı, RTK GPS, LiDAR | Yok (orijinal promptta "gelecekte eklenebilir" olarak işaretli) | Yeni `core-*` modülleri |

## İLGİLİ DİĞER PROJELER (bağlam için)

Bu proje, Hasan'ın halihazırda geliştirdiği şu projelerden bağımsız ama
kavramsal olarak ilişkilidir:
- **Kayser AreaScan** (`com.kayser.areascan`) — native Android, OpenGL ES 2.0,
  Marching Cubes isosurface, boustrophedon stepped-scan.
- **ArkeoSAR Ground Scan** (`com.arkeoscan.groundscan`) — Thuban Lodestar
  reverse-engineering'den port edilen detector ekranı.
- **ArkeoMag** (Flask+HTML/JS, Termux) — benzer bir tarama aracının web sürümü.

ArkeoScan Phone bunlardan farklı olarak **Compose+MVVM+Hilt ile sıfırdan**,
GPS+manyetometre+kamera+ivmeölçer+jiroskobu **tek bir entegre saha aracı**
olarak birleştiren, ayrı bir uygulamadır (paket adı: `com.arkeoscan.phone`).
