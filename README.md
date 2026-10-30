# Lebensmittel — Shopping List

An Android app for everyday grocery shopping in Germany. Lists are tied to days in a calendar.
Products are looked up in Open Food Facts and its sister databases: food, personal care, household goods and pet food.

## Features

- **Calendar of lists**: a list for every day, "bought" check marks, copy a list from another day
- **Smart search** in Ukrainian, Russian, English or German ("хліб", "хлеб", "bread" → Brot), by product name only
- **Barcode scanner** without camera permission (Google Code Scanner)
- **Supermarket filter**: Edeka, Lidl, Rewe, Aldi and more, including the chains' own brands
- **Calories** in the list and a **"Nutrition" tab**: protein, fat, carbohydrates, vitamins, allergens, additives, Nutri-Score, NOVA
- **Prices**: the last price is remembered; daily total and a **spending report** for any period
- **Reminders** with push notifications for a chosen day and time
- **Share a list** via a link without a server: the whole list is encoded in the link itself
- **Personalisation**: light/dark theme, background picture from the gallery with automatic text colour, custom colour for product names (RGB)
- **5 UI languages**: Ukrainian, English, German, Russian, Arabic (with right-to-left layout)

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Coil · ML Kit Code Scanner · AlarmManager · Open Food Facts API.
All data stays on the phone; no account needed.

## Data

Product information: [Open Food Facts](https://world.openfoodfacts.org), licensed under ODbL.

---

## Getting started

1. Install **Android Studio** (https://developer.android.com/studio).
2. Android Studio → **Open** → select the project folder.
3. Wait until Gradle downloads the libraries (5–15 minutes the first time; progress is shown at the bottom). If Android Studio offers to upgrade Gradle or AGP, you can accept or skip it.
4. Connect your phone:
   - Settings → About phone → tap "Build number" 7 times to enable developer mode;
   - Settings → Developer options → enable **USB debugging**;
   - connect the cable and allow debugging on the phone.
5. Select your phone at the top of Android Studio and press the green ▶ **Run**.

If the build fails, the error message is shown in the **Build** window.

To get an APK you can install without a cable: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The file appears in `app/build/outputs/apk/debug/app-debug.apk`; copy it to the phone and install it.

## What the app does

| Feature | How it works |
|---|---|
| Calendar | Days with a list are highlighted; each cell shows how many items are still to buy (✓ = everything bought) |
| Day list | Tap the **checkbox** to mark an item as bought (tapping the row itself does nothing, to avoid accidental taps); bought items move to the bottom |
| Adding | Type a name in German, Ukrainian, Russian or English. The app shows: products you entered before; the dictionary (хліб → Brot); products from the databases |
| "Bread comes in many kinds" | Tap "Brot" in the dictionary to see specific products from the database with brands and calories |
| Without choosing | "Just add “Brot”" or the "Just this" button adds a generic product without a brand |
| Quantity | pcs / g / kg / ml / l / pack, with − and + buttons |
| Calories | kcal per 100 g for every product plus the total when the weight is known. The bottom of the list shows "Total ≈ … kcal" |
| Barcode | Scanner button at the top of the add screen (Google scanner, no camera permission needed) |
| Name-only search | Only products whose **name** contains the searched words are shown ("Milch" finds "Vollmilch 3,5 %", but not a chocolate that only has milk in its ingredients) |
| Store in search | After a search a "Store ▾" menu lists the supermarkets found in the results (with counts). Picking e.g. Edeka keeps only Edeka products and additionally searches for products tagged with that store. You can also type "Milch Edeka" directly. Chains' own brands are recognised too (Milbona = Lidl, ja! = Rewe, Gut & Günstig = Edeka, Balea = dm…). The chosen store is kept for the next searches |
| Language | Settings → "Language": Ukrainian, English, German, Russian, Arabic or system language. Arabic uses a right-to-left layout. Product names stay German because that is how they are stored in the database |
| Bottom tabs | Lists · Nutrition · Spending · Reminders. Nutrition, Spending and Reminders can be hidden in Settings → "Tabs" |
| Settings | ⚙ icon at the top of the main screen (or ⋮ → "Settings"): language, theme (light by default / dark / system), background picture, product name colour (preset colours, R/G/B sliders or #RRGGBB code), tabs, and what to show in the list: photos, brand and size, calories |
| Reminders | "Reminders" tab: pick a day, tap "+", enter a text and a time. A notification arrives at that time; tapping it opens the reminder. Each reminder can be switched off or deleted. Reminders are re-scheduled after a phone restart |
| Nutrition | "Nutrition" tab: scan a barcode or type the digits. Shows energy (kcal and kJ), fat, saturates, carbohydrates, sugars, fibre, protein, salt, sodium, vitamins and minerals — everything in the database — per 100 g and per serving, plus Nutri-Score, NOVA, Eco-Score, ingredients, allergens, possible traces, additives (E numbers) and labels (organic, vegan…). Button "Add to today's list" |
| Text colour on a picture | With a background picture, the app measures its brightness: light text on dark pictures, dark text on light ones |
| Copy from another day | On a day screen: ⋮ → "Copy from another day" → pick e.g. last Tuesday |
| Share | The "Share" button sends the list as text plus a `lebensmittel://import?d=…` link |
| Price | Field "Total price, €" in the quantity dialog (optional). Next time the price is filled in automatically from the last price and scaled to the quantity (same unit as last time). Edit it with the ✎ button |
| Daily total | At the bottom of the list: "Total: … · bought: …" — shown only when **every** item of the day has a price |
| Spending | "Spending" tab: date range from–to, quick buttons (this week, this month, last month, 30 days, this year), total, daily average, chart by day, biggest expenses. "Only bought" counts only checked items |
| Background | Settings → "Background picture from gallery…" → choose a picture (no permissions needed). A dimming slider keeps text readable. "Remove background" restores the plain look |

### How someone else receives a list

The whole list is encoded in the link, so no server is needed. The recipient (with the same app installed) can:

1. **tap the link**, if the messenger makes it clickable; or
2. in WhatsApp/Telegram **long-press the message → Share → Lebensmittel**; or
3. **copy the message** and tap 📋 "Paste list" on the main screen of the app.

The app then shows what is in the list and asks which day to add it to.

> Some messengers (WhatsApp, Telegram) don't make `lebensmittel://` links clickable, so options 2 and 3 are the reliable ones. Always-clickable links would need an own https address (e.g. a free GitHub Pages site).

## Project structure

```
app/src/main/
  assets/dictionary.txt        dictionary: German;Ukrainian;synonyms;Russian;English, ~280 products, easy to extend
  java/de/mef/lebensmittel/
    MainActivity.kt            start-up, incoming links and "Share"
    AppViewModel.kt            screen logic
    Store.kt                   data storage (data.json in the app's private storage)
    OffApi.kt                  Open Food Facts requests (search and barcode)
    Nutrition.kt               full nutrition facts of a product
    Stores.kt                  supermarkets and the chains' own brands
    Reminders.kt               reminder notifications (alarms, boot receiver)
    I18n.kt                    all texts in 5 languages
    Dictionary.kt, Txt.kt      multilingual search
    Kcal.kt                    calorie calculation
    Spending.kt                spending report
    ShareCodec.kt              encoding a list into a link
    ui/                        screens: calendar, day, add, nutrition, spending, reminders, settings, dialogs
```

## Data sources

- Food and calories: [Open Food Facts](https://world.openfoodfacts.org), ODbL licence. Search is limited to products sold in Germany.
- Personal care, household and pet food: [Open Beauty Facts](https://world.openbeautyfacts.org), [Open Products Facts](https://world.openproductsfacts.org), [Open Pet Food Facts](https://world.openpetfoodfacts.org), with the same API. Search and the scanner check all four databases. Such products are tagged "hygiene", "household" or "pets" and have no calories. These databases are smaller than the food one, so some products may be missing.
- Open Food Facts allows about 10 searches per minute. That is plenty for normal use; if the limit is reached, the app asks you to wait a moment.
- All your lists are stored only on the phone.

## Reminders on Xiaomi / Redmi / POCO

MIUI and HyperOS put apps to sleep aggressively. For reminders to arrive on time:
1. Settings → Apps → Lebensmittel → **Autostart**: on.
2. Same place → **Battery saver**: "No restrictions".
3. Notifications for Lebensmittel must be allowed (the app asks for this with the first reminder).
