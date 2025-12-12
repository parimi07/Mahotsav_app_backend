# Event Admin App - Pricing Configuration

## Current Pricing Logic

The app now supports flexible pricing based on registration data stored in MongoDB:

### Pricing Categories

#### Male Category:
- **Person-A (Registration Only)**: ₹250
- **Person-B (Participation + Registration)**: ₹350

#### Female Category:
- **All Types**: ₹250

### How It Works

1. **Backend Storage**: Each registration in MongoDB has an `amount` field that stores the actual price (250 or 350)
2. **Total Money Calculation**: The backend `/api/stats` endpoint sums all `amount` values from registrations
3. **Widget Display**: Total money is automatically synced to the home screen widget every 15 minutes

## External Backend URL (Placeholder)

For future integration with an external website backend:

**Location to Update**: 
- File: `DashboardActivity.java`
- Line: ~47 (API_BASE_URL constant)

**Current Value**:
```java
private static final String API_BASE_URL = "http://10.10.197.159:3000/api";
```

**To integrate with external backend**:
```java
private static final String API_BASE_URL = "https://your-website-backend.com/api";
```

### Required External Backend Endpoints:

1. **GET /api/stats** - Returns:
```json
{
  "totalRegistrations": 15,
  "totalMoney": 4000,
  "todayRegistrations": 3,
  "monthRegistrations": 15
}
```

2. **GET /api/registrations** - Returns:
```json
{
  "registrations": [
    {
      "userId": "MH26000001",
      "name": "Student Name",
      "college": "College Name",
      "amount": 250,
      "coordinator": "Coordinator Name"
    }
  ]
}
```

3. **GET /api/current-series** - Returns:
```json
{
  "seriesId": "MH26000015"
}
```

## Registration Submission

When submitting a registration, include:
```json
{
  "name": "Student Name",
  "email": "student@example.com",
  "phone": "1234567890",
  "college": "College Name",
  "amount": 250,  // or 350 based on type
  "coordinator": "Coordinator Name"
}
```

## Notes

- All amounts are in Indian Rupees (₹)
- The widget updates every 15 minutes automatically
- Today/Month statistics are calculated based on registration timestamps
- Search functionality in LIST view searches across: MH_ID, Name, College, and Coordinator
