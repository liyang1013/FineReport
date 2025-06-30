const express = require('express');
const router = express.Router();
const appController = require('@/controller/appController');

router.post('/checkUpgrade',  appController.checkUpgrade)
router.post('/getAppList',  appController.getAppList)

module.exports = router;