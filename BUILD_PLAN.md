# ArkeoScan Phone — Build Plan

## Modül Yapısı
- app/ (Compose UI, Navigation, DI wiring)
- core-database/ (Room)
- core-gps/ (Location + outlier filter + polygon builder)
- core-magnetometer/ (Sensor + calibration + baseline)
- core-motion/ (Accelerometer/Gyro + heading)
- core-analysis/ (IDW interpolation, grid, anomaly engine)
- core-renderer/ (OpenGL ES 2.0 — Heatmap, Contour, 3D Surface)
- core-camera/ (CameraX + EXIF + Polygon association)
- core-reports/ (PDF, PNG, CSV, GeoJSON, KML export)
- core-common/ (shared models, utils, Result wrappers)

## Sıra
1. [x] Klasör/plan
2. [x] Root gradle files (settings, build, version catalog, gradle.properties)
3. [x] core-common
4. [x] core-database (Room entities + DAO)
5. [x] core-gps (LocationTracker, OutlierFilter, PolygonBuilder, Shoelace area)
6. [x] core-motion (Heading via rotation vector)
7. [x] core-magnetometer (Sensor listener, calibration, baseline, deviation)
8. [x] core-analysis (IDW, Grid, Anomaly Engine, confidence scoring)
9. [x] core-renderer (GLSurfaceView, Heatmap shader, Contour (marching squares), 3D Surface (height-mapped mesh))
10. [x] core-camera (CameraX capture, EXIF GPS write/read, polygon link)
11. [x] core-reports (PDF via PdfDocument, CSV, GeoJSON, KML writer, PNG snapshot)
12. [x] app module: Hilt setup, Navigation graph, Screens (Home, WalkScan, LiveMagnetometer, CameraSurvey, AreaAnalysis, Heatmap, Contour, Surface3D, Reports, Settings)
13. [x] GitHub Actions CI (debug+release APK build & upload artifact)
14. [x] README + devam promptu (continuity doc)
15. [~] Derleme doğrulaması — bu sandbox ortamında Gradle dağıtımı ve Android SDK
       indirilemediği için (`services.gradle.org` / `dl.google.com` allowlist dışında,
       `host_not_allowed`), gerçek `./gradlew assembleDebug` çalıştırılamadı.
       Yapılan doğrulamalar: brace/paren/bracket dengesi, paket/dizin uyumu,
       version-catalog referans tutarlılığı, modül include/project() tutarlılığı,
       ve tüm com.arkeoscan.* iç importların tanımlı sembollere karşılık gelmesi
       (184 sembol, 68 dosya, ~5700 satır) — hepsi geçti. Gerçek derleme,
       Android Studio veya Termux+SDK ortamında (kullanıcının her zamanki
       geliştirme akışı) ilk açılışta yapılmalı.

