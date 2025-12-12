# Event Admin Backend

Backend API for Event Admin Dashboard mobile app.

## Features
- MongoDB Atlas connection
- User registration tracking
- Real-time userId generation
- Auto-incrementing series ID (MH26XXXXXX format)

## Environment Variables
- `MONGODB_URI` - MongoDB Atlas connection string
- `JWT_SECRET` - Secret key for JWT tokens
- `PORT` - Server port (default: 3000)

## Deployment

### Render
1. Push code to GitHub
2. Create new Web Service on Render
3. Connect your GitHub repo
4. Add environment variables:
   - `MONGODB_URI`: Your MongoDB Atlas connection string
   - `JWT_SECRET`: Random secure string
5. Deploy!

### Railway
1. Install Railway CLI: `npm install -g @railway/cli`
2. Login: `railway login`
3. Init: `railway init`
4. Add variables: `railway variables set MONGODB_URI=your_uri`
5. Deploy: `railway up`

## Local Development
```bash
npm install
npm start
```

## API Endpoints
- `GET /api/current-series` - Get latest userId
- `POST /api/register` - Register new user
- `GET /api/health` - Health check
