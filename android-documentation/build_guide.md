# Yin Radio Android — Build Guide

Step-by-step instructions for building the Yin Radio Android app via Android Studio.

---

## Prerequisites

- Android Studio (latest stable version recommended)
- JDK 17 (matches `compileOptions` in `app/build.gradle.kts`)
- Android SDK API 37 (matches `compileSdk = 37`)
- `local.properties` file in `yin-radio-android-app/` with your SDK path:
  ```properties
  sdk.dir=C:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
  ```

---

## Open the Project

1. Launch Android Studio.
2. **File → Open...**
3. Select the `yin-radio-android-app` folder (the one containing `app/`, `gradle/`, etc.).
4. Wait for Gradle sync to complete.

---

## Build Debug APK

**Via Android Studio menu:**
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**

**Via terminal:**
```bash
./gradlew :app:assembleDebug
```

**Output:**
- `app/build/outputs/apk/debug/app-debug.apk`

**Notes:**
- Debug builds use `applicationIdSuffix = ".debug"` and `isDebuggable = true`.
- Can be installed alongside release builds (different package name).
- Points to the default stations API URL.

---

## Build Release APK

**Via Android Studio menu:**
- **Build → Generate Signed App Bundle or APK...**
- Select **APK**
- Create or select a **release keystore** (required for a signed release APK)

**Via terminal (unsigned):**
```bash
./gradlew :app:assembleRelease
```

**Output:**
- Signed: `app/build/outputs/apk/release/app-release.apk`
- Unsigned: `app/build/outputs/apk/release/app-release-unsigned.apk`

**Notes:**
- Release builds enable **R8 code shrinking** (`isMinifyEnabled = true`) and **resource shrinking** (`isShrinkResources = true`).
- ProGuard rules are defined in `app/proguard-rules.pro`.
- Uses the production stations API URL.

---

## Build AAB (Play Store)

**Via Android Studio menu:**
- **Build → Generate Signed App Bundle or APK...**
- Select **Android App Bundle (.aab)**
- Provide your release keystore

**Via terminal:**
```bash
./gradlew :app:bundleRelease
```

**Output:**
- `app/build/outputs/bundle/release/app-release.aab`

---

## Build Configuration (`app/build.gradle.kts`)

The build types are defined in `app/build.gradle.kts` inside the `buildTypes { }` block:

```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        isDebuggable = true
    }

    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

| Property | Debug | Release |
|----------|-------|---------|
| `applicationIdSuffix` | `.debug` | — |
| `isDebuggable` | `true` | `false` |
| `isMinifyEnabled` | — | `true` |
| `isShrinkResources` | — | `true` |
| ProGuard rules | — | `proguard-rules.pro` |

**Other key settings in `app/build.gradle.kts`:**
- `compileSdk = 37`
- `minSdk = 24` (Android 7.0+)
- `targetSdk = 37`
- `versionCode = 1`
- `versionName = "1.0"`
- `STATIONS_BASE_URL` is read from `local.properties` (fallback: `https://callmekelvin.github.io/yin-radio/`)

---

## Quick Reference Commands

| Task | Command |
|------|---------|
| Debug APK | `./gradlew :app:assembleDebug` |
| Release APK | `./gradlew :app:assembleRelease` |
| Release AAB | `./gradlew :app:bundleRelease` |
| Clean build | `./gradlew clean` |
