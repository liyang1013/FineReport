const pool = require('@/config/db');
const { addDevice } = require('../controller/deviceController');

const Device = {
    async findByDeviceId(deviceId) {
        const [rows] = await pool.query(
            'SELECT * FROM deviceinfo WHERE deviceId = ?',
            [deviceId]
        );
        return rows[0];
    },
    async deleteByDeviceId(deviceId) {
        await pool.query(
            'delete  FROM deviceinfo WHERE deviceId = ?',
            [deviceId]
        );
    },
    async createOrUpdate(deviceId, ipAddress, url = null, remark = null) {
        const device = await this.findByDeviceId(deviceId);
        if (device) {
            const updateFields = [];
            const updateValues = [];

            updateFields.push('ipAddress = ?');
            updateValues.push(ipAddress);

            if (url !== null) {
                updateFields.push('url = ?');
                updateValues.push(url);
            }

            if (remark !== null) {
                updateFields.push('remark = ?');
                updateValues.push(remark);
            }

            updateFields.push('lastSeen = NOW()');

            const updateQuery = `UPDATE deviceinfo SET ${updateFields.join(', ')} WHERE deviceId = ?`;
            updateValues.push(deviceId);

            await pool.query(updateQuery, updateValues);
        } else {
            await this.addDevice(deviceId, ipAddress, url, remark);
        }
    },

    async addDevice(deviceId, ipAddress, url, remark) {
        await pool.query(
            'INSERT INTO deviceinfo (deviceId, ipAddress, url,remark, lastSeen) VALUES (?, ?, ?,?, NOW())',
            [deviceId, ipAddress, url, remark]
        );
    },
    async queryDevices(deviceId, ipAddress, url, remark) {
        let query = 'SELECT * FROM deviceinfo WHERE 1=1';
        const params = [];

        if (deviceId) {
            query += ' AND LOWER(deviceId) LIKE LOWER(?)';
            params.push(`%${deviceId}%`);
        }

        if (ipAddress) {
            query += ' AND LOWER(ipAddress) LIKE LOWER(?)';
            params.push(`%${ipAddress}%`);
        }

        if (url) {
            query += ' AND LOWER(url) LIKE LOWER(?)';
            params.push(`%${url}%`);
        }

        if (remark) {
            query += ' AND LOWER(remark) LIKE LOWER(?)';
            params.push(`%${remark}%`);
        }

        query += ' ORDER BY lastSeen DESC';

        const [rows] = await pool.query(query, params);
        return rows;
    }
};

module.exports = Device;