# Item_DB_Android

Minimal Android app for managing items, topics, historical values, import/export, and graphs.

## Status
Runnable MVP scaffold with separate workflow pages.

## Implemented scope
- item creation with topic tagging
- historical value capture (`price`, `date`, `location`)
- search and sort across topic/price/location
- JSON export of all tables
- JSON import preview with explicit controls:
  - `Accept Merge`
  - `Accept Replace`
  - `Deny Import`
- basic graph view (ASCII-like price history chart)
- separate pages for:
  - items
  - history
  - search
  - import/export
  - graph

## Removed scope
- item groups
- nested group hierarchy
- group assignment UI

## Build and run (shell only)
```bash
cd /home/user/Downloads/Item_DB_Android
export JAVA_HOME=/home/user/Downloads/toolchains/jdk-17.0.18+8
export ANDROID_SDK_ROOT=/home/user/Downloads/android-sdk
export GRADLE_USER_HOME=/tmp/gradle-user-home
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
./gradlew :app:assembleDebug
```

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`
