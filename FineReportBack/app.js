require('dotenv').config();
const express = require('express');
const http = require('http');
const bodyParser = require('body-parser');
const cors = require('cors');
const deviceRoutes = require('./routes/deviceRoutes');
const { setupWebSocket, broadcastUrlUpdate } = require('./controller/websocketController');

const app = express();
const server = http.createServer(app);

// 中间件
app.use(cors());
app.use(bodyParser.json());

// API路由
app.use('/api/devices', deviceRoutes);

// WebSocket设置
setupWebSocket(server);

// 错误处理
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something went wrong!' });
});

// 启动服务器
const PORT = process.env.SERVER_PORT;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

module.exports = { app, broadcastUrlUpdate };