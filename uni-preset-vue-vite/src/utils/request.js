export const baseURL = 'http://localhost:8080/api';

const request = {
    get(url, params = {}) {
        // 构建查询字符串
        const queryString = Object.keys(params)
            .filter(key => params[key] !== null && params[key] !== undefined)
            .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
            .join('&');

        const fullUrl = queryString ? `${baseURL}${url}?${queryString}` : `${baseURL}${url}`;

        return new Promise((resolve, reject) => {
            uni.request({
                url: fullUrl,
                method: 'GET',
                header: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '')
                },
                success: (res) => {
                    if (res.statusCode === 200) {
                        const data = res.data;
                        // 支持多种响应格式
                        if (data.code === 0 || data.success === true || Array.isArray(data)) {
                            resolve(data);
                        } else if (data.code !== undefined && data.code !== 0) {
                            // 只在明确失败时显示错误
                            uni.showToast({
                                title: data.msg || data.message || '请求失败',
                                icon: 'none',
                                duration: 2000
                            });
                            reject(data);
                        } else {
                            // 其他情况直接返回数据
                            resolve(data);
                        }
                    } else if (res.statusCode === 404) {
                        uni.showToast({
                            title: '接口不存在',
                            icon: 'none',
                            duration: 2000
                        });
                        reject(res);
                    } else {
                        uni.showToast({
                            title: '网络请求失败',
                            icon: 'none',
                            duration: 2000
                        });
                        reject(res);
                    }
                },
                fail: (err) => {
                    uni.showToast({
                        title: '网络连接失败',
                        icon: 'none',
                        duration: 2000
                    });
                    reject(err);
                }
            });
        });
    },

    post(url, data = {}) {
        return new Promise((resolve, reject) => {
            uni.request({
                url: `${baseURL}${url}`,
                method: 'POST',
                data: data,
                header: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '')
                },
                success: (res) => {
                    if (res.statusCode === 200) {
                        const data = res.data;
                        // 支持多种响应格式
                        if (data.code === 0 || data.success === true) {
                            resolve(data);
                        } else if (data.code !== undefined && data.code !== 0) {
                            // 只在明确失败时显示错误
                            uni.showToast({
                                title: data.msg || data.message || '请求失败',
                                icon: 'none',
                                duration: 2000
                            });
                            reject(data);
                        } else {
                            // 其他情况直接返回数据
                            resolve(data);
                        }
                    } else if (res.statusCode === 404) {
                        uni.showToast({
                            title: '接口不存在',
                            icon: 'none',
                            duration: 2000
                        });
                        reject(res);
                    } else {
                        uni.showToast({
                            title: '网络请求失败',
                            icon: 'none',
                            duration: 2000
                        });
                        reject(res);
                    }
                },
                fail: (err) => {
                    uni.showToast({
                        title: '网络连接失败',
                        icon: 'none',
                        duration: 2000
                    });
                    reject(err);
                }
            });
        });
    }
};

export default request;
