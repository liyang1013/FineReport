const pool = require('@/config/db');

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
    async createOrUpdate(deviceDto) {
        const device = await this.findByDeviceId(deviceDto.deviceId);
        if (device) {
            const updateFields = [];
            const updateValues = [];

            updateFields.push('ipAddress = ?');
            updateValues.push(deviceDto.ipAddress);

            updateFields.push('version = ?');
            updateValues.push(deviceDto.version);


            if (deviceDto.url != null) {
                updateFields.push('url = ?');
                updateValues.push(deviceDto.url);
            }

            if (deviceDto.remark != null) {
                updateFields.push('remark = ?');
                updateValues.push(deviceDto.remark);
            }

            if (deviceDto.position != null) {
                updateFields.push('position = ?');
                updateValues.push(deviceDto.position);
            }
            if (deviceDto.department != null) {
                updateFields.push('department = ?');
                updateValues.push(deviceDto.department);
            }
            if (deviceDto.name != null) {
                updateFields.push('name = ?');
                updateValues.push(deviceDto.name);
            }
            if (deviceDto.type != null) {
                updateFields.push('type = ?');
                updateValues.push(deviceDto.type);
            }
            if (deviceDto.centre != null) {
                updateFields.push('centre = ?');
                updateValues.push(deviceDto.centre);
            }

            updateFields.push('lastSeen = NOW()');

            const updateQuery = `UPDATE deviceinfo SET ${updateFields.join(', ')} WHERE deviceId = ?`;
            updateValues.push(deviceDto.deviceId);
            await pool.query(updateQuery, updateValues);
        } else {
            await this.addDevice(deviceDto);
        }
    },

    async addDevice(deviceDTO) {
        await pool.query(
            'INSERT INTO deviceinfo (deviceId, ipAddress, url,remark,position,department,name,type,version,centre ,lastSeen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())',
            [deviceDTO.deviceId, deviceDTO.ipAddress, deviceDTO.url, deviceDTO.remark, deviceDTO.position, deviceDTO.department, deviceDTO.name, deviceDTO.type,deviceDTO.version, deviceDTO.centre]
        );
    },
    async queryDevices(deviceDto) {
        let query = 'SELECT * FROM deviceinfo WHERE 1=1';
        const params = [];

        if (deviceDto.position) {
            query += ' AND LOWER(position) LIKE LOWER(?)';
            params.push(`%${deviceDto.position}%`);
        }

        if (deviceDto.department) {
            query += ' AND LOWER(department) LIKE LOWER(?)';
            params.push(`%${deviceDto.department}%`);
        }

        if (deviceDto.name) {
            query += ' AND LOWER(name) LIKE LOWER(?)';
            params.push(`%${deviceDto.name}%`);
        }
        if (deviceDto.type) {
            query += ' AND LOWER(type) LIKE LOWER(?)';
            params.push(`%${deviceDto.type}%`);
        }

        if (deviceDto.deviceId) {
            query += ' AND LOWER(deviceId) LIKE LOWER(?)';
            params.push(`%${deviceDto.deviceId}%`);
        }

        if (deviceDto.ipAddress) {
            query += ' AND LOWER(ipAddress) LIKE LOWER(?)';
            params.push(`%${deviceDto.ipAddress}%`);
        }

        if (deviceDto.url) {
            query += ' AND LOWER(url) LIKE LOWER(?)';
            params.push(`%${deviceDto.url}%`);
        }

        if (deviceDto.remark) {
            query += ' AND LOWER(remark) LIKE LOWER(?)';
            params.push(`%${deviceDto.remark}%`);
        }

        if (deviceDto.centre) {
            query += ' AND LOWER(centre) LIKE LOWER(?)';
            params.push(`%${deviceDto.centre}%`);
        }

        query += ' ORDER BY deviceId, lastSeen DESC';

        const [rows] = await pool.query(query, params);
        return rows;
    }
};

module.exports = Device;