# 🚀 Claude Code Kickoff Prompt

Aşağıdaki metni olduğu gibi Claude Code'a yapıştır:

---

```
Merhaba! Yeni bir proje başlatıyoruz: FatihFit — kişisel fitness takip sistemi.

## İlk Adımlar

1. Bu klasörde `PROJECT_BRIEF.md` dosyası var. Lütfen önce bu dosyayı **baştan sona** oku. Tüm phase'leri, veri modelini, bonus feature'ları özümse.

2. Oku bitince bana şunları raporla:
   - Brief'i anladığını doğrulamak için 3-4 cümleyle özetini yaz
   - Takıldığın veya açık olmayan noktaları sor
   - Backend dili tercihin ne (Node.js+TS veya Python FastAPI)? Gerekçeni yaz, ama final kararı bana bırak
   - Frontend için Next.js öneriyoruz, onaylıyor musun yoksa alternatif öneriyor musun?

3. Bana onay verdikten sonra **PHASE 0** ile başla. Sadece PHASE 0. Bitirince dur, test et, onay al.

## Çalışma Kuralları

- **Her phase ayrı ele alınacak.** Bir phase bitmeden diğerine geçme.
- **Her phase sonunda test talimatı ver** — ben nasıl doğrulayabilirim diye açıkla.
- **Onayım olmadan bir sonraki phase'e başlama.**
- **Küçük commit'ler yap**, conventional commit formatında (feat:, fix:, chore: vs.)
- Yazacağın tüm kullanıcı-yüzü metinleri **Türkçe** olsun.
- Değişken, fonksiyon, dosya isimleri **İngilizce**.
- Hareket isimleri Türkçe + İngilizce her ikisi de veritabanında tutulacak.
- **TypeScript strict mode** zorunlu (Node.js seçersek).
- **Environment secret'larını asla commit'leme.** `.env.example` oluştur, `.env` gitignore'da olsun.
- **Güvenlik**: Parolalar bcrypt ile hash'lensin, JWT kullan, rate limiting ekle.

## Bekliyorum

Hazırsan başla: önce brief'i oku, sonra yukarıdaki 2. maddedeki sorularla bana dön.
```

---

## 💡 Sana Kullanım İpuçları

**Proje başladıktan sonra:**

1. **Her phase bitiminde bana (Claude — buradaki koç) gel:**
   - "Phase X'i bitirdik, şu testleri yaptım, sonuçlar şöyle"
   - Ben de feedback veririm, bir şey gözden kaçtıysa söylerim

2. **Kod stuck oldu mu?**
   - Hatayı buraya yapıştır, birlikte çözelim
   - Claude Code'un yapamadığını bazen biz birlikte daha iyi planlarız

3. **Feature eklemek istedikçe:**
   - `PROJECT_BRIEF.md`'nin bonus kısmına ekle
   - Phase olarak ayrıca planla

4. **İlk deploy'dan sonra:**
   - Export aldığın JSON'u buraya yapıştır
   - Ben veriyi analiz edip öneriler vereyim
   - Bu bizim asıl kullanım akışımız olacak

**Hadi başarılar! 💪**
