# RFCx Companion Android

## Development environment

### Requirements

- [Android Studio](https://developer.android.com/studio) 3.5+

### How to get started?

#### Get the code

- Checkout the repo [https://github.com/rfcx/companion-android.git](https://github.com/rfcx/companion-android.git) from GitHub.
- Open Android Studio and import project.
- Perform a Gradle sync (File -> Sync Project with Gradle).

#### Select build variant
##### Common
- Support RFCx Edge 1.2.0-rc.1
- Support RFCx Edge firmware [audiomoth-rfcx-0.0.6.bin]()
##### Internal
- Support RFCx Guardain (orange pi 3g-iot) [see](https://github.com/rfcx/guardian-opi-android-source)

```java
 productFlavors {
        common {
            buildConfigField 'boolean', 'ENABLE_GUARDIAN', 'false'
        }
        internal {
            buildConfigField 'boolean', 'ENABLE_GUARDIAN', 'true'
        }
    }
```