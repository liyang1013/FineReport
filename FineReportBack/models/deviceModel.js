const pool = require('../config/db');

const Device = {
    async findByDeviceId(deviceId) {
        const [rows] = await pool.query(
            'SELECT * FROM deviceinfo WHERE deviceId = ?', 
            [deviceId]
        );
        return rows[0];
    },

    async createOrUpdate(deviceId, ipAddress, url = null) {
        const device = await this.findByDeviceId(deviceId);
        if (device) {
            await pool.query(
                'UPDATE deviceinfo SET ipAddress = ?, lastSeen = NOW()' + 
                (url ? ', url = ?' : '') + 
                ' WHERE deviceId = ?',
                url ? [ipAddress, url, deviceId] : [ipAddress, deviceId]
            );
        } else {
            await pool.query(
                'INSERT INTO deviceinfo (deviceId, ipAddress, url) VALUES (?, ?, ?)',
                [deviceId, ipAddress, url || '']
            );
        }
        return this.findByDeviceId(deviceId);
    },

    async updateUrl(deviceId, url) {
        await pool.query(
            'UPDATE deviceinfo SET url = ? WHERE deviceId = ?',
            [url, deviceId]
        );
        return this.findByDeviceId(deviceId);
    },

    async getAllDevices() {
        const [rows] = await pool.query('SELECT * FROM deviceinfo ORDER BY lastSeen DESC');
        return rows;
    }
};

module.exports = Device;