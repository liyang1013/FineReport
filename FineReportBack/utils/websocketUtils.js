const WebSocket = require('ws');

// 存储 WebSocket 服务器实例
let wssInstance = null;

/**
 * 初始化 WebSocket 服务器
 * @param {WebSocket.Server} wss - WebSocket 服务器实例
 */
function initializeWebSocket(wss) {
    wssInstance = wss;
}

/**
 * 向特定设备广播消息
 * @param {string} deviceId - 目标设备ID
 * @param {object} message - 要发送的消息对象
 */
function broadcastToDevice(deviceId, message) {
    if (!wssInstance) {
        console.error('WebSocket server not initialized');
        return;
    }

    const messageString = JSON.stringify(message);
    
    wssInstance.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN && client.deviceId === deviceId) {
            client.send(messageString);
            console.log(`Message sent to device ${deviceId}:`, message);
        }
    });
}

module.exports = {
    initializeWebSocket,
    broadcastToDevice
};