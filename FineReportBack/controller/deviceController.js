const Device = require('../models/deviceModel');
const { broadcastUrlUpdate } = require('../controller/websocketController');

exports.getDevice = async (req, res) => {
    try {
        const device = await Device.findByDeviceId(req.params.deviceId);
        if (!device) {
            return res.status(404).json({ error: 'Device not found' });
        }
        res.json(device);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

exports.setDeviceUrl = async (req, res) => {
    try {
        const { url } = req.body;
        if (!url) {
            return res.status(400).json({ error: 'URL is required' });
        }

        const device = await Device.updateUrl(req.params.deviceId, url);

        notifyDeviceUrlUpdate(req.params.deviceId);

        res.json(device);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

exports.getAllDevices = async (req, res) => {
    try {
        const devices = await Device.getAllDevices();
        res.json(devices);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};