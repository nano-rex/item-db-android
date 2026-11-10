# Item_DB_Android

Minimal offline Android app for tracking items over time — prices, where you saw them,
rankings and progress notes — with tags, search, charts, and JSON import/export.
Everything is stored locally in SQLite.

## Features

- **Items** with a title, a description, and any number of **tags**
  (e.g. `technology, programming`).
- **Structured rows** per item (**Add Row**): price, location, date, ranking, and a
  progress note — one row per observation, so an item builds up a history.
- **Home list** with search, plus filters for tags (match any / all), location and color;
  select several items to **Add Tag** / **Remove Tag** in bulk.
- **Advanced Search** across title, description, tag and progress text.
- **Analysis** charts for the current filters: **Price Over Time** (line) and
  **Latest Price Comparison** (bar).
- **Import / Export**: export all tables as JSON; importing shows a preview with explicit
  **Accept Merge**, **Accept Replace**, or **Deny Import**.
- Dark mode and color themes.

Database layout: [`docs/SCHEMA.md`](docs/SCHEMA.md).

## Build

Requirements: JDK 17 and the Android SDK with platform 34 and build-tools 34
(`sdkmanager "platforms;android-34" "build-tools;34.0.0"`). Android Studio is not needed;
the Gradle 8.2 wrapper is included.

```bash
git clone https://github.com/nano-rex/item-db-android.git
cd item-db-android
export ANDROID_HOME=$HOME/Android/Sdk      # path to your Android SDK
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Example

Track the price of a laptop across shops:

1. **New Item** → title `ThinkPad X13`, description, tags `laptop, work` → **Save**.
2. Open it → **Add Row** for each sighting:

   | Price | Location | Date |
   |---|---|---|
   | 4299 | Lowyat Plaza | 2026-10-01 |
   | 3999 | Shopee | 2026-10-15 |
   | 3899 | Lowyat Plaza | 2026-11-02 |

3. Back on the home list, filter tags `laptop` → **Open Analysis** to see the price line
   over time and the latest price per item side by side.
4. **Import / Export → Export JSON** to back up; on another phone paste the JSON into
   **Import JSON** → **Preview Import** → **Accept Merge**.
