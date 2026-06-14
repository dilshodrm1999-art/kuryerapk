# Logotip haqida

Ilova ikonkasi sizning logotipingiz (navy yuguruvchi + orange paket, oq fonda)
asosida **vektor** ko'rinishida qayta chizilgan:
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`

## Aniq (original) rasmni qo'yish (ixtiyoriy)
Agar aynan o'zingiz yuborgan PNG logotipni qo'ymoqchi bo'lsangiz:

1. Android Studio'da: **res** ustiga o'ng tugma → **New → Image Asset**.
2. **Foreground Layer → Image** sifatida logo PNG'ni tanlang, fon oq qiling.
3. **Next → Finish** — barcha o'lchamdagi ikonkalar avtomatik yaratiladi.

Yoki PNG'ni quyidagi o'lchamlarda `mipmap-*` papkalariga joylang:
`mdpi 48px, hdpi 72px, xhdpi 96px, xxhdpi 144px, xxxhdpi 192px`
(fayl nomi: `ic_launcher.png` va `ic_launcher_round.png`).
