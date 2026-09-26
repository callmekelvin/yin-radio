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
- For automated signed builds in CI, see the **CI/CD via GitHub Actions** section below.

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

## CI/CD via GitHub Actions

The project includes an automated workflow at `.github/workflows/android-build.yml` that builds and signs the app on every push to `main` (and can be triggered manually).

### Workflow Jobs

| Job | Purpose | Output Artifact |
|-----|---------|-----------------|
| `debug-build` | Builds unsigned debug APK | `debug-apk` |
| `release-build` | Builds **signed** release APK | `release-apk` |

### Required GitHub Secrets

Configure these four secrets in **Settings → Secrets and variables → Actions**:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded contents of `upload-keystore.jks` |
| `STORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

**Encode your keystore:**
```bash
base64 -w 0 upload-keystore.jks
```
Copy the output and paste it as the `KEYSTORE_BASE64` secret.

### Downloading Artifacts

1. Go to the **Actions** tab in the GitHub repo.
2. Click on the latest `Android Build` run.
3. Scroll to the **Artifacts** section at the bottom.
4. Download `debug-apk` or `release-apk`.

---

## Quick Reference Commands

| Task | Command |
|------|---------|
| Debug APK | `./gradlew :app:assembleDebug` |
| Release APK | `./gradlew :app:assembleRelease` |
| Release AAB | `./gradlew :app:bundleRelease` |
| Clean build | `./gradlew clean` |
| CI Trigger (manual) | **Actions → Android Build → Run workflow** |
