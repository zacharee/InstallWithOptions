# Install with Options
An app with a simple purpose: more advanced app installs without needing to use ADB.

Install with Options uses Shizuku to run with shell permissions, allowing you to install test-only apps, bypass Android 14's target SDK limit, downgrade certain packages, and more, all without leaving your phone (at least on Android 11 and later).

It also supports installing split APKs or batch-installing multiple separate apps.

Downloads are available on the [Releases](https://github.com/zacharee/InstallWithOptions/releases) page.

# Translating
[![Crowdin](https://badges.crowdin.net/install-with-options/localized.svg)](https://crowdin.com/project/install-with-options)

Install with Options uses Crowdin for translations.

https://crowdin.com/project/install-with-options

# Troubleshooting/FAQ

## Common Errors
### `INSTALL_FAILED_NO_MATCHING_ABIS`
This error occurs when the app you're trying to install contains native (C/C++) libraries in addition to normal Java code. Unlike Java, native libraries have to be specifically compiled for different CPU architectures.

For example, If you have a device with an x86 or amd64 processor, but the app you want to install was only compiled for ARM, you'll receive this error. There is no workaround besides finding a version of the app compiled for your architecture.

You may also receive this error on ARM64 Android devices when the app contains native libraries only compiled for ARM32. Newer chipsets and Android versions are dropping support for running ARM32 apps on an ARM64 system.

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
#### `signatures do not match`
Android performs signature comparison between the installed version of an app and the update being installed. If you receive this error when installing an update, it means the new APK's signature doesn't match the signature of the installed app.

Without root and specific system patches, you can't work around this. If the app signature is different, the old app version needs to be uninstalled first.

The "Disable Verification" option in Install with Options will _not_ bypass signature verification. It only disables basic package verification, allowing certain special APKs to be installed.

### `INSTALL_FAILED_VERSION_DOWNGRADE`
Newer versions of Android have become more and more restrictive with regards to app downgrades.

On Android 14 and later, downgrading an app only works with root or if the app was compiled with debugging or test mode enabled.

This applies even if you have the "Allow Downgrade" option selected.

### `INSTALL_FAILED_USER_RESTRICTED`
This error can occur for a number of reasons:
* If you're trying to install from a managed profile or a guest user. Install with Options can't bypass profile restrictions that prevent app installs.
* If you're using an Android skin that limits sideloading.
  * Check Developer Options for settings like "Install via USB" and "USB Debugging (Security Settings)", and make sure they're enabled.

## FAQ
### Where is the "Bypass Low Target SDK Block" option?
This flag was only added to Android in Android 14. If you're using Install with Options on Android 13 or below, you won't see it in the app.

The low target SDK block was also only added to base Android in Android 14. If installing older apps fails on older Android versions, you likely are running into a different issue.

### Why doesn't using "Grant All Requested Permissions" actually grant all permissions?
This option will only automatically grant _runtime_ permissions, like Camera or Microphone access.

Special permissions, like All Files Access, or special access, like Accessibility Services, will not be automatically granted or enabled.

# Screenshots
<img src="https://github.com/zacharee/InstallWithOptions/assets/9020352/67ccc61a-27cd-4055-bc29-2d086c5db3ae" width="400"></img>
<img src="https://github.com/zacharee/InstallWithOptions/assets/9020352/598a9bcf-dc0e-4226-a3a1-c9327833197a" width="400"></img>
<img src="https://github.com/zacharee/InstallWithOptions/assets/9020352/9831e9fe-6802-4334-87c3-16713d7ea93e" width="400"></img>
<img src="https://github.com/zacharee/InstallWithOptions/assets/9020352/4400818f-d025-4318-b010-708f68395b17" width="400"></img>
<img src="https://github.com/zacharee/InstallWithOptions/assets/9020352/ce119609-99ed-49a7-9e58-5bf76c0771aa" width="400"></img>
