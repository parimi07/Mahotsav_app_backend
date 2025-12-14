require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const cors = require('cors');
const socketIO = require('socket.io');
const admin = require('firebase-admin');
const http = require('http');

const app = express();
const server = http.createServer(app);
const io = socketIO(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin (only if service account file exists)
try {
  const serviceAccount = require('./firebase-service-account.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('✅ Firebase Admin initialized');
} catch (error) {
  console.log('⚠️ Firebase service account not found. Push notifications disabled.');
  console.log('   To enable: Download firebase-service-account.json from Firebase Console');
}

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
  gender: { type: String },
  registerId: { type: String },
  userType: { type: String },
  participationType: { type: String },
  paymentStatus: { type: String },
  amount: { type: Number, default: 200 },
  createdAt: { type: Date, default: Date.now }
}, { collection: 'registrations' });

const User = mongoose.model('User', UserSchema);
const Registration = mongoose.model('Registration', RegistrationSchema);

// Chat Schemas
const ChatMessageSchema = new mongoose.Schema({
  chatRoomId: { type: String, required: true, index: true },
  senderId: { type: String, required: true },
  senderName: { type: String, required: true },
  receiverId: { type: String, required: true },
  message: { type: String, required: true },
  timestamp: { type: Number, required: true },
  isRead: { type: Boolean, default: false },
  messageType: { type: String, default: 'text' }
}, { collection: 'chatMessages' });

const ChatRoomSchema = new mongoose.Schema({
  roomId: { type: String, required: true, unique: true },
  user1Id: String,
  user2Id: String,
  user1Name: String,
  user2Name: String,
  lastMessage: String,
  lastMessageTime: Number,
  unreadCount: { type: Number, default: 0 }
}, { collection: 'chatRooms' });

const UserTokenSchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true },
  fcmToken: String,
  isOnline: { type: Boolean, default: false },
  lastSeen: Number
}, { collection: 'userTokens' });

// Dedicated Chat Users Schema (separate from registrations)
const ChatUserSchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  name: { type: String, required: true },
  phone: { type: String },
  profilePic: { type: String, default: '' },
  createdAt: { type: Date, default: Date.now }
}, { collection: 'chatUsers' });

const ChatMessage = mongoose.model('ChatMessage', ChatMessageSchema);
const ChatRoom = mongoose.model('ChatRoom', ChatRoomSchema);
const UserToken = mongoose.model('UserToken', UserTokenSchema);
const ChatUser = mongoose.model('ChatUser', ChatUserSchema);

// Initialize chat users on server start
async function initializeChatUsers() {
  try {
    const count = await ChatUser.countDocuments();
    if (count === 0) {
      // Create default users
      await ChatUser.insertMany([
        { userId: '001', password: 'Maruthi', name: 'Admin', phone: '1234567890' },
        { userId: '002', password: 'test123', name: 'Test User', phone: '9876543210' },
        { userId: '003', password: 'user123', name: 'User Three', phone: '8765432109' }
      ]);
      console.log('✅ Chat users initialized (001: Admin, 002: Test User, 003: User Three)');
    }
  } catch (error) {
    console.log('⚠️ Chat users initialization skipped:', error.message);
  }
}

// Call after MongoDB connects
mongoose.connection.once('open', () => {
  initializeChatUsers();
});

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

// Helper Functions
async function sendPushNotification(receiverId, senderName, message, type = 'chat', senderId = null) {
  try {
    if (!admin.apps.length) {
      console.log('Firebase not initialized. Skipping push notification.');
      return;
    }

    const userToken = await UserToken.findOne({ userId: receiverId });
    
    if (!userToken || !userToken.fcmToken) {
      console.log('⚠️ No FCM token found for user:', receiverId);
      return;
    }
    
    // Use data-only payload for background delivery (works when app is closed)
    const payload = {
      data: {
        type: type,
        title: senderName,
        message: message,
        senderId: senderId || senderName,
        timestamp: Date.now().toString()
      },
      token: userToken.fcmToken,
      // Android-specific options for better delivery
      android: {
        priority: 'high',
        notification: {
          channelId: 'mahotsav_admin_channel',
          sound: 'default',
          priority: 'high'
        }
      }
    };
    
    await admin.messaging().send(payload);
    console.log(`✅ Push notification sent to ${receiverId} (Token: ${userToken.fcmToken.substring(0, 20)}...)`);
    
  } catch (error) {
    console.error('❌ Error sending push notification:', error.message);
  }
}

async function updateUserStatus(userId, isOnline) {
  try {
    await UserToken.findOneAndUpdate(
      { userId },
      { isOnline, lastSeen: Date.now() },
      { upsert: true }
    );
  } catch (error) {
    console.error('Error updating user status:', error.message);
  }
}

async function sendAdminNotification(title, body, userId) {
  try {
    if (!admin.apps.length) return;

    const adminTokens = await UserToken.find({ 
      userId: { $in: ['001', '002'] }
    });
    
    const tokens = adminTokens.map(t => t.fcmToken).filter(Boolean);
    
    if (tokens.length > 0) {
      const payload = {
        notification: { title, body },
        data: {
          type: 'registration',
          userId: userId || ''
        }
      };
      
      await admin.messaging().sendEachForMulticast({
        tokens,
        ...payload
      });
      
      console.log('✅ Admin notifications sent');
    }
  } catch (error) {
    console.error('Error sending admin notification:', error.message);
  }
}

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

// Chat login endpoint (uses ChatUser collection)
app.post('/api/chat-login', async (req, res) => {
  try {
    const { userId, password } = req.body;
    const user = await ChatUser.findOne({ userId });
    
    if (!user) {
      return res.status(401).json({ error: 'Invalid user ID' });
    }
    
    if (user.password !== password) {
      return res.status(401).json({ error: 'Invalid password' });
    }
    
    res.json({ 
      success: true,
      userId: user.userId,
      name: user.name,
      phone: user.phone
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/stats', authMiddleware, async (req, res) => {
  try {
    const totalRegistrations = await Registration.countDocuments();
    const totalMoney = await Registration.aggregate([
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    const todayRegistrations = await Registration.countDocuments({
      createdAt: { $gte: new Date(new Date().setHours(0, 0, 0, 0)) }
    });
    const todayMoney = await Registration.aggregate([
      {
        $match: {
          createdAt: { $gte: new Date(new Date().setHours(0, 0, 0, 0)) }
        }
      },
      { $group: { _id: null, total: { $sum: '$amount' } } }
    ]);
    
    res.json({
      totalRegistrations,
      totalMoney: totalMoney[0]?.total || 0,
      todayRegistrations,
      todayMoney: todayMoney[0]?.total || 0
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

app.post('/api/register', async (req, res) => {
  try {
    const { name, email, phone, college } = req.body;
    
    // Generate next userId
    const lastRegistration = await Registration.findOne().sort({ createdAt: -1 });
    let nextNumber = 1;
    
    if (lastRegistration && lastRegistration.userId) {
      const lastNumber = parseInt(lastRegistration.userId.replace('MH26', ''));
      nextNumber = lastNumber + 1;
    }
    
    const userId = `MH26${String(nextNumber).padStart(6, '0')}`;
    
    const registration = await Registration.create({
      userId,
      name,
      email,
      phone,
      college,
      amount: 200
    });
    
    // Send push notification to admins
    sendAdminNotification(
      '🎉 New Registration!',
      `${name} just registered for Mahotsav 2025`,
      userId
    );
    
    res.status(201).json(registration);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

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

// Chat & Notification Endpoints
app.post('/api/notifications/register-token', async (req, res) => {
  try {
    const { userId, fcmToken } = req.body;
    
    await UserToken.findOneAndUpdate(
      { userId },
      { fcmToken, lastSeen: Date.now() },
      { upsert: true }
    );
    
    res.json({ success: true, message: 'Token registered' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/chat/rooms/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const rooms = await ChatRoom.find({
      $or: [{ user1Id: userId }, { user2Id: userId }]
    }).sort({ lastMessageTime: -1 });
    
    res.json(rooms);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get all chat users (for user list)
app.get('/api/chat/users', async (req, res) => {
  try {
    const users = await ChatUser.find({})
      .select('userId name phone profilePic')
      .sort({ name: 1 });
    
    res.json(users);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get specific chat user
app.get('/api/chat/users/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const user = await ChatUser.findOne({ userId })
      .select('userId name phone profilePic');
    
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    res.json(user);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/chat/messages/:chatRoomId', async (req, res) => {
  try {
    const { chatRoomId } = req.params;
    const limit = parseInt(req.query.limit) || 100;
    
    const messages = await ChatMessage.find({ chatRoomId })
      .sort({ timestamp: 1 })
      .limit(limit);
    
    res.json(messages);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    message: 'Server is running',
    features: {
      mongodb: mongoose.connection.readyState === 1,
      socketio: true,
      firebase: admin.apps.length > 0
    }
  });
});

// Track active chat rooms for users (to prevent notifications when chat is open)
const activeChats = new Map(); // userId -> chatRoomId

// Walkie-talkie state management (stateless relay)
const walkieTalkieUsers = new Map(); // socketId -> { userId, userName }
let activeSpeaker = null;

// Socket.io Connection Handling
io.on('connection', (socket) => {
  console.log('✅ User connected:', socket.id);
  
  // Join chat room
  socket.on('join_chat', async (data) => {
    const { userId, userName, chatRoomId, otherUserId } = data;
    socket.join(chatRoomId);
    socket.userId = userId; // Store userId in socket
    
    // Track that this user is viewing this chat
    activeChats.set(userId, chatRoomId);
    
    console.log(`User ${userId} (${userName}) joined chat room ${chatRoomId}`);
    console.log(`Active chats tracked: ${activeChats.size}`);
    
    // Update online status
    updateUserStatus(userId, true);
    io.to(chatRoomId).emit('user_status', { userId, isOnline: true });
    
    // NO join notification - WhatsApp doesn't notify when someone opens chat
  });
  
  // Send message
  socket.on('send_message', async (data) => {
    const { chatRoomId, senderId, senderName, receiverId, message, timestamp } = data;
    
    try {
      // Save message to MongoDB
      const chatMessage = new ChatMessage({
        chatRoomId,
        senderId,
        senderName,
        receiverId,
        message,
        timestamp,
        isRead: false
      });
      
      await chatMessage.save();
      
      // Update or create chat room
      await ChatRoom.findOneAndUpdate(
        { roomId: chatRoomId },
        {
          roomId: chatRoomId,
          user1Id: senderId,
          user2Id: receiverId,
          user1Name: senderName,
          user2Name: 'User', // You can fetch from database
          lastMessage: message,
          lastMessageTime: timestamp,
          $inc: { unreadCount: 1 }
        },
        { upsert: true }
      );
      
      // Broadcast message to chat room
      io.to(chatRoomId).emit('receive_message', data);
      
      // Only send push notification if receiver is NOT currently viewing this chat
      const receiverActiveChat = activeChats.get(receiverId);
      if (receiverActiveChat !== chatRoomId) {
        // Receiver is not in this chat (app closed or viewing different chat)
        sendPushNotification(receiverId, senderName, message, 'chat', senderId);
        console.log(`📲 Push notification sent to ${receiverId} (app closed or different chat)`);
      } else {
        console.log(`✓ No notification - ${receiverId} is viewing the chat`);
      }
      
      console.log(`Message sent in room ${chatRoomId}`);
    } catch (error) {
      console.error('Error saving message:', error.message);
    }
  });
  
  // Load messages
  socket.on('load_messages', async (data) => {
    const { chatRoomId } = data;
    
    try {
      const messages = await ChatMessage.find({ chatRoomId })
        .sort({ timestamp: 1 })
        .limit(100);
      
      socket.emit('messages_loaded', messages);
    } catch (error) {
      console.error('Error loading messages:', error.message);
      socket.emit('messages_loaded', []);
    }
  });
  
  // Typing indicators
  socket.on('user_typing', (data) => {
    console.log('User typing:', data.userId, 'in room:', data.chatRoomId);
    socket.to(data.chatRoomId).emit('user_typing', { userId: data.userId });
  });
  
  socket.on('user_stopped_typing', (data) => {
    console.log('User stopped typing:', data.userId, 'in room:', data.chatRoomId);
    socket.to(data.chatRoomId).emit('user_stopped_typing', { userId: data.userId });
  });
  
  // Leave chat room
  socket.on('leave_chat', (data) => {
    const { userId, chatRoomId } = data;
    
    // Remove from active chats tracking
    activeChats.delete(userId);
    console.log(`User ${userId} left chat room ${chatRoomId}`);
    console.log(`Active chats tracked: ${activeChats.size}`);
  });
  
  // Mark messages as read
  socket.on('mark_as_read', async (data) => {
    try {
      const { chatRoomId, userId } = data;
      console.log('Marking messages as read for:', userId, 'in room:', chatRoomId);
      
      // Update all unread messages in this chat room where user is receiver
      await ChatMessage.updateMany(
        { 
          chatRoomId: chatRoomId, 
          receiverId: userId,
          isRead: false
        },
        { isRead: true }
      );
      
      // Update chat room unread count
      await ChatRoom.updateOne(
        { roomId: chatRoomId },
        { unreadCount: 0 }
      );
      
      console.log('✅ Messages marked as read for room:', chatRoomId);
      
      // Notify other user about read receipts
      socket.to(chatRoomId).emit('messages_read', { userId: userId });
      
    } catch (error) {
      console.error('❌ Error marking messages as read:', error);
    }
  });
  
  // ========== WALKIE-TALKIE HANDLERS (Optimized with binary support) ==========
  
  // Join walkie-talkie (with user info)
  socket.on('join-walkie', (data) => {
    const { userId, userName } = data;
    walkieTalkieUsers.set(socket.id, { userId, userName });
    
    // Broadcast active user count
    const activeCount = walkieTalkieUsers.size;
    io.emit('active-users', activeCount);
    
    // Notify others
    socket.broadcast.emit('user-joined', { userId, userName });
    
    console.log(`📻 ${userName} joined walkie-talkie (Total: ${activeCount})`);
  });
  
  // Audio streaming (supports both Opus and PCM)
  socket.on('audio-stream', (data) => {
    // Stateless relay: Just forward the audio data to all other clients
    // data can be either Base64 string (legacy) or JSON with { audio, format }
    socket.broadcast.emit('audio-data', data);
  });
  
  // Binary audio streaming (most efficient)
  socket.on('audio-binary', (buffer) => {
    // Pure binary forwarding - lowest latency
    socket.broadcast.emit('audio-binary', buffer);
  });
  
  // User speaking notification (for UI updates)
  socket.on('user-speaking', (data) => {
    const userInfo = walkieTalkieUsers.get(socket.id);
    
    if (data.speaking) {
      activeSpeaker = userInfo ? userInfo.userName : 'Unknown';
      io.emit('user-speaking', { 
        speaking: true, 
        userId: userInfo ? userInfo.userId : socket.id,
        userName: activeSpeaker
      });
    } else {
      activeSpeaker = null;
      io.emit('user-speaking', { speaking: false });
    }
  });
  
  // Leave walkie-talkie
  socket.on('leave-walkie', () => {
    const userInfo = walkieTalkieUsers.get(socket.id);
    
    if (userInfo) {
      walkieTalkieUsers.delete(socket.id);
      
      // Broadcast updated count
      io.emit('active-users', walkieTalkieUsers.size);
      
      // Notify others
      socket.broadcast.emit('user-left', { 
        userId: userInfo.userId, 
        userName: userInfo.userName 
      });
      
      console.log(`📻 ${userInfo.userName} left walkie-talkie (Total: ${walkieTalkieUsers.size})`);
    }
  });
  
  // ========== END WALKIE-TALKIE HANDLERS ==========
  
  // Handle disconnect
  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
    
    // Remove from active chats tracking
    if (socket.userId) {
      activeChats.delete(socket.userId);
      console.log(`Removed ${socket.userId} from active chats`);
    }
    
    // Remove from walkie-talkie if connected
    const userInfo = walkieTalkieUsers.get(socket.id);
    if (userInfo) {
      walkieTalkieUsers.delete(socket.id);
      io.emit('active-users', walkieTalkieUsers.size);
      socket.broadcast.emit('user-left', { 
        userId: userInfo.userId, 
        userName: userInfo.userName 
      });
      console.log(`📻 ${userInfo.userName} disconnected from walkie-talkie`);
    }
  });
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
server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
  console.log(`📊 MongoDB: ${mongoose.connection.readyState === 1 ? '✅ Connected' : '⚠️ Disconnected'}`);
  console.log(`💬 Socket.io: ✅ Enabled`);
  console.log(`🔔 Firebase: ${admin.apps.length > 0 ? '✅ Enabled' : '⚠️ Disabled'}`);
  
  setTimeout(() => initializeAdmin(), 1000);
});

server.on('error', (error) => {
  console.error('❌ Server error:', error);
});

// Keep server alive
server.keepAliveTimeout = 61 * 1000;
server.headersTimeout = 65 * 1000;
