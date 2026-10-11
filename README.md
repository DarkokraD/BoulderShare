# BoulderShare

Photograph a climbing wall, mark a boulder problem, save it and share it with friends.

## Build

Use JDK 17 and a current Android Studio version supporting Android Gradle Plugin 9.4.
Install Android SDK platform **37.0** and SDK Build Tools **36.0.0**, then set your
SDK location in Android Studio or in an untracked `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

The Gradle 9.8.1 wrapper verifies its downloaded distribution with SHA-256.
On Windows use `gradlew.bat`; on macOS/Linux use `./gradlew`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 7.0+ device.
To run the device regression tests with an emulator or connected device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

The app targets Android 17 (API 37) and supports Android 7.0 (API 24) and newer.
Android 4–6 support was retired to use current AndroidX and Material releases.
The application's ID remains `com.herak.bouldershare`.

## Use

- **Choose photo** opens the system photo picker (with a document picker fallback).
  **Take photo** uses your installed camera app. No broad storage or camera permission is needed.
- Tap a hold to add a marker; tap an existing marker to remove it.
- Double tap a marker to cycle between start, regular and finish holds.
- Pinch to zoom into the wall (up to 8×); drag empty space to pan. **Fit photo**
  resets the view. Zooming never changes the saved or shared image framing.
- Drag a marker directly to move it; no long press is needed.
- Choose **Resize hold**, tap the marker, and adjust its size with the slider.
- Use **Route details** to add a name, author, grade and comment.
- **Save** stores the route and exports its annotated photo. **Share** saves changes
  first and opens the Android sharing sheet.
- Long press a route card to delete it after confirmation.
- The interface follows your device's light/dark theme. Unsaved edits survive
  rotation, resizing and normal Android state restoration.

On Android 10+ exported photos appear in **Pictures/BoulderShare** through MediaStore.
On Android 7–9 they are stored in the app's external Pictures/Output directory.
Original photos are copied into private app storage so saved routes do not depend
on a temporary gallery permission. Removing the app removes these private originals;
exported MediaStore photos remain. Route deletion removes its database records and
keeps photos already exported to the gallery.

## Existing routes

The existing `boulder.db` database is upgraded additively from version 3 to 4;
route IDs, metadata, photo URIs and hold types are retained. The old app stored
marker coordinates in screen pixels without recording the original editor size.
Existing markers use the first modern editor's fitted photo size as their reference.
They may need adjustment if opened on a differently sized device; new routes store
reference dimensions and remain aligned through resizing and export.

Some old gallery URI grants can no longer be read. The editor prompts you to
select the original wall photo again, preserving that route's metadata and holds.
Previously exported photos remain in their original gallery folders.

## Release signing

Debug builds use the standard debug key and do not require private files.
An unsigned release can be built without publishing credentials. If configured
credentials refer to a missing keystore, the build warns and creates an unsigned
release. To sign for an
upgrade of an existing installation, retain the **original release key** and put
these properties in `../keystore/keystore.properties` (outside this repository):

```properties
keyAlias=your_alias
keyPassword=your_key_password
storeFile=/absolute/path/to/release.jks
storePassword=your_store_password
```

Run `./gradlew :app:assembleRelease` or `./gradlew :app:bundleRelease`.
Before publishing, assign a version code higher than the last Play release.
The existing local versionCode 9 / versionName 1.1 has been preserved.

## Modernization

The project uses Java 17, AndroidX, Material 3 Views, Lifecycle ViewModels and
Glide. Legacy support libraries, JCenter, JitPack, AppIntro, Picasso and debug-db
were removed. Photos are decoded on background threads with a 2048-pixel maximum
dimension and full EXIF orientation handling. Route and hold writes are transactional.
File sharing grants access only to app image directories; the route provider is private.

Dependency versions are pinned in `app/build.gradle`. Official version references:
[Android Gradle Plugin](https://developer.android.com/build/releases/gradle-plugin),
[AndroidX](https://developer.android.com/jetpack/androidx/versions),
[Material Components](https://github.com/material-components/material-components-android/releases),
[Glide](https://github.com/bumptech/glide/releases).
