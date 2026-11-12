# Birthday Reminder

**🇹🇷 Türkçe** · [🇬🇧 English](README.en.md)

Android için Türkçe ve İngilizce kullanılabilen çevrimdışı doğum günü hatırlatıcısı. Yaklaşan tarihleri gör, her kişi için kendi bildirim planını oluştur ve kayıtlarını bir JSON dosyasıyla taşı.

[Son sürüm ve APK](https://github.com/KaanKaya17/BirthdayReminder/releases/latest)

<img src="docs/screenshots/home.png" width="250" alt="Açık temada doğum günleri ana ekranı"> <img src="docs/screenshots/reminder-plan.png" width="250" alt="Aynı gün için iki farklı saat içeren hatırlatma planı">

## Neler yapabilirsin?

- **Hızlı kayıt.** İsim, gün ve ay ile kayıt oluştur. Yıl isteğe bağlı. Hediye ve kutlama fikirlerini kişinin notlarına ekle.
- **Kişiye özel bildirim planı.** Aynı gün 09.00 ve 19.00 gibi birden fazla saat seçebilir, bunlara bir hafta önce gibi ek hatırlatmalar ekleyebilirsin. Birden fazla kişinin doğum günü aynı tarihteyse her biri kendi bildirimiyle görünür.
- **Bildirimden işlem yapma.** Hatırlatmayı **1 saat sonra** tekrar göster, **Kutladım** ile o yılın kalan hatırlatmalarını kapat veya **Mesaj yaz** ile düzenlenebilir bir kutlama taslağı aç.
- **Yaklaşan doğum günleri.** Ana ekranda sıradaki doğum gününü ve önümüzdeki 30 günü gör. İsim veya notla ara. Widget, en yakın üç kayda doğrudan erişim sağlar.
- **Arşiv ve yedek.** Kullanmadığın kayıtları arşivle ve istediğinde geri getir. JSON yedeği oluştur, son yedek zamanını gör ve geri yüklemeden önce kaç yeni kaydın ekleneceğini kontrol et.
- **Türkçe ve İngilizce.** **Yedek → Hatırlatma ayarları → Uygulama dili** üzerinden **Sistem dili**, **English** veya **Türkçe** seç. Ekranlar, tarihler, yeni bildirimler, kutlama metni ve widget seçilen dile uyarlanır.
- **Açık ve koyu tema.** Uygulama sistem temasını izler. Büyük yazı boyutunda kayıt formundaki tarih alanları dikey olarak yerleşir.

## Kurulum ve güncelleme

1. **Android 13 veya üstü** bir cihazda [Releases](https://github.com/KaanKaya17/BirthdayReminder/releases) bölümünden APK'yı indir.
2. İlk kurulumun ardından **Hatırlatma ayarları** üzerinden bildirim iznini ver. **Test bildirimi gönder** ile izinleri kontrol edebilirsin.
3. Seçtiğin saate daha yakın bildirim için **Kesin alarm ayarını aç** seçeneğini kullan. Android, kesin alarm izni kapalıyken bildirimi daha geç gösterebilir.

GitHub APK'sı bir **debug derlemesidir**. Android, yerinde güncelleme için mevcut APK ile yeni APK'nın aynı sertifikayla imzalanmasını ister. Mevcut kurulumun üzerine yükleyerek kayıtlarını koru. Farklı imza nedeniyle kurulum reddedilirse önce çalışan kurulumdan bir JSON yedeği oluştur ve yedeği erişebileceğin bir konuma kaydet.

## Bildirim planı, widget ve yedekleme

- Bir kayıt açıp **Düzenle → Hatırlatma ekle** yolunu izle. Gün ve saat seç. Farklı saatler için işlemi tekrarla. Kayıt detayında planı ve sıradaki bildirimi görebilirsin.
- **Yedek → Hatırlatma ayarları → Ana ekrana widget ekle** yolunu izle. Desteklenen Android başlatıcılarında widget doğrudan ana ekrana eklenebilir.
- **Yedek → Yedek dosyası oluştur** ile etkin ve arşivlenmiş kayıtları, notları, bildirim planlarını ve kutlama durumunu dışa aktar.
- **Yedek → Yedekten geri yükle** ile bir JSON dosyası seç. Önizlemeyi onayladığında uygulama yeni kayıtları ekler ve mevcut kayıtları korur. Önceki JSON yedek biçimleri de okunur.

Kayıtlar cihazda Room veritabanında tutulur. JSON dosyasının nereye kaydedileceğini sen seçersin. Yeni sürümde eski veritabanı şemaları yerinde yükseltilir.

## Geliştirme

Proje Java, AndroidX View Binding, Material 3, Room ve Android AlarmManager kullanır. Android Studio'da kök klasörü aç, Android SDK 36'nın kurulu olduğundan emin ol ve `app` yapılandırmasını çalıştır. Gradle için Android Studio'nun beraberinde gelen JDK kullanılabilir.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Son komut için bağlı bir Android cihazı veya çalışan emülatör gerekir. Oluşan debug APK'sı `app/build/outputs/apk/debug/app-debug.apk` konumundadır.

- **`data/`** Room şeması, sürüm geçişleri ve JSON yedekleme
- **`notification/`** yıllık planlama, eş zamanlı doğum günleri ve erteleme
- **`widget/`** yaklaşan doğum günleri için Android ana ekran widget'ı

[Sürüm geçmişi](https://github.com/KaanKaya17/BirthdayReminder/releases)
