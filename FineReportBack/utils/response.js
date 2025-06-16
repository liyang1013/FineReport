class ApiResponse {
    constructor(success, data, message = '', code = 200) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
        this.timestamp = new Date().toISOString();
    }

    static success(data, message = '操作成功') {
        return new ApiResponse(true, data, message, 200);
    }

    static error(message = '操作失败', code = 500, data = null) {
        return new ApiResponse(false, data, message, code);
    }

    static validationError(errors, message = '参数验证失败') {
        return new ApiResponse(false, { errors }, message, 422);
    }

    static paginate(data, pagination, message = '') {
        return new ApiResponse(true, {
            items: data,
            pagination: {
                total: pagination.total,
                page: pagination.page,
                pageSize: pagination.pageSize,
                totalPages: Math.ceil(pagination.total / pagination.pageSize)
            }
        }, message, 200);
    }

    toJSON() {
        return {
            success: this.success,
            code: this.code,
            message: this.message,
            data: this.data,
            timestamp: this.timestamp
        };
    }
}

module.exports = ApiResponse;

// const result = await Model.findAndCountAll({
//     limit: pageSize,
//     offset: (page - 1) * pageSize
// });

// return ApiResponse.paginate(result.rows, {
//     total: result.count,
//     page,
//     pageSize
// });