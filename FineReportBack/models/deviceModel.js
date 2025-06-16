const pool = require('../config/db');

const Device = {
    async findByDeviceId(deviceId) {
        const [rows] = await pool.query(
            'SELECT * FROM devices WHERE device_id = ?', 
            [deviceId]
        );
        return rows[0];
    },

    async createOrUpdate(deviceId, ipAddress, url = null) {
        const device = await this.findByDeviceId(deviceId);
        
        if (device) {
            // 更新现有设备
            await pool.query(
                'UPDATE devices SET ip_address = ?, last_seen = NOW()' + 
                (url ? ', current_url = ?' : '') + 
                ' WHERE device_id = ?',
                url ? [ipAddress, url, deviceId] : [ipAddress, deviceId]
            );
            
            if (url) {
                // 记录URL变更历史
                await pool.query(
                    'INSERT INTO url_history (device_id, url) VALUES (?, ?)',
                    [deviceId, url]
                );
            }
        } else {
            // 创建新设备
            await pool.query(
                'INSERT INTO devices (device_id, ip_address, current_url) VALUES (?, ?, ?)',
                [deviceId, ipAddress, url || '']
            );
        }
        
        return this.findByDeviceId(deviceId);
    },

    async updateUrl(deviceId, url) {
        await pool.query(
            'UPDATE devices SET current_url = ? WHERE device_id = ?',
            [url, deviceId]
        );
        
        // 记录URL变更历史
        await pool.query(
            'INSERT INTO url_history (device_id, url) VALUES (?, ?)',
            [deviceId, url]
        );
        
        return this.findByDeviceId(deviceId);
    },

    async getAllDevices() {
        const [rows] = await pool.query('SELECT * FROM devices ORDER BY last_seen DESC');
        return rows;
    }
};

module.exports = Device;