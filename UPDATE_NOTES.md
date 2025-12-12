# Event Admin App - Updated Version

## ✅ Latest Changes

### Login Credentials (Mock Authentication)
- **User ID:** `001`
- **Password:** `Maruthi`

No backend required for login - credentials are hardcoded for quick access.

### Dashboard Features

#### 1. **Latest User ID Display (Main Highlight)**
- Shows the **latest userId from MongoDB** in large, centered display
- Format: `MH26000001` series
- **Animated digit transition** when new user is added
- Auto-refreshes every 5 seconds to show latest ID
- Orange highlighted card in the center of dashboard

#### 2. **Live Data from MongoDB Atlas**
- Connects directly to MongoDB Atlas cluster
- Fetches data from `test.registrations` collection
- Shows real-time user count and statistics
- No Node.js backend needed for viewing data

### Technical Implementation

#### Files Modified:
1. **LoginActivity.java** - Mock credential validation (001/Maruthi)
2. **DashboardActivity.java** - MongoDB Data API integration with animated userId display
3. **activity_dashboard.xml** - Enhanced UI for latest userId card

#### MongoDB Integration:
- **Endpoint:** MongoDB Data API (HTTPS)
- **Collection:** `test.registrations`
- **Field:** `userId` (MH26XXXXXX format)
- **Auto-refresh:** Every 5 seconds
- **Animation:** 1.5-second smooth transition between numbers

### How It Works:

1. **Login:** Enter ID `001` and Password `Maruthi`
2. **Dashboard Opens:** Shows latest userId from your MongoDB
3. **Live Updates:** When someone adds a new registration in MongoDB, the userId animates to the new value within 5 seconds
4. **Digit Animation:** Numbers flip smoothly like a timer/counter

### APK Location:
```
C:\Users\maruthi velaga\Desktop\admin\EventAdminApp_Native\app\build\outputs\apk\debug\app-debug.apk
```

### Install & Test:
```powershell
cd "C:\Users\maruthi velaga\Desktop\admin\EventAdminApp_Native"
.\gradlew.bat installDebug
```

### Test the Animation:
1. Login to app
2. Open MongoDB Compass or Atlas web interface
3. Add a new document to `test.registrations` collection with userId `MH26000013`
4. Watch the app - within 5 seconds, the digits will animate from current ID to new ID!

---

**Note:** The app now fetches data directly from MongoDB Atlas using Data API, so no Node.js backend is needed for viewing. The registration feature still requires backend if you want to add new users from the app.
