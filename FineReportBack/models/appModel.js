const pool = require('@/config/db');

const App = {
    async querylatestVersion() {
        const [rows] = await pool.query(
            'SELECT * FROM appinfo order by version desc limit 1', []
        );
        return rows[0];
    },
    async getAppList() {
        const [rows] = await pool.query(
            'SELECT * FROM appinfo order by version', []
        );
        return rows;
    },
}

module.exports = App;