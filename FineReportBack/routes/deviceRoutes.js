const express = require('express');
const router = express.Router();
const deviceController = require('@/controller/deviceController');


router.post('/query', deviceController.queryDevices);
router.get('/:deviceId', deviceController.getDevice);
router.get('/:deviceId/delete', deviceController.deleteDevice);
router.post('/add', deviceController.addDevice);
router.post('/update', deviceController.updateDevice);
router.post('/ws', deviceController.sendInfo);



module.exports = router;