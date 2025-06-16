const Device = require('../models/deviceModel');
const WebSocket = require('ws');

let wss;

function setupWebSocket(server) {
    wss = new WebSocket.Server({ server });
    
    wss.on('connection', (ws) => {
        ws.deviceId = null;
        
        ws.on('message', async (message) => {
            try {
                const data = JSON.parse(message);
                
                if (data.device_id && data.ip_address) {
                    // 设备注册
                    ws.deviceId = data.device_id;
                    
                    // 更新或创建设备记录
                    await Device.createOrUpdate(
                        data.device_id, 
                        data.ip_address
                    );
                    
                    console.log(`Device connected: ${data.device_id}`);
                }
            } catch (error) {
                console.error('Error processing WebSocket message:', error);
            }
        });
        
        ws.on('close', () => {
            console.log(`Device disconnected: ${ws.deviceId}`);
        });
    });
}

function broadcastUrlUpdate(deviceId) {
    if (!wss) return;
    
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN && client.deviceId === deviceId) {
            client.send(JSON.stringify({
                type: 'url_update',
                device_id: deviceId,
                timestamp: new Date().toISOString()
            }));
        }
    });
}

module.exports = {
    setupWebSocket,
    broadcastUrlUpdate
};