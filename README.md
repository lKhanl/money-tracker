# Money Tracker

Banka ekstrelerinden harcamaları takip eden kişisel bir finans uygulaması. Android ve iOS'ta
aynı arayüzle çalışır (Kotlin Multiplatform + Compose Multiplatform). Veriler yalnızca cihazda saklanır.

Şu an desteklenen ekstre: **GetirFinans hesap cüzdanı (PDF)**. İşlemler kategorilere ayrılır,
kendi hesapların arasındaki transferler ayrıca işaretlenir ve aylık özet çıkarılır.

## Modüller

| Modül | İçerik |
| --- | --- |
| `core` | Ekstre ayrıştırıcıları, kategorileme, aylık özet. Saf Kotlin (JVM + iOS). |
| `shared` | Ortak arayüz, ViewModel ve Room veritabanı. |
| `app` | Android uygulaması. |
| `iosApp` | iOS uygulaması (Xcode projesi). |

## Çalıştırma

Gereksinimler: JDK 21, Android SDK; iOS için Xcode.

```sh
./gradlew :core:jvmTest :core:iosSimulatorArm64Test   # testler
./gradlew :app:installDebug                           # Android
./gradlew :app:installDebug -PdemoData=true           # Android, örnek veriyle
```

iOS için `iosApp/iosApp.xcodeproj`'u Xcode'da açıp çalıştır; Kotlin tarafı derleme sırasında
Gradle ile otomatik derlenir. Örnek veri için şemada "Arguments Passed On Launch" alanına
`-demoData` ekle. Gerçek cihaza kurmak için Signing bölümünden kendi geliştirici ekibini seç.
