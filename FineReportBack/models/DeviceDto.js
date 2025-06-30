class DeviceDTO {
    constructor({
        deviceId,
        ipAddress,
        url,
        remark,
        position,
        department,
        name,
        type,
        version,
        centre,
        isUpdate = true
    }) {
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.url = url;
        this.remark = remark;
        this.position = position;
        this.department = department;
        this.name = name;
        this.type = type;
        this.version = version;
        this.isUpdate = isUpdate;
        this.centre = centre;
    }

    validate() {
        if (!this.deviceId) {
            throw new Error('Device ID is required');
        }
    }
}

module.exports = DeviceDTO;