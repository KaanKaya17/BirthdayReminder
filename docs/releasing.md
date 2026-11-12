# Release build

Official GitHub APKs starting with 1.5 use the project release key. The signing lineage authorizes an in-place update from the official debug APKs on Android 13 and later.

## Build

```powershell
.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
```

On macOS and Linux, use `./gradlew`. The unsigned artifact is `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Sign

Use `apksigner` from the Android SDK Build Tools and the project’s existing release keystore and lineage. Keep the keystore and its password in a separate, backed-up location with access restricted to the maintainer. For the original Windows setup, the release files are in `%USERPROFILE%\.android\birthday-reminder-signing`. The saved password is protected for that Windows account.

```powershell
apksigner sign --min-sdk-version 33 --v1-signing-enabled false --v2-signing-enabled false `
  --v3-signing-enabled true --v4-signing-enabled false --rotation-min-sdk-version 33 `
  --lineage "$lineagePath" --ks "$keystorePath" --ks-key-alias birthdayreminder `
  --ks-pass env:BIRTHDAY_SIGNING_PASSWORD --out "BirthdayReminder-$version.apk" `
  "app/build/outputs/apk/release/app-release-unsigned.apk"

apksigner verify --verbose --print-certs --min-sdk-version 33 "BirthdayReminder-$version.apk"
```

Set `$lineagePath`, `$keystorePath`, and `$version` for the release. Provide `BIRTHDAY_SIGNING_PASSWORD` to the signing process and clear it afterward. Use the same release key and signing lineage for subsequent versions.

## Publish

- Test a clean install and an upgrade over the preceding official release on Android.
- Upload the signed APK and its SHA-256 checksum to a GitHub release for the source commit. Include Turkish and English release notes.
- Check that the APK downloads without signing in, and compare its checksum with the local artifact.

## Bundled libraries

The app uses [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Material Components](https://github.com/material-components/material-components-android/blob/master/LICENSE), and [Kotlin libraries](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt), including [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt). The APK contains copies of the Apache 2.0 license supplied with the bundled libraries.
