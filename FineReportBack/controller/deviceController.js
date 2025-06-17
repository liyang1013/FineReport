const Device = require('../models/deviceModel');
const { notifyDeviceUrlUpdate } = require('./websocketController');
const ApiResponse = require('../utils/response');

exports.getDevice = async (req, res) => {
    try {
        const device = await Device.findByDeviceId(req.params.deviceId);
        if (!device) {
            return res.status(404).json(ApiResponse.error('Device not found'));
        }
        res.json(ApiResponse.success(device.url));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.setDeviceUrl = async (req, res) => {
    try {
        const { url } = req.body;
        if (!url) {
            return res.status(400).json(ApiResponse.error('URL is required'));
        }
        const device = await Device.updateUrl(req.params.deviceId, url);
        notifyDeviceUrlUpdate(req.params.deviceId);
        res.json(ApiResponse.success(device));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.queryDevices = async (req, res) => {
    try {
        console.log('asdasd')
        const devices = await Device.getAllDevices();
        const { deviceId, ipAddress, url } = req.query
        console.log(deviceId)
        console.log(ipAddress)
        console.log(url)
        res.json(ApiResponse.success(devices));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};