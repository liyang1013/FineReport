const express = require('express');
const router = express.Router();
const deviceController = require('../controller/deviceController');

router.get('/:deviceId', deviceController.getDevice);
router.post('/:deviceId/url', deviceController.setDeviceUrl);
router.get('/', deviceController.getAllDevices);

module.exports = router;