# 0.7.3
- Replace UserHandle construction with reflection.

# 0.7.2
- Add a settings action to clear IWO's cache.
- Replace more hidden API usages with reflection.

# 0.7.1
- Replace direct hidden API usage with reflection.
- Show a check-mark next to the currently-selected dropdown menu option.
- Fix the Target User function for Android < 10.

# 0.7.0
- Add option to specify target user by ID.
- Support Sui.
- Update translations.
- Crash fixes.

# 0.6.4
- Fix some issues with reflection that caused installation errors or hangs.

# 0.6.3
- Reduce APK size by removing native Bugsnag modules.

# 0.6.2
- Add ability to import .apks files.
- Update translations.

# 0.6.1
- A bunch of crash fixes.
- Update translations.

# 0.6.0
- Add "Install Reason" option.
- Add Brazilian Portuguese.
- Add Turkish.
- UI tweaks.
- Crash fixes.

# 0.5.0
- Automatically detect split APKs and normal APKs inside a single selection and group them accordingly.
- Add preliminary ability to import split APKs (.xapk or .apkm).
- Add view target to open APKs from the system open sheet.
- Add ability to remove packages or individual APKs from the install queue.
- Add text below install progress indicator to show how many packages have completed their install.
- Update explanation for "Grant All Requested Permissions" to be clearer that it's about runtime permission only.
- UI tweaks.

# 0.4.3
- Fix a crash on Android 14 QPR3 Beta 2 caused by an issue in Compose.

# 0.4.2
- Fix an issue where installs would immediately silently fail, resulting in an infinite loading animation.
- Fix some formatting and error handling.
- Better format statuses for installation results.

# 0.4.1
- Crash fixes.

# 0.4.0
- Add ability to specify installer package.

# 0.3.0
- Add direct root support.
- Remove "Disable Allowed APEX Update Check" on Android 12 and 13 since it shares its value with "Allow Restricted Permissions".

# 0.2.3
- Fix a crash on Android 13.

# 0.2.2
- Initial public release.
