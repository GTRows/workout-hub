# 🏋️ FatihFit - Kişisel Fitness Takip Sistemi

## 📋 Proje Özeti

Self-hosted, kullanıcı bazlı bir fitness takip uygulaması. Fatih'in kendi sunucusunda çalışacak, web ve mobil tarayıcıdan erişilebilecek. Kullanıcılar hesap açıp kendi antrenman planlarını, ilerlemelerini ve vücut metriklerini takip edebilecek. Tüm veriler JSON olarak export/import edilebilecek ki Claude (AI koç) bu verileri okuyup analiz edebilsin.

### Teknik Hedef

- **Self-hosted**: Docker ile kolay deploy
- **Web + mobile responsive**: Telefondan da rahat kullanım
- **Multi-user**: Login sistemi, kullanıcı başına izole veri
- **Offline-first düşüncesi**: Spor sırasında internet kopsa bile çalışmalı
- **Data portability**: JSON export/import her yerde

---

## 🎯 Kullanıcı Hikayeleri

1. **"Bugün ne var?"** → Telefonu aç, bugünün antrenmanını gör
2. **"Bu hareket neydi?"** → Harekete tıkla, nasıl yapılacağını öğren (video/gif/text)
3. **"Kaç kg ile yapmıştım?"** → Son antrenmandaki ağırlığı gör, ona göre bu sefer artır
4. **"İlerliyor muyum?"** → Grafik ile son 3 ay gelişimi gör
5. **"Claude'a veri atayım"** → JSON export al, sohbete yapıştır

---

## 🏗️ Teknoloji Stack Önerisi

**Backend (Fatih zaten backend developer):**

- **Node.js + Express** VEYA **Python FastAPI** (Fatih karar versin, backend developer kendisi)
- **PostgreSQL** (ilişkisel veri için ideal — kullanıcılar, antrenmanlar, setler)
- **JWT** authentication
- **Docker Compose** ile tek komutla ayağa kalkma

**Frontend:**

- **Next.js** (React) veya **SvelteKit** — SSR + PWA desteği
- **Tailwind CSS** — hızlı stillendirme
- **Recharts** veya **Chart.js** — grafikler için
- **PWA**: Telefona "app gibi" kurulabilir olsun

**Opsiyonel:**

- **MinIO** veya local storage — hareket GIF/videolarını sunmak için

---

## 📐 PHASE'LERE BÖLÜNMÜŞ GELİŞTİRME PLANI

Claude Code bu sırayı **aynen** takip etmeli. Her phase bitince test et, sonra diğerine geç.

---

### 🔧 PHASE 0: Proje Kurulumu ve Claude Code Yapılandırması

**Amaç:** Claude Code'un bu projede verimli çalışması için gerekli rules, skills ve hooks'ları oluşturmak.

**Yapılacaklar:**

1. **`CLAUDE.md` dosyası oluştur** (proje root'unda) - Claude Code'un kalıcı talimatları:
   - Proje mimarisi özeti
   - Kod stil kuralları (TypeScript strict, ESLint, Prettier config)
   - Commit mesaj formatı (conventional commits)
   - Güvenlik kuralları (secret'ları commit etme, JWT_SECRET env'den)
   - Test yazma gereklilikleri
   - "Her phase sonunda test et ve kullanıcı onayı olmadan sonrakine geçme" kuralı

2. **`.claude/` klasörü içinde skills:**
   - `.claude/skills/backend-api/SKILL.md` → REST endpoint standartları
   - `.claude/skills/database/SKILL.md` → Migration ve schema kuralları
   - `.claude/skills/testing/SKILL.md` → Unit + integration test yaklaşımı
   - `.claude/skills/ui-components/SKILL.md` → Component standartları, accessibility

3. **`.claude/hooks/` içinde:**
   - `pre-commit.sh` → lint + type check
   - `post-phase.sh` → phase bitiminde otomatik test suite çalıştır

4. **`.env.example` dosyası** — gerekli tüm env değişkenleri listesi

5. **`.gitignore`** — node_modules, .env, dist, coverage, vb.

6. **`README.md`** — kurulum talimatları (docker-compose up)

**Phase 0 Çıktısı:**

- Boş ama yapılandırılmış repo
- Claude Code'un kurallarını bildiği bir ortam

---

### 🗄️ PHASE 1: Veritabanı ve Backend İskelet

**Amaç:** API endpoint'leri olmadan önce sağlam bir veri modeli ve auth sistemi kurmak.

**Database Schema:**

```
users
  - id (uuid, pk)
  - email (unique)
  - password_hash
  - display_name
  - created_at, updated_at

user_profile
  - user_id (fk, pk)
  - height_cm
  - weight_kg (başlangıç)
  - birth_date
  - gender
  - health_notes (text) — "karaciğer yağlanması" gibi
  - goals (text)

exercises (master liste — tüm hareketlerin kataloğu)
  - id (uuid, pk)
  - name_tr (Türkçe isim)
  - name_en
  - category (push/pull/legs/cardio/core/forearm)
  - equipment (dumbbell/zbar/bodyweight/bar/wrist_tool)
  - muscle_primary
  - muscle_secondary
  - description_tr (nasıl yapılır, uzun metin)
  - form_tips (array)
  - common_mistakes (array)
  - image_url (opsiyonel)
  - video_url (opsiyonel)
  - difficulty (beginner/intermediate/advanced)

workout_plans (kullanıcının haftalık planı)
  - id (uuid, pk)
  - user_id (fk)
  - name ("Başlangıç Planı")
  - is_active (bool)
  - created_at

workout_days (plan içindeki günler)
  - id (uuid, pk)
  - plan_id (fk)
  - day_of_week (1-7: Pzt=1)
  - name ("Üst Vücut İtme")
  - focus (push/pull/legs/cardio/rest)
  - estimated_duration_min

workout_day_exercises (bir günde yapılacak hareketler)
  - id (uuid, pk)
  - workout_day_id (fk)
  - exercise_id (fk)
  - order_index
  - target_sets
  - target_reps_min
  - target_reps_max
  - target_weight_kg (hedef, opsiyonel)
  - rest_seconds
  - notes

workout_sessions (kullanıcının yaptığı gerçek antrenmanlar)
  - id (uuid, pk)
  - user_id (fk)
  - workout_day_id (fk, hangi planlanmış güne denk geliyor)
  - started_at, ended_at
  - notes (kullanıcı notu: "bugün yorgundum", "iyi geçti")
  - mood (1-5)
  - energy_level (1-5)

session_sets (her sette yapılan gerçek iş)
  - id (uuid, pk)
  - session_id (fk)
  - exercise_id (fk)
  - set_number
  - reps_done
  - weight_kg
  - rpe (1-10, opsiyonel — "ne kadar zorladın")
  - completed (bool)
  - notes

body_metrics (vücut metrikleri)
  - id (uuid, pk)
  - user_id (fk)
  - recorded_date
  - weight_kg
  - body_fat_percent (opsiyonel)
  - waist_cm (opsiyonel)
  - chest_cm, arm_cm, thigh_cm (opsiyonel)
  - photo_url (opsiyonel — progress fotoğrafı)
  - notes

supplements (kullanıcının aldığı takviyeler)
  - id (uuid, pk)
  - user_id (fk)
  - name
  - dosage
  - timing (morning/pre_workout/post_workout/evening)
  - active (bool)

nutrition_logs (opsiyonel, ileride eklenebilir)
  - id, user_id, date, meal_type, description, protein_g, carbs_g, fat_g, calories
```

**Backend Endpoints (REST):**

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/users/me`
- `PUT /api/users/me` (profil güncelle)

**Phase 1 Çıktısı:**

- PostgreSQL migration'ları
- Auth middleware çalışıyor
- Tek test kullanıcı açıp login olabiliyor
- Postman/Thunder Client collection

---

### 💪 PHASE 2: Hareket Kataloğu (Exercises)

**Amaç:** Tüm hareketleri veritabanına seed etmek ve API'den çekmek.

**Yapılacaklar:**

1. **Seed data hazırla** — Fatih'in planındaki tüm hareketler:
   - Duvar/eğimli şınav, diz şınav, tam şınav
   - Dumbbell shoulder press, bench press (yerde), lateral raise
   - Triceps kickback, triceps extension
   - Wrist curl, reverse wrist curl, hammer curl, reverse curl
   - Goblet squat, dumbbell squat, lunges
   - Romanian deadlift (Z bar)
   - Calf raise
   - Negatif pull-up, dead hang
   - Dumbbell row (tek kol)
   - Z bar curl
   - Plank, dead bug, Russian twist
   - Farmer's carry
   - Yürüyüş, merdiven

2. **Her hareket için şunları doldur:**
   - Türkçe ismi
   - Nasıl yapılır (adım adım, 4-6 madde)
   - Form ipuçları (3-5 madde)
   - Yaygın hatalar (3-5 madde)
   - Hangi kasları çalıştırır
   - Zorluk seviyesi
   - Placeholder image_url (sonra gerçek GIF'ler eklenir)

3. **Endpoints:**
   - `GET /api/exercises` (filter: category, equipment, difficulty)
   - `GET /api/exercises/:id`
   - `GET /api/exercises/search?q=...`

4. **Admin endpoint (sadece admin user):**
   - `POST /api/admin/exercises` (yeni hareket ekle)
   - `PUT /api/admin/exercises/:id`

**Phase 2 Çıktısı:**

- 30+ hareket veritabanında
- API'den listeleme ve detay çekilebiliyor

---

### 📅 PHASE 3: Antrenman Planları

**Amaç:** Haftalık plan oluşturma ve yönetme.

**Yapılacaklar:**

1. **Fatih'in mevcut planını default olarak seed et** (yeni kullanıcılara "Başlangıç Planı" olarak atanabilir)

2. **Endpoints:**
   - `GET /api/workout-plans` (user'ın planları)
   - `POST /api/workout-plans` (yeni plan)
   - `GET /api/workout-plans/:id` (plan + günler + hareketler)
   - `PUT /api/workout-plans/:id`
   - `DELETE /api/workout-plans/:id`
   - `POST /api/workout-plans/:id/activate` (aktif plan yap)
   - `POST /api/workout-plans/:id/days` (güne hareket ekle)
   - `PUT /api/workout-plans/:id/days/:dayId/exercises/:exId`
   - `DELETE /api/workout-plans/:id/days/:dayId/exercises/:exId`

3. **Drag & drop reorder desteği** — günler ve hareketler sıralaması

**Phase 3 Çıktısı:**

- Fatih giriş yapıp kendi planını düzenleyebiliyor
- Yeni plan oluşturabiliyor

---

### 🏋️ PHASE 4: Antrenman Session Takibi (Workout Execution)

**Amaç:** Uygulamanın **kalbi**. Spor yaparken kullanılan ekran.

**Yapılacaklar:**

1. **Endpoints:**
   - `POST /api/sessions/start` (bugünün planına göre session başlat)
   - `GET /api/sessions/active` (devam eden session varsa)
   - `POST /api/sessions/:id/sets` (set kaydet: reps, weight, rpe)
   - `PUT /api/sessions/:id/sets/:setId`
   - `POST /api/sessions/:id/finish`
   - `GET /api/sessions/history` (geçmiş antrenmanlar)
   - `GET /api/sessions/:id` (session detay)

2. **Özel endpoints:**
   - `GET /api/exercises/:id/last-performance` (bu hareketi en son ne zaman, kaç kg, kaç tekrar yaptı)
   - `GET /api/exercises/:id/progress` (zaman içinde gelişim)

3. **Offline-first strateji:**
   - Frontend IndexedDB'de set'leri önce local kaydetsin
   - Bağlantı gelince sync etsin

**Phase 4 Çıktısı:**

- Tam bir antrenman baştan sona yapılabiliyor
- Her set kaydediliyor, önceki antrenman verisi görülebiliyor

---

### 🎨 PHASE 5: Frontend - Temel Sayfalar

**Amaç:** Kullanıcının göreceği arayüz.

**Sayfalar:**

1. **Login / Register**
2. **Dashboard (Ana sayfa)**
   - Bugünün antrenmanı (büyük card)
   - Son kilo kaydı
   - Bu haftanın özeti (kaç antrenman yaptı, hedef vs gerçek)
   - Hızlı aksiyonlar: "Antrenmanı başlat", "Kilo ekle"

3. **Plan Görüntüleme**
   - Haftalık takvim görünümü (Pzt-Pzr)
   - Her güne tıklayınca hareketler listelenir
   - Günü düzenle butonu

4. **Antrenman Sayfası (en önemli)**
   - Büyük, parmak dostu butonlar
   - Her hareket bir card
   - Card içinde: set tamamlama checkbox + kilo ve tekrar inputları
   - "Önceki antrenmanda: 7kg x 10,10,9" bilgisi görünür
   - Set arası dinlenme timer'ı (otomatik)
   - Hareket ismine tıklayınca **hareket detay modal'ı** açılsın

5. **Hareket Detay (Modal veya ayrı sayfa)**
   - Büyük isim + kas grupları
   - Görsel/GIF (varsa)
   - "Nasıl yapılır" - adım adım
   - Form ipuçları
   - Yaygın hatalar
   - Bu hareketle ilgili kişisel rekor + son performans
   - Gelişim grafiği (min, max, volume zaman içinde)

6. **Geçmiş Antrenmanlar**
   - Takvim görünümü (yapılan günler yeşil)
   - Listeye tıklayınca o günün detayı

7. **Vücut Metrikleri**
   - Kilo grafiği (haftalık/aylık/tüm zamanlar)
   - Ölçüm ekleme formu
   - Progress fotoğrafı yükleme (opsiyonel)

8. **Hareket Kataloğu**
   - Tüm hareketler listesi
   - Filter: kas grubu, ekipman, zorluk
   - Search

9. **Profil & Ayarlar**
   - Kullanıcı bilgileri düzenle
   - Sağlık notları (Claude için önemli)
   - Hedefler
   - Takviyeler listesi

10. **Data Export/Import**
    - "Tüm verilerimi JSON olarak indir"
    - "JSON'dan veri yükle" (restore için)
    - "Claude için özet JSON" (son 30 gün, kompakt format)

**Phase 5 Çıktısı:**

- Tüm sayfalar mobil responsive
- PWA olarak kurulabilir
- Temel akış baştan sona çalışıyor

---

### 📊 PHASE 6: Grafikler ve İstatistikler

**Amaç:** Motivasyon + veri odaklı ilerleme takibi.

**Grafikler:**

1. **Volume grafiği** (haftalık toplam kg x tekrar)
2. **Her hareket için 1RM tahmini** (Epley formula)
3. **Kilo değişim grafiği**
4. **Antrenman sıklığı** (heatmap, GitHub-style)
5. **PR (Personal Record) listesi** — her hareket için en iyi set
6. **Streak takibi** — kaç gündür düzenli

**Phase 6 Çıktısı:**

- Detaylı insight sayfası
- Motive edici görseller

---

### 🔔 PHASE 7: Hatırlatıcılar ve Bildirimler (Opsiyonel)

**Amaç:** Kullanıcıyı düzenli tutmak.

- **Web push notification** (PWA ile):
  - "Bugün antrenman günün, henüz başlamadın"
  - "Kilo kaydı yapmayalı 7 gün oldu"
  - "Creatine almayı unutma"
- **Dinlenme timer** otomatik bildirim
- **Supplement hatırlatıcı** (isteğe bağlı saatlerde)

---

### 🧪 PHASE 8: Test, Docker, Deploy

**Amaç:** Production-ready hale getirmek.

**Yapılacaklar:**

1. **Unit testler** (%70+ coverage hedefi)
2. **E2E testler** (Playwright) — kritik akışlar:
   - Register → Login → Antrenman yap → Session kaydet
3. **Docker Compose** konfigürasyonu:
   - `app` service
   - `db` service (postgres)
   - `nginx` reverse proxy (opsiyonel)
4. **README'de deploy rehberi:**
   - VPS'e nasıl kurulur
   - SSL (Let's Encrypt + Certbot)
   - Backup stratejisi (postgres dump cron job)

**Phase 8 Çıktısı:**

- `docker compose up -d` ile çalışan production app
- Backup ve restore çalışıyor

---

## 🎁 BONUS FEATURE ÖNERİLERİM (Kullanımı Arttıracak)

Bunlar MVP için şart değil ama sonradan eklemek için değerli:

1. **AI Koç Entegrasyonu** 🤖
   - Uygulama içinden direkt Claude API'ye sorabilme
   - Session sonrası "bugünkü performansım nasıldı?" diye soru → Claude JSON'u okur, feedback verir
   - Haftalık otomatik Claude raporu (e-mail olarak)

2. **Meal Tracker** 🍽️
   - Kalori değil ama **protein + makro** takibi (karaciğer için önemli)
   - "Bugün ne yedim" quick-log

3. **Water Tracker** 💧
   - Creatine alan biri için kritik
   - 8 bardak gibi basit tracker

4. **Rest Day Suggestions** 😴
   - Dinlenme günlerinde mobility/stretch önerileri

5. **Exercise Video Library** 🎥
   - Self-hosted videolar (MinIO)
   - YouTube embed fallback

6. **Workout Templates / Share** 🔗
   - Plan'ını başkasıyla paylaşma (public link)
   - Community plan'ları import etme

7. **Gamification** 🏆
   - Haftalık streak rozetleri
   - PR hit olunca konfeti animasyonu
   - "100 şınav" gibi milestone achievements

8. **Dark Mode** 🌙 (mutlaka!)

9. **Türkçe/İngilizce i18n** — gerçi şu an sadece Türkçe ama altyapı olsun

10. **"Bugün atlayacağım" modu** 🤷
    - Programdan sapınca neden diye sor (hasta/yorgun/yoğun)
    - Pattern analizi → Claude'a veri

11. **Supplement Reminder System** 💊
    - Alınan/alınmayan takviyeleri daily log'a geçme

12. **Period/Cycle Awareness** (kadın kullanıcılar için, Fatih için gerekmez ama multi-user)

13. **Export to Google Fit / Apple Health** (ileride)

14. **Body measurement photo compare** 📸
    - Yan yana ay 1 vs ay 3 fotoğraf karşılaştırma

15. **Notes/Journal** 📝
    - Her güne serbest not alanı
    - Claude için değerli veri kaynağı

---

## 🤖 CLAUDE İÇİN VERİ YÖNETİMİ

Bu kısım senin (Fatih) için önemli — Claude'un senin datana erişmesi:

### Export Format (JSON)

`/api/export/claude-summary` endpoint'i şunu üretsin:

```json
{
  "user": {
    "name": "Fatih",
    "age": 26,
    "height_cm": 178,
    "current_weight_kg": 78,
    "health_notes": "Karaciğer yağlanması (NAFLD)",
    "goals": ["Fit vücut", "Karaciğer iyileştirme"]
  },
  "period": {
    "from": "2026-03-21",
    "to": "2026-04-21",
    "days": 30
  },
  "summary": {
    "total_workouts": 18,
    "planned_workouts": 20,
    "adherence_percent": 90,
    "total_volume_kg": 45200,
    "avg_session_duration_min": 47,
    "weight_change_kg": -1.2
  },
  "workouts": [
    {
      "date": "2026-04-20",
      "type": "Üst Vücut İtme",
      "duration_min": 48,
      "exercises": [
        {
          "name": "Dumbbell Shoulder Press",
          "sets": [
            { "weight": 7, "reps": 10 },
            { "weight": 7, "reps": 10 },
            { "weight": 7, "reps": 9 }
          ]
        }
      ],
      "user_notes": "İyi geçti, shoulder press'te bir sonraki sefer 8kg deneyeyim",
      "mood": 4,
      "energy": 4
    }
  ],
  "prs": [
    {
      "exercise": "Goblet Squat",
      "weight": 15,
      "reps": 12,
      "date": "2026-04-15"
    }
  ],
  "body_metrics": [
    { "date": "2026-04-01", "weight_kg": 79.2 },
    { "date": "2026-04-21", "weight_kg": 78.0 }
  ],
  "consistency": {
    "current_streak_days": 5,
    "missed_days": ["2026-04-10", "2026-04-14"],
    "missed_reasons": ["Hasta", "İş yoğun"]
  }
}
```

Bu JSON'u sohbete yapıştırdığında Claude ne yaptığını görür, yorumlar, plan önerileri verir.

### Import

Aynı format geri yüklenebilsin ki bir makineden diğerine taşınabilsin.

---

## 📏 KALİTE STANDARTLARI

- **Mobile-first tasarım** (çoğunlukla telefondan kullanacaksın)
- **Hızlı yükleme** (antrenman ekranı < 2 sn açılmalı)
- **Offline çalışma** (spor yaparken net yoksa kaydetsin, sonra sync)
- **Erişilebilirlik** (büyük butonlar, okunabilir font)
- **Güvenlik** (JWT, bcrypt, rate limiting, HTTPS zorunlu)
- **Türkçe arayüz** + Türkçe hareket isimleri

---

## 🚀 CLAUDE CODE'A İLK PROMPT

Bu dosyayı Claude Code'a vermeden önce şu komutu ver:

```
Bu projeyi adım adım kuracağız. Lütfen:

1. Önce PROJECT_BRIEF.md dosyasını baştan sona oku
2. Tüm phase'leri özümse
3. PHASE 0'dan başla — proje yapılandırması, CLAUDE.md, skills, hooks
4. Her phase bitiminde:
   - Neyi tamamladığını özetle
   - Nasıl test edebileceğimi söyle
   - Benim onayımı bekle, sonraki phase'e atlama
5. Backend dili olarak Node.js + TypeScript mi yoksa Python FastAPI mı kullanayım? Sen developer'sın, seçimi sor bana
6. Frontend için Next.js öneriyorum, onaylıyor musun?

Başla.
```

---

## 📂 ÖNERİLEN KLASÖR YAPISI

```
fatihfit/
├── CLAUDE.md                    # Claude Code için kurallar
├── PROJECT_BRIEF.md             # Bu dosya
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── .claude/
│   ├── skills/
│   │   ├── backend-api/
│   │   ├── database/
│   │   ├── testing/
│   │   └── ui-components/
│   └── hooks/
│       ├── pre-commit.sh
│       └── post-phase.sh
├── backend/
│   ├── src/
│   │   ├── auth/
│   │   ├── users/
│   │   ├── exercises/
│   │   ├── workouts/
│   │   ├── sessions/
│   │   ├── metrics/
│   │   └── export/
│   ├── migrations/
│   ├── seeds/
│   ├── tests/
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── app/  (Next.js app router)
│   │   ├── components/
│   │   ├── lib/
│   │   └── hooks/
│   ├── public/
│   └── Dockerfile
└── docs/
    ├── API.md
    ├── DEPLOYMENT.md
    └── DATA_SCHEMA.md
```

---

## ✅ BAŞARI KRİTERLERİ

Proje "tamamlandı" sayılmak için:

- [ ] Fatih telefonundan login olabiliyor
- [ ] Antrenman sayfasını açıp tüm setleri kaydedebiliyor
- [ ] Her hareketin detayını (nasıl yapılır) görebiliyor
- [ ] Önceki antrenman verisi gösteriliyor
- [ ] Kilo ve vücut metriği ekleyebiliyor
- [ ] Grafiklerde ilerlemesini görüyor
- [ ] JSON export alıp Claude'a yapıştırabiliyor
- [ ] Sunucuya docker-compose ile deploy ettik
- [ ] HTTPS ile erişim var
- [ ] Backup çalışıyor

---

**Not:** Bu brief yaşayan bir döküman. Geliştirme sırasında değişiklik gerekirse güncellenir.
