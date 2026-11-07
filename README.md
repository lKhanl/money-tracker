# Money Tracker

Banka ekstrelerinden harcamaları takip eden kişisel bir finans uygulaması. Android ve iOS'ta
aynı arayüzle çalışır (Kotlin Multiplatform + Compose Multiplatform). Veriler yalnızca cihazda saklanır.

Şu an desteklenen ekstre: **GetirFinans hesap cüzdanı (PDF)**. İşlemler kategorilere ayrılır,
kendi hesapların arasındaki transferler ayrıca işaretlenir ve aylık özet çıkarılır.

## Ekranlar

- **Ana Sayfa**: seçili ayın toplam harcaması, kategori dağılımı ve son işlemler.
- **İşlemler**: seçili ayın işlemleri güne göre gruplu. Sağ üstteki **+** ile PDF ekstre içe aktarılır.
- **Hesaplar**: eklediğin hesapların kartları. **+** hesap ekleme panelini açar; karta dokununca o hesabın
  seçili aydaki işlemleri alttan açılan panelde listelenir (panelde de PDF içe aktarma **+**'sı var).
  Kartı sola kaydırınca silme düğmesi çıkar.
- **Ayarlar**: tema, desteklenen bankalar ve sürüm bilgisi.

PDF içe aktarma sonucu ekranın altında kısa süre görünen bir bildirimle (toast) gösterilir.
Üstteki ay seçiciyle aylar arasında gezilir.

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

## Lisans

Copyright (C) 2026 lKhanl

Bu proje [GNU Genel Kamu Lisansı v3.0](LICENSE) (GPL-3.0-or-later) ile lisanslanmıştır. Kodu değiştirip
dağıtırsan, türev çalışmayı da aynı lisansla ve kaynak koduyla birlikte yayımlamalısın.

Uygulamadaki banka logoları (QNB, GetirFinans) ilgili kurumların markasıdır ve bu lisansın kapsamında değildir.
