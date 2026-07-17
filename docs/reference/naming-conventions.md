# قراردادهای نام‌گذاری (Naming Conventions)

**نقش:** Reference — راهنما، نه قانون الزام‌آور
**وضعیت:** فعال (Stack: Kotlin / Jetpack Compose / Room)

قواعد یکسان نام‌گذاری در کل پروژه — فایل‌های Kotlin، بلوپرینت‌ها، پوشه‌ها، متغیرها.

---

## فایل‌های Kotlin

**کلاس‌ها، Data Class ها، Interface ها، Composable ها:** `PascalCase.kt`، نام فایل معمولاً با نام کلاس اصلی داخلش یکی است.
```
StoryWizardViewModel.kt
DnaManager.kt
ShotEntity.kt
MainScreen.kt          (شامل Composable به همین نام)
```

**فایل‌های عمومی/Utility (بدون یک کلاس اصلی):** `PascalCase.kt` با نام توصیفی.
```
DateUtils.kt
PromptFormatters.kt
```

---

## کلاس‌ها و تایپ‌ها (Kotlin)

| نوع | قاعده | مثال |
|---|---|---|
| Class / Data Class | `PascalCase` | `class DnaManager`, `data class Shot(...)` |
| Interface | `PascalCase` (بدون پیشوند `I`) | `interface PromptRenderer` |
| Sealed Class / Sealed Interface | `PascalCase` | `sealed class ValidationResult` |
| Object (Singleton) | `PascalCase` | `object TokenEstimator` |
| Enum Class | نام کلاس `PascalCase`, مقادیر `UPPER_SNAKE_CASE` | `enum class StoryType { NARRATIVE, CONCEPTUAL, VISUAL_ONLY }` |
| Composable Function | `PascalCase` (چون در واقع UI "می‌سازد") | `@Composable fun ShotCard(...)` |

---

## توابع و متغیرها (Kotlin)

| نوع | قاعده | مثال |
|---|---|---|
| توابع عادی | `camelCase`, معمولاً با فعل شروع | `fun getStoryContext()`, `fun validateShot()` |
| متغیرها / Property ها | `camelCase` | `val storyType`, `var isLoading` |
| ثابت‌ها (`const val` در سطح فایل/Companion) | `UPPER_SNAKE_CASE` | `const val MAX_SCENE_COUNT = 100` |
| متغیر Boolean | پیشوند `is` / `has` / `should` / `can` | `isLoading`, `hasError`, `canProceed` |
| پارامتر Composable برای Callback | پیشوند `on` | `onShotClick: () -> Unit` |

---

## فیلدهای JSON / Room Entity

فیلدهای ذخیره‌شده در دیتابیس یا رد‌وبدل‌شده به‌صورت JSON (مثلاً در `PromptBlueprint`)، از `snake_case` پیروی می‌کنند — همان‌طور که در تمام بلوپرینت‌های `docs/blueprints/` استفاده شده:

```json
{
  "story_type": "Narrative",
  "narrative_intensity": "High",
  "created_at": "2026-01-26T10:30:00Z"
}
```

**دلیل این استثنا:** بلوپرینت‌ها (منبع منطق کسب‌وکار) با `snake_case` نوشته شده‌اند؛ حفظ همین قرارداد در لایه‌ی داده از ناهماهنگی بین مستندات و کد جلوگیری می‌کند. در کد Kotlin، این فیلدها هنگام Map شدن به `data class` به `camelCase` تبدیل می‌شوند (طبق قرارداد بالا).

---

## شناسه‌ها (IDs)

```
[نوع]_[شناسه]

char_001
scene_intro_01
shot_001
proj_2026_scifi_short
```

---

## پوشه‌های Kotlin (ساختار پروژه)

```
app/src/main/java/com/operaboys/cinemashotgenerator/
├── data/       → Room Entities, DAO, Database
├── domain/     → مدل‌های دامنه، منطق کسب‌وکار
├── ui/         → Compose Screens, Components
│   └── theme/  → تعریف تم
└── di/         → (رزرو برای آینده)
```

---

## نام‌گذاری بلوپرینت‌ها و اسناد (بدون تغییر نسبت به Stack)

این بخش مستقل از زبان برنامه‌نویسی است:

```
docs/blueprints/[NN]-[unit-name]/blueprint.md
docs/governance/[topic-name].md
docs/reference/[topic-name].md
```

فایل‌های Root: `README.md`, `CHANGELOG.md` — همیشه با حروف بزرگ.

---

## کامنت‌های ویژه

```kotlin
// TODO: توضیح کار باقی‌مانده
// FIXME: مشکلی که باید رفع شود
// NOTE: نکته‌ی مهم برای خواننده‌ی بعدی
```

---

## الگوهای نامناسب (پرهیز کنید)

- **نام‌های مبهم:** `data`, `temp`, `x` — به‌جایش نام معنادار بدهید.
- **مخفف‌های نامفهوم:** `usr`, `cfg` — مگر رایج و بدون ابهام باشند.
- **نام‌های منفی گمراه‌کننده:** `isNotActive` — به‌جایش `isActive` با منطق معکوس.
- **نام‌های خیلی طولانی:** `theCurrentUserStoryContextObjectForThisSession` — به‌جایش `userStoryContext`.

---

## خلاصه‌ی سریع

| مورد | قاعده | مثال |
|---|---|---|
| فایل Kotlin | `PascalCase.kt` | `DnaManager.kt` |
| کلاس / Data Class | `PascalCase` | `class ShotEngine` |
| تابع / متغیر | `camelCase` | `getStoryContext()`, `isLoading` |
| ثابت | `UPPER_SNAKE_CASE` | `MAX_SCENE_COUNT` |
| Enum values | `UPPER_SNAKE_CASE` | `NARRATIVE`, `VISUAL_ONLY` |
| فیلد JSON/Entity | `snake_case` | `story_type`, `created_at` |
| شناسه (ID) | `type_identifier` | `char_001` |
| پوشه‌ی بلوپرینت | `NN-kebab-case` | `01-story-and-override` |

---

**یادآوری:** این سند راهنماست، نه الزام سخت‌گیرانه. رعایتش به یکسانی و خوانایی پروژه کمک می‌کند؛ در صورت تناقض با یک تصمیم صریح در بلوپرینت یک واحد خاص، آن بلوپرینت اولویت دارد.
