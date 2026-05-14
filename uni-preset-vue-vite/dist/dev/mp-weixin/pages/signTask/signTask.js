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
  __name: "signTask",
  setup(__props) {
    const formRef = common_vendor.ref(null);
    const form = common_vendor.ref({
      title: "",
      description: ""
    });
    const activityId = common_vendor.ref(null);
    common_vendor.onLoad((options) => {
      activityId.value = options.id;
    });
    function submit() {
      if (!form.value.title) {
        return common_vendor.index.showToast({ title: "请输入签到标题", icon: "none" });
      }
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId) {
        return common_vendor.index.redirectTo({ url: "/pages/login/login" });
      }
      const payload = {
        ...form.value,
        activityId: activityId.value,
        createUserId: userId
      };
      utils_request.request.post("/signTask/create", payload).then(() => {
        common_vendor.index.showToast({ title: "签到任务已发布" });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 800);
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => form.value.title = $event),
        b: common_vendor.p({
          placeholder: "请输入签到任务标题",
          modelValue: form.value.title
        }),
        c: common_vendor.p({
          label: "签到标题",
          prop: "title",
          required: true
        }),
        d: common_vendor.o(($event) => form.value.description = $event),
        e: common_vendor.p({
          type: "textarea",
          placeholder: "请输入签到说明（可选）",
          modelValue: form.value.description
        }),
        f: common_vendor.p({
          label: "签到说明",
          prop: "description"
        }),
        g: common_vendor.o(submit),
        h: common_vendor.p({
          type: "primary"
        }),
        i: common_vendor.sr(formRef, "d4b69464-0", {
          "k": "formRef"
        }),
        j: common_vendor.p({
          model: form.value
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d4b69464"]]);
wx.createPage(MiniProgramPage);
