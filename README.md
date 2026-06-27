# ArkeoScan Phone

Sahada yürüyerek manyetik/yüzey taraması yapan, profesyonel seviyede mimariye
sahip bir Android uygulaması. Kotlin + Jetpack Compose + Material 3 + MVVM +
Hilt + Room + Coroutines/Flow + OpenGL ES 2.0 ile yazılmıştır.

## Mimari

Çok modüllü Gradle yapısı (her modül tek bir sorumluluk taşır):

```
app/                  → Compose UI, Navigation, ViewModel'ler, Hilt wiring
core-common/          → Framework-bağımsız veri modelleri (GeoPoint, Polygon,
                         MagnetometerSample, GridCell, AnomalyResult, AppSettings...)
core-database/        → Room entity + DAO + Database + Hilt DatabaseModule
core-gps/             → LocationTracker (FusedLocationProvider), GpsOutlierFilter,
                         PolygonBuilder (convex-hull fallback'li)
core-motion/          → HeadingProvider (rotation vector sensörü), StepDetector
core-magnetometer/    → MagnetometerSensorManager, MagnetometerCalibrator
                         (baseline, peak-to-peak gürültü tabanı, deadzone, log-gain)
core-analysis/        → IdwInterpolation (Kriging için modüler arayüz: InterpolationStrategy),
                         GridGenerator (Polygon-maskeli grid üretimi),
                         AnomalyEngine (flood-fill clustering + sapma/devamlılık/
                         komşuluk/şekil/güven puanı)
core-renderer/        → OpenGL ES 2.0: HeatmapRenderer, ContourRenderer (Marching
                         Squares), Surface3DRenderer (height-mapped mesh + Gouraud
                         shading + directional light), ColorScaleMapper
core-camera/          → CameraX capture + EXIF GPS yazma/okuma
core-reports/         → PdfReportGenerator, CsvExporter, GeoJsonExporter,
                         KmlExporter, PngExporter, ReportRepository (facade)
```

### Veri akışı (özet)

1. **Walk Scan**: `LocationTracker` + `MagnetometerSensorManager` + `HeadingProvider`
   eşzamanlı çalışır → her GPS güncellemesinde `ScanPointEntity` Room'a yazılır.
2. **STOP**: `GpsOutlierFilter.clean()` + `removeZigzagNoise()` ile rota temizlenir →
   `PolygonBuilder.buildFromRoute()` ile Polygon oluşturulur → alan/çevre hesaplanır
   (Shoelace formülü, equirectangular yerel projeksiyon).
3. **Area Analysis → "Analiz Et"**: `GridGenerator` Polygon'un bounding box'ını
   seçilen çözünürlükte (0.5/1/2 m) hücrelere böler, her hücre için
   `IdwInterpolation` ile manyetik değer tahmin eder, Polygon-dışı hücreleri
   `isInsidePolygon=false` ile maskeler.
4. **Anomaly Engine**: Grid ortalamasından sigma-eşiği üstü hücreleri flood-fill
   ile kümeler, her küme için alan/devamlılık/komşuluk/şekil/güven puanı hesaplar.
5. **Heatmap / Contour / 3D Surface**: Aynı `AreaAnalysisViewModel` (Activity-scoped,
   `hiltViewModel(activity)`) örneğinden grid'i okur, ilgili mesh builder
   (`HeatmapMeshBuilder` / `ContourBuilder` / `Surface3DMeshBuilder`) ile GPU
   buffer'larına çevirip OpenGL ES 2.0 ile render eder.
6. **Reports**: `ReportRepository.exportAllFormats()` tüm formatları
   (PDF/CSV/GeoJSON/KML/PNG) `context.getExternalFilesDir(null)/reports/session_<id>/`
   altına yazar.

### Önemli bilimsel/etik kısıt (kasıtlı tasarım kararı)

`AnomalyResult` ve onu üreten `AnomalyEngine`, **yalnızca** "manyetik anomali" /
"yüzey anomalisi" / "incelemeye değer bölge" etiketleri üretir.
`AnomalyResult.DISCLAIMER` sabiti şu cümleyi her rapora (PDF, KML, UI kartı) otomatik
ekler: yeraltında oda/tünel/lahit gibi bir yapının **kesin varlığına** dair hiçbir
çıkarım yapılmaz. Aynı şekilde `CameraSurveyCapture`, kamerayı yalnızca yüzey
fotoğrafı + GPS ilişkilendirmesi için kullanır; GPR benzeri bir yetenek iddia etmez.
Bu kısıtlar orijinal proje promptunun "ÖNEMLİ KISITLAR" bölümünden gelir ve kod
genelinde (yorumlar + veri modeli + UI metinleri) tutarlı şekilde uygulanmıştır.

## Kurulum

### Gereksinimler
- Android Studio (Ladybug/Koala veya üzeri) **veya** Termux + Gradle + Android SDK
  command-line tools (senin mevcut Kayser AreaScan / ArkeoSAR Ground Scan
  kurulumun gibi)
- JDK 17
- Android SDK (minSdk 29, compileSdk/targetSdk 35)

### Gradle Wrapper jar'ı (otomatik — elle bir şey yapmana gerek yok)

Bu repoda `gradlew` / `gradlew.bat` script'leri var, ancak `gradle-wrapper.jar`
(binary dosya) dahil edilmemiştir çünkü bu proje, dış dosya indirmeye izin
vermeyen kısıtlı bir ortamda üretildi. **Bunun için elle bir şey yapmana
gerek yok**: `.github/workflows/android-ci.yml` içindeki "Generate Gradle
Wrapper jar if missing" adımı, jar'ı GitHub Actions runner'ında otomatik
üretir (runner'da zaten Gradle kurulu gelir, network kısıtın yok).

Eğer **lokal makinende** (Termux, Windows) de derleme yapmak istiyorsan,
orada bir kere şunu çalıştırman gerekir (CI'dan bağımsız, sadece kendi
cihazın için):

```bash
gradle wrapper --gradle-version 8.9
```

Termux'ta `pkg install gradle` ile Gradle'ı kurabilirsin; Android Studio
kullanıyorsan projeyi açtığında bunu otomatik teklif eder, elle bir şey
yapmana gerek kalmaz.

## GitHub Actions ile APK Üretimi

Projede `.github/workflows/android-ci.yml` zaten hazır. Adımlar:

1. **Repoyu GitHub'a yükle** (henüz yüklemediysen):
   ```bash
   cd ArkeoScanPhone
   git init
   git add .
   git commit -m "İlk commit: ArkeoScan Phone"
   git branch -M main
   git remote add origin https://github.com/<kullanici-adi>/<repo-adi>.git
   git push -u origin main
   ```
   Wrapper jar'ı eklemen gerekmiyor — CI bunu kendisi üretir (yukarıdaki not).

2. **Maps API key'i ekle** (yoksa harita boş gelir ama derleme bozulmaz) —
   `app/src/main/AndroidManifest.xml` içindeki placeholder'ı değiştir.

3. **GitHub'a push ettiğinde workflow otomatik tetiklenir.** Repo sayfasında
   **Actions** sekmesine git, "Android CI Build" workflow'unun çalıştığını
   göreceksin (yaklaşık 5-10 dakika sürer — ilk çalıştırmada bağımlılık
   indirme nedeniyle daha uzun olabilir).

4. **APK'yı indirmek için**: workflow tamamlandığında, o çalıştırmanın
   sayfasına gir → en altta **Artifacts** bölümünde `arkeoscan-debug-apk` ve
   `arkeoscan-release-apk` görünür → tıkla, zip olarak iner, içinden `.apk`
   dosyasını çıkarıp telefonuna kurabilirsin.
   - **Debug APK** otomatik debug-key ile imzalı gelir, doğrudan kurulabilir
     (Ayarlar > Güvenlik > Bilinmeyen kaynaklara izin ver gerekebilir).
   - **Release APK imzasızdır** (`isMinifyEnabled = false`, signing config
     tanımlanmadı) — doğrudan kurulamaz, önce kendi keystore'unla imzalaman
     gerekir (`jarsigner` veya `apksigner` ile). Sahada test için debug APK
     kullanman daha pratik.

5. **workflow_dispatch** tanımlı olduğundan, push beklemeden Actions
   sekmesinden "Run workflow" butonuyla da elle tetikleyebilirsin.

### Release APK'yı imzalamak istersen (opsiyonel)

```bash
keytool -genkey -v -keystore arkeoscan-release.keystore -alias arkeoscan \
  -keyalg RSA -keysize 2048 -validity 10000

apksigner sign --ks arkeoscan-release.keystore \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

İleride bunu CI'ya otomatik entegre etmek istersen, keystore'u base64 olarak
GitHub Secrets'a ekleyip workflow'da bir "Sign APK" adımı eklenebilir — bunu
istersen birlikte kurabiliriz.

### Google Maps API Key (lokal derleme için)

`app/src/main/AndroidManifest.xml` içindeki şu satırı kendi API key'inle değiştir
(Walk Scan ekranındaki canlı harita için gereklidir):

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

Google Cloud Console'dan "Maps SDK for Android" için bir key al.

### Lokal Derleme

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (imzasız; CI'da da bu şekilde)
```

## Bilinen sınırlamalar / yapılacaklar

- **Gerçek cihazda test edilmedi.** Bu kod tabanı bu oturumda sıfırdan, derleme
  ortamı olmayan bir sandbox'ta yazıldı (bkz. `BUILD_PLAN.md` madde 15). Statik
  olarak (paket/dizin uyumu, brace dengesi, import bütünlüğü, version-catalog
  referans tutarlılığı) doğrulandı ama gerçek `gradle build` / cihaz testi
  YAPILMADI. İlk açılışta muhtemelen küçük import/tip hataları çıkabilir —
  bunlar Android Studio'nun "Quick Fix" özelliğiyle hızla giderilir.
- **3D Surface mesh builder** `marching cubes` değil, basit height-mapped grid
  triangulation kullanır (Kayser AreaScan'deki gerçek Marching Cubes
  isosurface'den farklı, çünkü burada veri zaten 2D grid + tek değer; tam hacimsel
  veri olmadığından klasik marching cubes'a ihtiyaç yoktur).
- **Contour renkli değil** (tek renk çizgi); `ColorScaleMapper`'dan isovalue'ya
  göre renk almak için `ContourRenderer`'da küçük bir değişiklik yeterli
  (yorum olarak not edildi).
- **Settings ekranı kalıcı değil** (in-memory). Jetpack DataStore eklenmesi
  kolay bir genişletme (AppSettings zaten framework-bağımsız tasarlandı).
- **Kriging** henüz yok; `InterpolationStrategy` arayüzü bunun için hazır
  (yeni bir `KrigingInterpolation : InterpolationStrategy` sınıfı eklemek yeterli).
- Maps Compose kullanımı `accompanist-permissions` gibi bir izin kütüphanesi
  yerine `MainActivity`'de toplu izin isteği ile basitleştirildi; prod için
  ekran-bazlı runtime izin akışı (özellikle Walk Scan başlamadan önce GPS izni
  kontrolü) eklenmelidir.

## Devam etmek için

Bkz. `DEVAM_PROMPTU.md` — bir sonraki oturumda Claude'a (veya başka bir
asistana) bu projenin tam bağlamını vermek için hazırlanmış özet doküman.
