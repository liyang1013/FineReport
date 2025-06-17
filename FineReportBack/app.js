require('dotenv').config();
const express = require('express');
const http = require('http');
const bodyParser = require('body-parser');
const cors = require('cors');
const ApiResponse = require('./utils/response');
const deviceRoutes = require('./routes/deviceRoutes');
const { setupWebSocket, broadcastUrlUpdate } = require('./controller/websocketController');


const app = express();
const server = http.createServer(app);

app.use(cors());
app.use(bodyParser.json());

app.use('/api/devices', deviceRoutes);

setupWebSocket(server);

app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json(ApiResponse.error('Something went wrong!' ));
});

const PORT = process.env.SERVER_PORT;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

module.exports = { app, broadcastUrlUpdate };