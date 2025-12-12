require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// MongoDB Connection with better options
mongoose.connect(process.env.MONGODB_URI, {
  serverSelectionTimeoutMS: 30000,
  socketTimeoutMS: 45000,
  family: 4, // Force IPv4
})
  .then(() => {
    console.log('✅ Connected to MongoDB Atlas');
    mongoose.connection.on('error', err => {
      console.error('MongoDB connection error:', err);
    });
  })
  .catch(err => {
    console.error('❌ MongoDB connection error:', err);
    console.log('⚠️ Server will continue without MongoDB connection');
  });

// Schemas
const UserSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  role: { type: String, default: 'admin' }
});

const RegistrationSchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true },
  name: { type: String, required: true },
  email: { type: String, required: true },
  password: { type: String },
  phone: { type: String, required: true },
  college: { type: String },
  dateOfBirth: { type: String },
  gender: { type: String, enum: ['Male', 'Female', 'Other'] },
  registerId: { type: String },
  userType: { type: String, enum: ['Person-A', 'Person-B'], comment: 'Person-A: Registration Only, Person-B: Participation + Registration' },
  participationType: { type: String },
  paymentStatus: { type: String, enum: ['Paid', 'Unpaid', 'Pending'], default: 'Unpaid' },
  amount: { type: Number, default: 0 },
  amountToBePaid: { type: Number, default: 0 },
  coordinator: { type: String },
  coordinatorPhone: { type: String },
  paymentDate: { type: Date },
  paymentMode: { type: String, enum: ['Cash', 'UPI', 'Card', 'Other'] },
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
}, { collection: 'registrations' });

const User = mongoose.model('User', UserSchema);
const Registration = mongoose.model('Registration', RegistrationSchema);

// Auth Middleware
const authMiddleware = (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'No token provided' });
  
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.userId = decoded.userId;
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid token' });
  }
};

// Initialize default admin user
async function initializeAdmin() {
  try {
    if (mongoose.connection.readyState !== 1) {
      console.log('⚠️ Skipping admin initialization - MongoDB not connected');
      return;
    }
    const adminExists = await User.findOne({ email: 'admin@example.com' }).maxTimeMS(5000);
    if (!adminExists) {
      const hashedPassword = await bcrypt.hash('admin123', 10);
      await User.create({
        email: 'admin@example.com',
        password: hashedPassword,
        role: 'admin'
      });
      console.log('✅ Default admin user created (admin@example.com / admin123)');
    }
  } catch (error) {
    console.log('⚠️ Could not initialize admin user:', error.message);
  }
}

// Routes
app.post('/api/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email });
    
    if (!user) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    
    const isValidPassword = await bcrypt.compare(password, user.password);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    
    const token = jwt.sign({ userId: user._id }, process.env.JWT_SECRET, { expiresIn: '24h' });
    res.json({ token, email: user.email });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/stats', async (req, res) => {
  try {
    const now = new Date();
    const startOfDay = new Date(now.setHours(0, 0, 0, 0));
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    
    const totalRegistrations = await Registration.countDocuments();
    const totalMoney = await Registration.aggregate([
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    
    const todayRegistrations = await Registration.countDocuments({
      createdAt: { $gte: startOfDay }
    });
    
    const monthRegistrations = await Registration.countDocuments({
      createdAt: { $gte: startOfMonth }
    });
    
    res.json({
      totalRegistrations,
      totalMoney: totalMoney[0]?.total || 0,
      todayRegistrations,
      monthRegistrations
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/current-series', async (req, res) => {
  try {
    const lastRegistration = await Registration.findOne().sort({ createdAt: -1 });
    
    if (lastRegistration && lastRegistration.userId) {
      res.json({ seriesId: lastRegistration.userId });
    } else {
      res.json({ seriesId: 'MH26000001' });
    }
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// READ-ONLY APP - Modification endpoints disabled

app.get('/api/registrations', async (req, res) => {
  try {
    const registrations = await Registration.find()
      .sort({ userId: 1 })
      .select('userId name college amount coordinator paymentStatus amountToBePaid createdAt');
    res.json({ registrations });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// READ-ONLY APP - Registration endpoint disabled
// Data can only be added/modified through the main event registration website

app.get('/api/widget/money', async (req, res) => {
  try {
    const totalMoney = await Registration.aggregate([
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    
    res.json({ totalMoney: totalMoney[0]?.total || 0 });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Server is running' });
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  console.error('❌ Unhandled Rejection at:', promise, 'reason:', reason);
  // Don't exit on unhandled rejection
});

process.on('uncaughtException', (error) => {
  console.error('❌ Uncaught Exception:', error);
  // Don't exit on uncaught exception
});

// Start server
const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
  console.log(`📊 MongoDB Status: ${mongoose.connection.readyState === 1 ? 'Connected' : 'Disconnected'}`);
  // Don't wait for MongoDB - initialize admin in background
  setTimeout(() => initializeAdmin(), 1000);
});

server.on('error', (error) => {
  console.error('❌ Server error:', error);
});

// Keep server alive
server.keepAliveTimeout = 61 * 1000;
server.headersTimeout = 65 * 1000;
