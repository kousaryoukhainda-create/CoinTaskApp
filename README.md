# 💰 CoinTaskApp

A complete task rewards platform for Android built with Kotlin and Material Design 3.

## Overview

CoinTask is a multi-role platform where:
- **Users** can complete tasks and earn coins
- **Advertisers** can create campaigns and track ROI
- **Admins** can manage the platform and monitor metrics

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **UI**: Material Design 3, ViewBinding
- **Local Storage**: SharedPreferences
- **Networking**: Retrofit + OkHttp
- **Database**: Room

## Project Structure

```
app/src/main/java/com/cointask/
├── auth/           # Login & Register activities
├── user/           # User dashboard
├── advertiser/     # Advertiser dashboard
├── admin/          # Admin panel
├── utils/          # Utility classes
└── data/models/    # Data models
```

## Demo Credentials

The app uses demo authentication based on email patterns:

| Email | Role |
|-------|------|
| user@example.com | User |
| advertiser@example.com | Advertiser |
| admin@example.com | Admin |

*Any password works for demo purposes*

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Build & Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on emulator or physical device

```bash
./gradlew assembleDebug
```

## Features

### User Features
- View coin balance
- Level progress tracking
- Withdraw coins
- Transaction history

### Advertiser Features
- Campaign management
- Budget tracking
- ROI analytics
- Task completion metrics

### Admin Features
- Platform overview
- User/advertiser management
- Revenue tracking
- Suspicious activity monitoring

## License

MIT License
