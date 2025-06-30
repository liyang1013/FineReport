
const App = require('@/models/appModel');
const ApiResponse = require('@/utils/responseUtils');

exports.checkUpgrade = async(req, res) => {
    try {
        const appinfo =  await App.querylatestVersion();
        res.json(ApiResponse.success(appinfo));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};

exports.getAppList = async(req, res) => {
    try {
        const appList =  await App.getAppList();
        res.json(ApiResponse.success(appList));
    } catch (error) {
        res.status(500).json(ApiResponse.error(error.message));
    }
};



