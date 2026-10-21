# BoulderShare Google Play publishing guide

Use this to update the original BoulderShare listing and repeat future releases.
Requirements checked on **12 October 2026**; check Play Console notices each time.
Keep the application ID **`com.herak.bouldershare`** and the correct signing identity
so existing users receive an update.

## First return to Play Console

1. Open [Google Play Console](https://play.google.com/console) with the Google account
   used for the original publication. Select **BoulderShare** and confirm its package
   name. If missing, check your other accounts or ask the account owner for access.
   Resolve account verification, app status and policy notices first. If the account
   was closed, contact Play support before planning an update; old accounts can be
   [closed for inactivity](https://support.google.com/googleplay/android-developer/answer/11605267).
2. Find the highest uploaded **version code**, including testing tracks, under
   **Test and release → Latest releases and bundles**. Record it for the next build.
3. Find **Play app signing** (currently under **Protected with Play → Play Store
   distribution**, or search Console for it). Determine which signing route applies:

| Signing status | What to use |
| --- | --- |
| Already enrolled in Play App Signing | Sign an Android App Bundle (`.aab`) with the registered **upload key**. Google signs the installed app. A lost upload key can be reset. |
| Not enrolled, original key available | Sign an APK with the **original app signing key**, or enroll using that original key and then upload an AAB. Follow Console's PEPK key-transfer instructions. |
| Not enrolled, original key lost | An ordinary update is impossible. Search old computer backups for the keystore and passwords; a newly generated key cannot replace it. |

See [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
and [signing key recovery limits](https://developer.android.com/studio/publish/app-signing).
Back up the keystore securely and keep passwords in a password manager.

## Prepare a release locally

1. Follow the [README build setup](README.md#build). In `app/build.gradle`, set
   `versionCode` higher than **every previously uploaded code** and choose a
   user-facing `versionName`. The prepared release has code **10**, name **2.0**,
   following Play's highest uploaded code **9** (confirmed on 21 October 2026).
   Check Play again before assigning a code for future updates.
2. Check the [current target API requirement](https://support.google.com/googleplay/android-developer/answer/11926878).
   Mobile updates currently require **API 36 or higher**. This checkout targets
   **37** and supports Android 7.0+, so older devices will not receive this build.
3. Configure the appropriate key in **`C:/Workspace/keystore/keystore.properties`**
   (outside this repository). Use forward slashes in the keystore path:

   ```properties
   keyAlias=YOUR_ALIAS
   keyPassword=YOUR_KEY_PASSWORD
   storeFile=C:/path/to/release.jks
   storePassword=YOUR_STORE_PASSWORD
   ```

4. From the repository folder in PowerShell, run:

   ```powershell
   .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
   ```

   Upload **`app/build/outputs/bundle/release/app-release.aab`**. For the legacy APK
   route, replace `:app:bundleRelease` with `:app:assembleRelease` and use
   **`app/build/outputs/apk/release/app-release.apk`**. Check that release signing is
   configured: this project can successfully build an **unsigned** release when
   credentials are absent. Do not use the old `app/app-release.apk`.

5. Check Smart marking's MediaPipe native libraries with Android Studio's APK
   Analyzer and test on a 16 KB emulator. Google's current
   [16 KB compatibility guidance](https://developer.android.com/guide/practices/page-sizes)
   gives **1 February 2027** as the update-blocking deadline; follow any Console
   notices and fix incompatible dependencies before release.

## Refresh the listing and declarations

Complete the Dashboard and **App content** tasks: Data safety, ads, app access,
content rating, target audience and any other requested declarations. Refresh the
description, screenshots, support email and country availability.

Publish a public privacy-policy webpage and add its URL to Console **and a link or
the policy text inside the app**. Include photo/route storage, local Smart marking,
user-initiated sharing, backup behavior, retention/deletion and a contact address.
The current app needs this privacy-policy entry added. Google requires it even for
apps that collect no data. [Privacy policy requirements](https://support.google.com/googleplay/android-developer/answer/10144311).

The code indicates local processing and removes Internet permission. This supports
a possible “no data collected” answer, but review the final app and SDKs before
declaring it; follow the form's exceptions for user-initiated sharing.
[Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469).

## Test and publish

1. Open **Test and release → Testing → Internal testing → Create new release**.
   Upload the signed artifact, add release notes, resolve errors and roll out to
   your testers. Add their Google accounts and send them the opt-in link.
2. Test the **Play-installed release**, including photo import/camera, marking,
   Smart marking, save/share, rotation and dark mode. Crucially, update over the
   original Play version **without uninstalling** and confirm old routes/photos
   survive. Test Android 7.0 and a current Android version where possible.
3. Promote the tested artifact to **Test and release → Production**. Start with a
   small rollout, such as 10%, and submit changes through **Publishing overview**.
   [Release workflow](https://support.google.com/googleplay/android-developer/answer/9859348).
4. For control over launch timing, enable **Managed publishing** before submission;
   after approval, publish the approved changes yourself. Otherwise approved changes
   can go live automatically. [Publishing controls](https://support.google.com/googleplay/android-developer/answer/9859654).
5. Monitor **Android vitals**, reviews and installation/update reports. Increase to
   100% when stable. If problems appear, halt the rollout and publish a fix with a
   higher version code; halting does not undo updates already installed.

The 12-testers-for-14-days gate applies to personal accounts created **after
13 November 2023**, so it does not apply to your original account under that rule.
If you need a new personal account, check its
[production access requirements](https://support.google.com/googleplay/android-developer/answer/14151465).

## Every future update

**Check Console notices → increase version code → build/sign → internal test →
update declarations and release notes → submit production release → monitor → 100%.**

Keep a private release log: version code/name, source commit, artifact backup,
signing certificate fingerprint, release notes and rollout date. Preserve
`app/build/outputs/mapping/release/mapping.txt` for diagnosing obfuscated crashes;
never put passwords or private keys in the log or Git.
