# Event Admin - Quick Start Guide

## Build and Run (5 Minutes)

### Option 1: Android Studio (Easiest)

1. **Start Backend**
   ```bash
   cd EventAdminApp/backend
   npm start
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click "Open" → Select `EventAdminApp_Native` folder
   - Wait for Gradle sync (2-3 minutes first time)

3. **Run**
   - Connect Android device OR start emulator
   - Click green Run button (▶️)
   - App will install and launch automatically

### Option 2: Command Line

1. **Start Backend**
   ```bash
   cd EventAdminApp/backend
   npm start
   ```

2. **Build APK**
   ```bash
   cd EventAdminApp_Native
   build.bat
   ```
   
3. **Install** (device connected via USB)
   ```bash
   gradlew.bat installDebug
   ```

## Default Login

- **Email:** admin@example.com
- **Password:** admin123

## Testing on Physical Device

If testing on a real phone (not emulator), update API URLs:

**Edit these files:**
- `app/src/main/java/com/eventadmin/LoginActivity.java`
- `app/src/main/java/com/eventadmin/DashboardActivity.java`
- `app/src/main/java/com/eventadmin/WidgetUpdateWorker.java`

**Change:**
```java
private static final String API_BASE_URL = "http://10.0.2.2:3000/api";
```

**To:**
```java
private static final String API_BASE_URL = "http://YOUR_COMPUTER_IP:3000/api";
```

**Find your IP:**
- Windows: Open Command Prompt → `ipconfig` → Look for "IPv4 Address"
- Example: `http://192.168.1.100:3000/api`

## Widget Setup

1. Install app and login
2. Go to home screen
3. Long-press → Widgets
4. Find "Event Admin" widget
5. Drag to home screen
6. Auto-updates every 15 minutes

## Features Working

✅ Login with JWT authentication  
✅ Dashboard with 4 stat cards  
✅ Series ID display (MH26XXXXXX)  
✅ New registration form  
✅ Auto-increment series IDs  
✅ Home screen widget  
✅ Pull-to-refresh  
✅ Logout menu  

## Troubleshooting

**"Cannot connect to server"**
- Emulator: URL should be `http://10.0.2.2:3000/api`
- Physical device: Use your computer's IP address
- Ensure backend is running: `npm start` in backend folder

**Build fails**
```bash
gradlew.bat clean
gradlew.bat build
```

**Widget not showing**
- Ensure app is installed
- Check Settings → Apps → Event Admin → Permissions
- Widget updates every 15 minutes (first update may take time)

## What's Different from React Native

🚀 **Faster Build** - No C++ compilation, just Java  
🚀 **Smaller APK** - ~5MB vs 30MB+  
🚀 **No NDK Issues** - Pure Java, no native dependencies  
🚀 **Better Performance** - Direct Android API access  
🚀 **Easier Debugging** - Standard Android Studio tools  

## Next Steps

1. Customize UI colors in `res/values/colors.xml`
2. Add app icon (replace files in `res/mipmap-*` folders)
3. Test on real device
4. Build release APK for production
