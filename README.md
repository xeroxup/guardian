# Guardian - Android Security App

A modern Android security application built with Jetpack Compose that helps protect your device from potential threats.

## Features

- **App Blacklist**: Block unwanted applications from being installed
- **USB Debug Monitoring**: Monitor USB debugging status
- **Security Events Logging**: Track security-related events
- **Real-time Protection**: Toggle protection on/off
- **Modern UI**: Beautiful dark theme with Material Design 3



## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture principles
- **State Management**: Kotlin Flows + StateFlow
- **Data Persistence**: DataStore Preferences
- **Build System**: Gradle 8.2 with Kotlin DSL

## Requirements

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34

## Building

```bash
# Clone the repository
git clone https://github.com/xeroxll/guard.git

# Open in Android Studio or build from command line
./gradlew assembleDebug
```



## Permissions

The app requires the following permissions:
- INTERNET
- POST_NOTIFICATIONS
- RECEIVE_BOOT_COMPLETED
- QUERY_ALL_PACKAGES
- PACKAGE_USAGE_STATS
- FOREGROUND_SERVICE
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS



## License

MIT License
