const WebSocket = require('ws');
const Device = require('@/models/deviceModel');
const { broadcastToDevice, initializeWebSocket } = require('@/utils/websocketUtils'); 
let wss;

function setupWebSocket(server) {
    wss = new WebSocket.Server({ server });
    
    initializeWebSocket(wss);
    wss.on('connection', (ws) => {
        ws.deviceId = null;
        ws.on('message', async (message) => {
            try {
                const data = JSON.parse(message);
                if (data.device_id && data.ip_address) {
                    ws.deviceId = data.device_id;
                    await Device.createOrUpdate(data.device_id, data.ip_address);
                    console.log(`Device connected: ${data.device_id}`);
                }
            } catch (error) {
                console.error('Error processing message:', error);
            }
        });
    });
}

function notifyDevice(deviceId, type) {
    console.log(type)
    broadcastToDevice(deviceId, {
        type: type,
        device_id: deviceId,
        timestamp: new Date().toISOString()
    });
}

module.exports = {
    setupWebSocket,
    notifyDevice
};