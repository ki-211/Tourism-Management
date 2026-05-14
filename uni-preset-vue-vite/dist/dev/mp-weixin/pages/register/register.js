"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
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
const _sfc_main = {
  __name: "register",
  setup(__props) {
    const form = common_vendor.reactive({
      username: "",
      password: "",
      confirmPassword: "",
      nickname: ""
    });
    const loading = common_vendor.ref(false);
    const showPassword = common_vendor.ref(false);
    const showConfirmPassword = common_vendor.ref(false);
    function validateForm() {
      if (!form.username.trim()) {
        common_vendor.index.showToast({ title: "请输入用户名", icon: "none", duration: 2e3 });
        return false;
      }
      if (form.username.length < 3 || form.username.length > 16) {
        common_vendor.index.showToast({ title: "用户名长度应为3-16位", icon: "none", duration: 2e3 });
        return false;
      }
      if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
        common_vendor.index.showToast({ title: "用户名只能包含字母、数字和下划线", icon: "none", duration: 2e3 });
        return false;
      }
      if (!form.password.trim()) {
        common_vendor.index.showToast({ title: "请输入密码", icon: "none", duration: 2e3 });
        return false;
      }
      if (form.password.length < 6 || form.password.length > 20) {
        common_vendor.index.showToast({ title: "密码长度应为6-20位", icon: "none", duration: 2e3 });
        return false;
      }
      if (form.password !== form.confirmPassword) {
        common_vendor.index.showToast({ title: "两次输入的密码不一致", icon: "none", duration: 2e3 });
        return false;
      }
      if (form.nickname && form.nickname.length > 20) {
        common_vendor.index.showToast({ title: "昵称长度不能超过20位", icon: "none", duration: 2e3 });
        return false;
      }
      return true;
    }
    function doRegister() {
      if (!validateForm()) {
        return;
      }
      loading.value = true;
      const submitData = {
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username
        // 如果没填昵称，默认使用用户名
      };
      utils_request.request.post("/auth/register", submitData).then((res) => {
        common_vendor.index.showToast({
          title: res.msg || "注册成功",
          icon: "success",
          duration: 1500
        });
        setTimeout(() => common_vendor.index.navigateBack(), 1500);
      }).catch((err) => {
        console.error("注册失败:", err);
      }).finally(() => {
        loading.value = false;
      });
    }
    function toLogin() {
      common_vendor.index.navigateBack();
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => form.username = $event),
        b: common_vendor.p({
          placeholder: "请输入用户名(3-16位字母数字)",
          clearable: true,
          modelValue: form.username
        }),
        c: common_vendor.p({
          label: "用户名"
        }),
        d: common_vendor.o(($event) => showPassword.value = !showPassword.value),
        e: common_vendor.p({
          name: showPassword.value ? "eye-fill" : "eye-off"
        }),
        f: common_vendor.o(($event) => form.password = $event),
        g: common_vendor.p({
          password: !showPassword.value,
          placeholder: "请输入密码(6-20位)",
          clearable: true,
          modelValue: form.password
        }),
        h: common_vendor.p({
          label: "密码"
        }),
        i: common_vendor.o(($event) => showConfirmPassword.value = !showConfirmPassword.value),
        j: common_vendor.p({
          name: showConfirmPassword.value ? "eye-fill" : "eye-off"
        }),
        k: common_vendor.o(($event) => form.confirmPassword = $event),
        l: common_vendor.p({
          password: !showConfirmPassword.value,
          placeholder: "请再次输入密码",
          clearable: true,
          modelValue: form.confirmPassword
        }),
        m: common_vendor.p({
          label: "确认密码"
        }),
        n: common_vendor.o(($event) => form.nickname = $event),
        o: common_vendor.p({
          placeholder: "请输入昵称(选填)",
          clearable: true,
          modelValue: form.nickname
        }),
        p: common_vendor.p({
          label: "昵称"
        }),
        q: common_vendor.t(loading.value ? "注册中..." : "注册"),
        r: common_vendor.o(doRegister),
        s: common_vendor.p({
          type: "primary",
          loading: loading.value
        }),
        t: common_vendor.o(toLogin)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-97bb96ad"]]);
wx.createPage(MiniProgramPage);
