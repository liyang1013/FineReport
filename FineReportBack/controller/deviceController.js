const Device = require('@/models/deviceModel');
const { notifyDevice } = require('@/controller/websocketController');
const ApiResponse = require('@/utils/responseUtils');
const wsType = require('@/models/wsType')
const DeviceDto = require('@/models/DeviceDto');

exports.getDevice = async (req, res) => {
    try {
        const device = await Device.findByDeviceId(req.params.deviceId);
        if (!device) {
            return res.status(404).json(ApiResponse.error('Device not found'));
        }
        res.json(ApiResponse.success(device));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.deleteDevice = async (req, res) => {
    try {
        await Device.deleteByDeviceId(req.params.deviceId);
        res.json(ApiResponse.success());
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.addDevice = async (req, res) => {
    try {
        const deviceDto = new DeviceDto(req.body);
        await Device.addDevice(deviceDto);
        res.json(ApiResponse.success());
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.queryDevices = async (req, res) => {
    try {
        const deviceDto = new DeviceDto(req.body);
        const devices = await Device.queryDevices(deviceDto);
        res.json(ApiResponse.success(devices));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.sendInfo = async (req, res) => {
    try {
        const { deviceList, type } = req.body;
        deviceList.forEach(item => {
            notifyDevice(item, type);
        });
        res.json(ApiResponse.success());
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.updateDevice = async (req, res) => {
    try {
       const deviceDto = new DeviceDto(req.body);
        await Device.createOrUpdate(deviceDto)
        if (deviceDto.isUpdate) {
            notifyDevice(deviceDto.deviceId, wsType.UPDATE);
        }
        res.json(ApiResponse.success());
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

