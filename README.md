# Kuryer ilovasi (Android APK)

Bu — [yangi-kuryer](https://github.com/dilshodrm1999-art/yangi-kuryer) tizimining
**kuryer paneli**ni telefonda ochadigan Android ilova (WebView).

Ilova `https://testercol.xo.je/login.php` manzilini ochadi va quyidagilarni
qo'llab-quvvatlaydi:
- 📍 Geolokatsiya (kuryer GPS) — kuryer joylashuvini jonli uzatish
- 🎙️ Mikrofon (ratsiya — ovozli xabar)
- 📷 Fayl yuklash (rasm)
- 🔄 Pastga tortib yangilash (pull-to-refresh)
- ⬅️ Orqaga tugmasi bilan navigatsiya

---

## 📥 Tayyor APK'ni qayerdan olish

APK **avtomatik bulut**da (GitHub Actions) build qilinadi — kompyuterga hech narsa
o'rnatish shart emas.

1. Repozitoriyaning **Actions** bo'limiga kiring → oxirgi "Build APK" ishini oching.
2. Pastdagi **Artifacts** dan `kuryer-apk` ni yuklab oling, **yoki**
3. **Releases → `latest`** bo'limidan to'g'ridan-to'g'ri `kuryer.apk` ni yuklab oling.

> Telefonda o'rnatishda "Noma'lum manbalardan o'rnatish"ga ruxsat bering.
> Bu **debug** APK — test uchun. Play Store'ga qo'yish uchun imzolangan
> release APK kerak bo'ladi (kalit bilan).

---

## ⚙️ Sozlash

Saytingiz domeni o'zgarsa, `app/src/main/java/uz/kuryer/app/MainActivity.java`
faylida quyidagilarni o'zgartiring:

```java
private static final String START_URL = "https://testercol.xo.je/login.php";
private static final String HOST = "testercol.xo.je";
```

So'ng o'zgarishni `main` ga push qiling — APK avtomatik qayta build bo'ladi.

---

## 🛠️ Texnik

- WebView wrapper, `minSdk 23`, `targetSdk 34`.
- Build: GitHub Actions (`.github/workflows/build-apk.yml`).
- Lokal build (Android Studio'da): loyihani oching va **Run** bosing.
