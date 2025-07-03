const pool = require("@/config/db");

const Device = {
  async findByDeviceId(deviceId) {
    const [rows] = await pool.query(
      "SELECT * FROM deviceinfo WHERE deviceId = ?",
      [deviceId]
    );
    return rows[0];
  },
  async deleteByDeviceId(deviceId) {
    await pool.query("delete  FROM deviceinfo WHERE deviceId = ?", [deviceId]);
  },
  async createOrUpdate(deviceDto) {
    const device = await this.findByDeviceId(deviceDto.deviceId);
    if (device) {
      const updateFields = [];
      const updateValues = [];

      const buildConditions = (field, value) => {
        if (value != null) {
          updateFields.push(`${field} = ?`);
          updateValues.push(value);
        }
      };

      buildConditions("position", deviceDto.position);
      buildConditions("department", deviceDto.department);
      buildConditions("name", deviceDto.name);
      buildConditions("type", deviceDto.type);
      buildConditions("ipAddress", deviceDto.ipAddress);
      buildConditions("url", deviceDto.url);
      buildConditions("remark", deviceDto.remark);
      buildConditions("centre", deviceDto.centre);
      buildConditions("version", deviceDto.version);

      updateFields.push("lastSeen = NOW()");

      const updateQuery = `UPDATE deviceinfo SET ${updateFields.join(
        ", "
      )} WHERE deviceId = ?`;
      updateValues.push(deviceDto.deviceId);
      await pool.query(updateQuery, updateValues);
    } else {
      await this.addDevice(deviceDto);
    }
  },
  async addDevice(deviceDTO) {
    await pool.query(
      "INSERT INTO deviceinfo (deviceId, ipAddress, url,remark,position,department,name,type,version,centre ,lastSeen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
      [
        deviceDTO.deviceId,
        deviceDTO.ipAddress,
        deviceDTO.url,
        deviceDTO.remark,
        deviceDTO.position,
        deviceDTO.department,
        deviceDTO.name,
        deviceDTO.type,
        deviceDTO.version,
        deviceDTO.centre,
      ]
    );
  },

  async queryDevices(deviceDto, page = 1, pageSize = 20) {
    let query = "SELECT * FROM deviceinfo WHERE 1=1";
    let countQuery = "SELECT COUNT(*) as total FROM deviceinfo WHERE 1=1";
    const params = [];
    const countParams = [];

    const buildConditions = (field, value) => {
      if (value) {
        query += ` AND LOWER(${field}) LIKE LOWER(?)`;
        countQuery += ` AND LOWER(${field}) LIKE LOWER(?)`;
        params.push(`%${value}%`);
        countParams.push(`%${value}%`);
      }
    };

    buildConditions("position", deviceDto.position);
    buildConditions("department", deviceDto.department);
    buildConditions("name", deviceDto.name);
    buildConditions("type", deviceDto.type);
    buildConditions("deviceId", deviceDto.deviceId);
    buildConditions("ipAddress", deviceDto.ipAddress);
    buildConditions("url", deviceDto.url);
    buildConditions("remark", deviceDto.remark);
    buildConditions("centre", deviceDto.centre);

    query += " ORDER BY deviceId, lastSeen DESC";

    const offset = (page - 1) * pageSize;
    query += " LIMIT ? OFFSET ?";
    params.push(pageSize, offset);

    const [rows, [countResult]] = await Promise.all([
      pool.query(query, params),
      pool.query(countQuery, countParams),
    ]);

    return {
      devices: rows[0],
      total: countResult[0].total,
    };
  },
};

module.exports = Device;
