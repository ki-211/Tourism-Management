"use strict";
const common_vendor = require("../common/vendor.js");
const baseURL = "http://localhost:8080/api";
const request = {
  get(url, params = {}) {
    const queryString = Object.keys(params).filter((key) => params[key] !== null && params[key] !== void 0).map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`).join("&");
    const fullUrl = queryString ? `${baseURL}${url}?${queryString}` : `${baseURL}${url}`;
    return new Promise((resolve, reject) => {
      common_vendor.index.request({
        url: fullUrl,
        method: "GET",
        header: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + (common_vendor.index.getStorageSync("token") || "")
        },
        success: (res) => {
          if (res.statusCode === 200) {
            const data = res.data;
            if (data.code === 0 || data.success === true || Array.isArray(data)) {
              resolve(data);
            } else if (data.code !== void 0 && data.code !== 0) {
              common_vendor.index.showToast({
                title: data.msg || data.message || "请求失败",
                icon: "none",
                duration: 2e3
              });
              reject(data);
            } else {
              resolve(data);
            }
          } else if (res.statusCode === 404) {
            common_vendor.index.showToast({
              title: "接口不存在",
              icon: "none",
              duration: 2e3
            });
            reject(res);
          } else {
            common_vendor.index.showToast({
              title: "网络请求失败",
              icon: "none",
              duration: 2e3
            });
            reject(res);
          }
        },
        fail: (err) => {
          common_vendor.index.showToast({
            title: "网络连接失败",
            icon: "none",
            duration: 2e3
          });
          reject(err);
        }
      });
    });
  },
  post(url, data = {}) {
    return new Promise((resolve, reject) => {
      common_vendor.index.request({
        url: `${baseURL}${url}`,
        method: "POST",
        data,
        header: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + (common_vendor.index.getStorageSync("token") || "")
        },
        success: (res) => {
          if (res.statusCode === 200) {
            const data2 = res.data;
            if (data2.code === 0 || data2.success === true) {
              resolve(data2);
            } else if (data2.code !== void 0 && data2.code !== 0) {
              common_vendor.index.showToast({
                title: data2.msg || data2.message || "请求失败",
                icon: "none",
                duration: 2e3
              });
              reject(data2);
            } else {
              resolve(data2);
            }
          } else if (res.statusCode === 404) {
            common_vendor.index.showToast({
              title: "接口不存在",
              icon: "none",
              duration: 2e3
            });
            reject(res);
          } else {
            common_vendor.index.showToast({
              title: "网络请求失败",
              icon: "none",
              duration: 2e3
            });
            reject(res);
          }
        },
        fail: (err) => {
          common_vendor.index.showToast({
            title: "网络连接失败",
            icon: "none",
            duration: 2e3
          });
          reject(err);
        }
      });
    });
  }
};
exports.request = request;
