"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_input2 = common_vendor.resolveComponent("u-input");
  const _easycom_u_form_item2 = common_vendor.resolveComponent("u-form-item");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_form2 = common_vendor.resolveComponent("u-form");
  (_easycom_u_input2 + _easycom_u_form_item2 + _easycom_u_button2 + _easycom_u_form2)();
}
const _easycom_u_input = () => "../../node-modules/uview-plus/components/u-input/u-input.js";
const _easycom_u_form_item = () => "../../node-modules/uview-plus/components/u-form-item/u-form-item.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_form = () => "../../node-modules/uview-plus/components/u-form/u-form.js";
if (!Math) {
  (_easycom_u_input + _easycom_u_form_item + _easycom_u_button + _easycom_u_form)();
}
const _sfc_main = {
  __name: "editInfo",
  setup(__props) {
    const form = common_vendor.ref({ nickname: "" });
    const formRef = common_vendor.ref();
    common_vendor.onMounted(() => {
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      utils_request.request.get(`/auth/info?userId=${userId}`).then((res) => {
        form.value.nickname = res.data.nickname || "";
      });
    });
    function submit() {
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId)
        return;
      utils_request.request.post("/auth/update", {
        userId,
        nickname: form.value.nickname
      }).then(() => {
        common_vendor.index.showToast({ title: "修改成功", icon: "success" });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 800);
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => form.value.nickname = $event),
        b: common_vendor.p({
          placeholder: "请输入新昵称",
          modelValue: form.value.nickname
        }),
        c: common_vendor.p({
          label: "昵称"
        }),
        d: common_vendor.o(submit),
        e: common_vendor.p({
          type: "primary"
        }),
        f: common_vendor.sr(formRef, "0de44be6-0", {
          "k": "formRef"
        }),
        g: common_vendor.p({
          model: form.value
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0de44be6"]]);
wx.createPage(MiniProgramPage);
