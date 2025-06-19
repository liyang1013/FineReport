const WebSocket = require('ws');

let wssInstance = null;

/**
 * 初始化 WebSocket 服务器
 * @param {WebSocket.Server} wss
 */
function initializeWebSocket(wss) {
    wssInstance = wss;
}

/**
 * 向特定设备广播消息
 * @param {string} deviceId 目标设备ID
 * @param {object} message 要发送的消息对象  url_update | report_device_ip | show_device_info | clear_container | device_register
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