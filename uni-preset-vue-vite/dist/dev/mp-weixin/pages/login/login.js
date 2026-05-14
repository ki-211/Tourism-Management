"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const _sfc_main = {
  data() {
    return {
      form: {
        username: "",
        password: ""
      },
      loading: false,
      passwordFocused: false,
      showPassword: false
    };
  },
  methods: {
    async doLogin() {
      if (!this.validateForm()) {
        return;
      }
      this.loading = true;
      try {
        const res = await utils_request.request.post("/auth/login", this.form);
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const payload = packet ? packet.data : res && res.data !== void 0 ? res.data : res;
        if (!payload || !payload.token) {
          throw new Error("登录失败，token未返回");
        }
        const expiresIn = 86400;
        const expireTime = Date.now() + expiresIn * 1e3;
        common_vendor.index.setStorageSync("token", payload.token);
        common_vendor.index.setStorageSync("token_expire", expireTime);
        common_vendor.index.setStorageSync("userId", payload.userId);
        common_vendor.index.showToast({
          title: packet && packet.msg ? packet.msg : "登录成功",
          icon: "success",
          duration: 1500
        });
        setTimeout(() => {
          this.safeSwitchTab("/pages/home/home");
        }, 1500);
      } catch (err) {
        if (err && err.msg)
          ;
        else {
          common_vendor.index.showToast({
            title: err.message || "登录失败，请检查网络连接",
            icon: "none",
            duration: 2e3
          });
        }
      } finally {
        this.loading = false;
      }
    },
    validateForm() {
      if (!this.form.username.trim()) {
        common_vendor.index.showToast({
          title: "用户名不能为空",
          icon: "none",
          duration: 2e3
        });
        return false;
      }
      if (!this.form.password.trim()) {
        common_vendor.index.showToast({
          title: "密码不能为空",
          icon: "none",
          duration: 2e3
        });
        return false;
      }
      if (this.form.password.length < 6) {
        common_vendor.index.showToast({
          title: "密码长度不能少于6位",
          icon: "none",
          duration: 2e3
        });
        return false;
      }
      return true;
    },
    toRegister() {
      common_vendor.index.navigateTo({ url: "/pages/register/register" });
    },
    isTokenValid() {
      const token = common_vendor.index.getStorageSync("token");
      const expire = common_vendor.index.getStorageSync("token_expire");
      if (!token || !expire)
        return false;
      return Date.now() < expire;
    },
    safeSwitchTab(url) {
      if (!this.isTokenValid()) {
        common_vendor.index.showToast({
          title: "登录状态已过期，请重新登录",
          icon: "none",
          duration: 2e3
        });
        common_vendor.index.reLaunch({ url: "/pages/login/login" });
      } else {
        common_vendor.index.switchTab({ url });
      }
    }
  }
};
if (!Array) {
  const _easycom_u_input2 = common_vendor.resolveComponent("u-input");
  const _easycom_u_form_item2 = common_vendor.resolveComponent("u-form-item");
  const _easycom_u_icon2 = common_vendor.resolveComponent("u-icon");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_form2 = common_vendor.resolveComponent("u-form");
  (_easycom_u_input2 + _easycom_u_form_item2 + _easycom_u_icon2 + _easycom_u_button2 + _easycom_u_form2)();
}
const _easycom_u_input = () => "../../node-modules/uview-plus/components/u-input/u-input.js";
const _easycom_u_form_item = () => "../../node-modules/uview-plus/components/u-form-item/u-form-item.js";
const _easycom_u_icon = () => "../../node-modules/uview-plus/components/u-icon/u-icon.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_form = () => "../../node-modules/uview-plus/components/u-form/u-form.js";
if (!Math) {
  (_easycom_u_input + _easycom_u_form_item + _easycom_u_icon + _easycom_u_button + _easycom_u_form)();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.o(($event) => $data.form.username = $event),
    b: common_vendor.p({
      placeholder: "请输入用户名",
      clearable: true,
      modelValue: $data.form.username
    }),
    c: common_vendor.p({
      label: "用户名"
    }),
    d: common_vendor.o(($event) => $data.showPassword = !$data.showPassword),
    e: common_vendor.p({
      name: $data.showPassword ? "eye-fill" : "eye-off"
    }),
    f: common_vendor.o(($event) => $data.passwordFocused = true),
    g: common_vendor.o(($event) => $data.passwordFocused = false),
    h: common_vendor.o(($event) => $data.form.password = $event),
    i: common_vendor.p({
      password: !$data.showPassword,
      placeholder: "请输入密码",
      clearable: true,
      modelValue: $data.form.password
    }),
    j: common_vendor.p({
      label: "密码"
    }),
    k: $data.passwordFocused
  }, $data.passwordFocused ? {} : {}, {
    l: common_vendor.t($data.loading ? "登录中..." : "登录"),
    m: common_vendor.o($options.doLogin),
    n: common_vendor.p({
      type: "primary"
    }),
    o: common_vendor.o((...args) => $options.toRegister && $options.toRegister(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-cdfe2409"]]);
wx.createPage(MiniProgramPage);
