const pool = require('@/config/db');

const Upgrade = {
    async querylatestVersion() {
        const [rows] = await pool.query(
            'SELECT * FROM appinfo order by version desc limit 1', []
        );
        return rows[0];
    },
}

module.exports = Upgrade;