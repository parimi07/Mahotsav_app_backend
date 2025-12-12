# Event Admin - Native Android App

A native Android application for managing event registrations with real-time statistics and home screen widget.

## Features

✅ **Login System** - Secure JWT authentication
✅ **Dashboard** - Display series ID (MH26XXXXXX format) and statistics
✅ **Registration Form** - Create new registrations with auto-incrementing series IDs
✅ **Statistics Cards** - Total, Today, This Month, Total Money
✅ **Home Screen Widget** - Auto-updating money display (refreshes every 15 minutes)
✅ **Pull to Refresh** - Swipe down to refresh dashboard data

## Project Structure

```
EventAdminApp_Native/
├── app/
│   ├── src/main/
│   │   ├── java/com/eventadmin/
│   │   │   ├── LoginActivity.java          # Login screen with authentication
│   │   │   ├── DashboardActivity.java      # Main dashboard with stats and form
│   │   │   ├── MoneyWidgetProvider.java    # Widget provider
│   │   │   └── WidgetUpdateWorker.java     # Background worker for widget updates
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_login.xml      # Login screen layout
│   │   │   │   ├── activity_dashboard.xml  # Dashboard layout with cards
│   │   │   │   └── widget_money.xml        # Widget layout
│   │   │   ├── values/
│   │   │   │   ├── colors.xml              # Color definitions
│   │   │   │   ├── strings.xml             # String resources
│   │   │   │   └── themes.xml              # App themes
│   │   │   ├── drawable/
│   │   │   │   └── widget_background.xml   # Widget background shape
│   │   │   ├── xml/
│   │   │   │   └── widget_info.xml         # Widget configuration
│   │   │   └── menu/
│   │   │       └── dashboard_menu.xml      # Dashboard menu (logout)
│   │   └── AndroidManifest.xml             # App manifest
│   ├── build.gradle                        # App-level Gradle config
│   └── proguard-rules.pro                  # ProGuard rules
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle wrapper config
├── build.gradle                            # Project-level Gradle config
├── gradle.properties                       # Gradle properties
└── settings.gradle                         # Gradle settings
```

## Prerequisites

- **Android Studio** (Arctic Fox or later)
- **JDK 17** (already installed at `C:\Program Files\Java\jdk-17`)
- **Android SDK** with API 34
- **Backend Server** running at `http://localhost:3000` (or your IP address)

## Backend Configuration

The backend is already set up in the `EventAdminApp/backend` folder with:
- MongoDB Atlas connection
- JWT authentication
- Series ID auto-increment (MH26XXXXXX format)
- All required API endpoints

### Start the Backend

```bash
cd EventAdminApp/backend
npm install
npm start
```

Backend runs on port 3000.

## Building the App

### Option 1: Using Android Studio (Recommended)

1. **Open Project**
   - Launch Android Studio
   - Click "Open" and select the `EventAdminApp_Native` folder
   - Wait for Gradle sync to complete

2. **Update API URL** (if testing on physical device)
   - Open `LoginActivity.java` and `DashboardActivity.java`
   - Replace `http://10.0.2.2:3000/api` with `http://YOUR_COMPUTER_IP:3000/api`
   - Find your IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)

3. **Build APK**
   - Click `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Run on Device/Emulator**
   - Connect device via USB (enable USB Debugging)
   - OR start Android Emulator
   - Click the green "Run" button (▶️)

### Option 2: Using Command Line

```bash
cd EventAdminApp_Native

# Build debug APK
gradlew.bat assembleDebug

# Install on connected device
gradlew.bat installDebug

# Build and install in one command
gradlew.bat build installDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

## API Endpoints Used

- `POST /api/login` - User authentication
- `GET /api/stats` - Dashboard statistics
- `GET /api/current-series` - Next series ID
- `POST /api/register` - Create new registration
- `GET /api/widget/money` - Total money for widget

## Default Login Credentials

**Email:** admin@example.com  
**Password:** admin123

## Adding the Widget

1. Install and open the app
2. Long-press on home screen
3. Tap "Widgets"
4. Find "Event Admin" widget
5. Drag to home screen
6. Widget auto-updates every 15 minutes

## Troubleshooting

### Cannot connect to backend

**For Emulator:**
- Use `http://10.0.2.2:3000/api` (already configured)
- This is the emulator's special alias for localhost

**For Physical Device:**
- Both device and computer must be on same WiFi
- Update API_BASE_URL in Java files to `http://YOUR_COMPUTER_IP:3000/api`
- Disable firewall or allow port 3000
- Ensure backend is running: `npm start` in backend folder

### Gradle sync fails

```bash
# Clean and rebuild
gradlew.bat clean
gradlew.bat build
```

### Widget not updating

- Ensure backend is accessible
- Check background restrictions in phone settings
- Widget updates every 15 minutes automatically

## Technology Stack

- **Java** - Native Android development
- **Material Design** - Modern UI components
- **Volley** - HTTP networking
- **WorkManager** - Background widget updates
- **SharedPreferences** - Local storage for JWT tokens
- **CardView** - Material card components
- **SwipeRefreshLayout** - Pull-to-refresh functionality

## Architecture

```
┌─────────────┐
│ LoginScreen │ → JWT Token → SharedPreferences
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Dashboard  │ → API Calls → Backend (Node.js)
└──────┬──────┘               │
       │                      │
       │                      ▼
       │                  MongoDB Atlas
       │
       ▼
┌─────────────┐
│   Widget    │ → WorkManager → Backend API
└─────────────┘   (15 min)
```

## Next Steps

1. **Customize Colors** - Edit `res/values/colors.xml`
2. **Add App Icon** - Replace default launcher icons in `mipmap-*` folders
3. **Generate Release APK** - Sign with keystore for production
4. **Add More Features** - Export data, push notifications, etc.

## Notes

- Widget refreshes every 15 minutes automatically
- Data is cached in SharedPreferences for offline display
- Uses `android:usesCleartextTraffic="true"` for local HTTP testing
- For production, use HTTPS and remove cleartext traffic permission
