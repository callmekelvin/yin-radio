# Compose Material Icons Migration Guide

**Context:** Google has deprecated and removed the `androidx.compose.material.icons.*` library from the latest Material 3 releases. This project currently relies on these icons transitively through the Compose BOM.

**Goal:** Replace all `androidx.compose.material.icons.*` usage with direct local Vector Drawable XML files using `painterResource()`.

- Import Drawables into Project: https://developer.android.com/studio/write/resource-manager#import
- Unresolved Reference to `androidx.compose.material.icons`: https://stackoverflow.com/questions/79787692/androidx-compose-material-icons-unresolved-refrence
- Google Fonts Page: https://fonts.google.com/

---

## Table of Contents

1. [Icon Inventory](#icon-inventory)
2. [Step-by-Step Migration Instructions](#step-by-step-migration-instructions)
   - Step 1: Download Vector Assets
   - Step 2: Import into Project
   - Step 3: Configure RTL Support (Arrow Back)
   - Step 4: Refactor Kotlin Files
3. [Before/After Code Examples](#beforeafter-code-examples)
4. [Troubleshooting](#troubleshooting)

---

## Icon Inventory

You need **13 distinct vector drawables** to replace all current usage. The links below point to the exact icon configurations on the Material Symbols website:

| # | Material Icon Name | Variant | Used In | Drawable Filename | Source URL |
|---|-------------------|---------|---------|------------------|------------|
| 1 | `Search` | Filled | MainScreen.kt, DiscoverScreen.kt | `ic_search.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:search:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=search&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 2 | `Home` | Filled | MainScreen.kt | `ic_home.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:home:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=home&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 3 | `Favorite` | Filled | MainScreen.kt, ExpandedPlayerScreen.kt, StationCard.kt | `ic_favorite.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:favorite:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=fav&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 4 | `Settings` | Filled | MainScreen.kt | `ic_settings.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:settings:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=settings&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 5 | `Clear` | Filled | DiscoverScreen.kt | `ic_clear.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:clear_all:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=clear&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 6 | `ArrowBack` | AutoMirrored Filled | ExpandedPlayerScreen.kt | `ic_arrow_back.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:arrow_back:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=arrow&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 7 | `Pause` | Filled | ExpandedPlayerScreen.kt | `ic_pause.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:pause_circle:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=pause&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 8 | `PlayArrow` | Filled | ExpandedPlayerScreen.kt | `ic_play.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:play_arrow:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=play&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 9 | `Share` | Filled | ExpandedPlayerScreen.kt | `ic_share.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:share:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=share&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 10 | `SkipNext` | Filled | ExpandedPlayerScreen.kt | `ic_skip_next.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:skip_next:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=skip&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 11 | `SkipPrevious` | Filled | ExpandedPlayerScreen.kt | `ic_skip_previous.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:skip_previous:FILL@1;wght@400;GRAD@0;opsz@24&icon.query=skip&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 12 | `FavoriteBorder` | Outlined | ExpandedPlayerScreen.kt, StationCard.kt | `ic_favorite_border.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:favorite:FILL@0;wght@400;GRAD@0;opsz@24&icon.query=favo&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |
| 13 | `Close` | Filled | FilterChips.kt | `ic_close.xml` | [fonts.google.com/icons](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:close:FILL@0;wght@400;GRAD@0;opsz@24&icon.query=close&icon.size=24&icon.color=%231f1f1f&icon.platform=web) |

**Note:** Click any source URL to open the Material Symbols page with the correct style, weight, and optical size pre-configured. Download the **Android (Vector Drawable)** format.

---

## Step-by-Step Migration Instructions

### Step 1: Download Vector Assets

There are two recommended ways to get official Material Vector Drawables:

#### Option A: Using Android Studio (Recommended)

1. Open Android Studio.
2. In the **Project** panel, right-click on `res/drawable`.
3. Select **New > Vector Asset**.
4. In the dialog, click the **clip art** button (the little Android icon).
5. Search for the icon name (e.g., `home`, `search`, `favorite`).
6. Make sure **Material Icon** is selected in the dropdown.
7. Choose the correct style:
   - For most icons: select the **Filled** variant.
   - For `Favorite Border`: select the **Outlined** variant.
8. Change the default name to the suggested filename from the table above (e.g., `ic_home`).
9. Click **Next**, then **Finish**.
10. Repeat for all 13 icons.

#### Option B: Using the Material Symbols Website

1. Go to [fonts.google.com/icons](https://fonts.google.com/icons).
2. Search for the icon name (e.g., `Home`, `Search`).
3. Select the icon.
4. In the options panel, set:
   - **Style**: `Filled` (or `Outlined` for `Favorite Border`)
   - **Weight**: `400`
   - **Grade**: `0`
   - **Optical size**: `24dp`
5. Click the **Download** button and choose **Android (Vector Drawable)**.
6. Rename the downloaded `.xml` file to the suggested filename.
7. Move the file into:
   ```
   app/src/main/res/drawable/
   ```

---

### Step 2: Import into Project

Place all downloaded `.xml` files into:

```
yin-radio-android-app/app/src/main/res/drawable/
```

Your project already has this directory. Existing files there include:
- `default_station.xml`
- `ic_launcher_background.xml`
- `ic_launcher_foreground.xml`

---

### Step 3: Configure RTL Support (Arrow Back)

The `Arrow Back` icon (`ic_arrow_back.xml`) must support right-to-left layouts.

1. Open `ic_arrow_back.xml` in Android Studio or a text editor.
2. In the root `<vector>` tag, add this attribute if it is not already present:
   ```xml
   android:autoMirrored="true"
   ```
3. The root tag should look like this:
   ```xml
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="24dp"
       android:height="24dp"
       android:autoMirrored="true"
       android:viewportWidth="24"
       android:viewportHeight="24">
       <path ... />
   </vector>
   ```

---

### Step 4: Refactor Kotlin Files

For each file listed below, replace `Icon(imageVector = Icons.XXX.YYY, ...)` with a `painterResource` reference.

Add this import to each file:
```kotlin
import androidx.compose.ui.res.painterResource
```

#### 4.1 Refactor Example

Remove these imports:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
```

Replace each icon usage:
```kotlin
// Before
Icon(imageVector = Icons.Default.Home, contentDescription = "Home")

// After
Icon(painter = painterResource(R.drawable.ic_home), contentDescription = "Home")
```
Do the same for:
- `Icons.Default.Search` &rarr; `painterResource(R.drawable.ic_search)`
- `Icons.Default.Favorite` &rarr; `painterResource(R.drawable.ic_favorite)`
- `Icons.Default.Settings` &rarr; `painterResource(R.drawable.ic_settings)`

---

## Before/After Code Examples

### Using an Icon in Compose

**Before (deprecated):**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon

Icon(
    imageVector = Icons.Default.Home,
    contentDescription = "Home"
)
```

**After (migrated):**
```kotlin
import androidx.compose.ui.res.painterResource
import com.yin_radio.yin_radio_android_app.R
import androidx.compose.material3.Icon

Icon(
    painter = painterResource(R.drawable.ic_home),
    contentDescription = "Home"
)
```

### Conditional Icon Toggle

**Before:**
```kotlin
if (isFavorite) {
    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Unfavorite")
} else {
    Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "Favorite")
}
```

**After:**
```kotlin
if (isFavorite) {
    Icon(painter = painterResource(R.drawable.ic_favorite), contentDescription = "Unfavorite")
} else {
    Icon(painter = painterResource(R.drawable.ic_favorite_border), contentDescription = "Favorite")
}
```