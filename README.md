# RFCx Companion Android

## Development environment

### Requirements

- [Android Studio](https://developer.android.com/studio) 3.5+

### How to get started?

#### Get the code

- Checkout the repo [https://github.com/rfcx/companion-android.git](https://github.com/rfcx/companion-android.git) from GitHub.
- Open the project in Android Studio.
- Perform a Gradle sync (File -> Sync Project with Gradle).
- Connect a device and Run.

#### Build variants

The companion app supports build variants for different deployment devices. Choose the build variant via View -> Tool Windows -> Build Variants.

##### Common (Default)
- Supports RFCx Edge 1.2.0-rc.1 and AudioMoth 1.1.0 (firmware audiomoth-rfcx-0.0.6.bin)

##### Internal
- Supports RFCx Guardian (OrangePi 3G-IOT) [guardian-opi-android-source](https://github.com/rfcx/guardian-opi-android-source)
