const pool = require('@/config/db');

const Device = {
    async findByDeviceId(deviceId) {
        const [rows] = await pool.query(
            'SELECT * FROM deviceinfo WHERE deviceId = ?',
            [deviceId]
        );
        return rows[0];
    },

    async createOrUpdate(deviceId, ipAddress, url = null, remark = null) {
        const device = await this.findByDeviceId(deviceId);
        if (device) {
            await pool.query(
                'UPDATE deviceinfo SET ipAddress = ?, lastSeen = NOW() , url = ? , remark = ? WHERE deviceId = ?',
                [ipAddress, url || '', remark || '', deviceId]
            );
        } else {
            await pool.query(
                'INSERT INTO deviceinfo (deviceId, ipAddress, url,remark, lastSeen) VALUES (?, ?, ?,?, NOW())',
                [deviceId, ipAddress, url || '', remark || '']
            );
        }
        return this.findByDeviceId(deviceId);
    },

    async updateUrl(deviceId, url, remark = null) {
        await pool.query(
            'UPDATE deviceinfo SET url = ?, remark = ? WHERE deviceId = ?',
            [url, remark || '', deviceId]
        );
        return this.findByDeviceId(deviceId);
    },

    async getAllDevices() {
        const [rows] = await pool.query('SELECT * FROM deviceinfo ORDER BY lastSeen DESC');
        return rows;
    }
};

module.exports = Device;