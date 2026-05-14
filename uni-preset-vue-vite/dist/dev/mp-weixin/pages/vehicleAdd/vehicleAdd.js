"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_input2 = common_vendor.resolveComponent("u-input");
  const _easycom_u_form_item2 = common_vendor.resolveComponent("u-form-item");
  const _easycom_u_datetime_picker2 = common_vendor.resolveComponent("u-datetime-picker");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_form2 = common_vendor.resolveComponent("u-form");
  (_easycom_u_input2 + _easycom_u_form_item2 + _easycom_u_datetime_picker2 + _easycom_u_button2 + _easycom_u_form2)();
}
const _easycom_u_input = () => "../../node-modules/uview-plus/components/u-input/u-input.js";
const _easycom_u_form_item = () => "../../node-modules/uview-plus/components/u-form-item/u-form-item.js";
const _easycom_u_datetime_picker = () => "../../node-modules/uview-plus/components/u-datetime-picker/u-datetime-picker.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_form = () => "../../node-modules/uview-plus/components/u-form/u-form.js";
if (!Math) {
  (_easycom_u_input + _easycom_u_form_item + _easycom_u_datetime_picker + _easycom_u_button + _easycom_u_form)();
}
const _sfc_main = {
  __name: "vehicleAdd",
  setup(__props) {
    const form = common_vendor.ref({
      plateNumber: "",
      driverName: "",
      pickupTime: "",
      pickupLocation: "",
      activityId: null,
      creatorId: null
    });
    const pickupTimeValue = common_vendor.ref(Date.now());
    const formattedPickupTime = common_vendor.ref("");
    const showPickupTimePicker = common_vendor.ref(false);
    function formatDate(ts) {
      const d = new Date(ts);
      const pad = (n) => n < 10 ? "0" + n : n;
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }
    function onPickupTimeConfirm(e) {
      showPickupTimePicker.value = false;
      pickupTimeValue.value = e.value;
      form.value.pickupTime = formatDate(e.value);
      const d = new Date(e.value);
      formattedPickupTime.value = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours()}时${d.getMinutes()}分`;
    }
    common_vendor.onMounted(() => {
      var _a, _b;
      const params = ((_b = (_a = getCurrentPages()) == null ? void 0 : _a.pop()) == null ? void 0 : _b.options) || {};
      if (params.id || params.activityId) {
        form.value.activityId = Number(params.id || params.activityId);
      }
      const uid = common_vendor.index.getStorageSync("userId");
      if (uid) {
        form.value.creatorId = uid;
      }
    });
    function submit() {
      if (!form.value.plateNumber || !form.value.driverName || !form.value.pickupLocation || !form.value.pickupTime || !form.value.activityId || !form.value.creatorId) {
        return common_vendor.index.showToast({ title: "请填写完整信息", icon: "none" });
      }
      utils_request.request.post("/vehicle/add", form.value).then(() => {
        common_vendor.index.showToast({ title: "发布成功", icon: "success" });
        common_vendor.index.navigateBack();
      }).catch(() => {
        common_vendor.index.showToast({ title: "发布失败，请重试", icon: "none" });
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => form.value.plateNumber = $event),
        b: common_vendor.p({
          placeholder: "请输入车牌号",
          modelValue: form.value.plateNumber
        }),
        c: common_vendor.p({
          label: "车牌号"
        }),
        d: common_vendor.o(($event) => form.value.driverName = $event),
        e: common_vendor.p({
          placeholder: "请输入司机姓名",
          modelValue: form.value.driverName
        }),
        f: common_vendor.p({
          label: "司机姓名"
        }),
        g: common_vendor.o(($event) => formattedPickupTime.value = $event),
        h: common_vendor.p({
          placeholder: "请选择上车时间",
          readonly: true,
          prefixIcon: "calendar",
          modelValue: formattedPickupTime.value
        }),
        i: common_vendor.o(($event) => showPickupTimePicker.value = true),
        j: common_vendor.p({
          label: "上车时间"
        }),
        k: common_vendor.o(onPickupTimeConfirm),
        l: common_vendor.o(($event) => showPickupTimePicker.value = false),
        m: common_vendor.o(($event) => pickupTimeValue.value = $event),
        n: common_vendor.p({
          mode: "datetime",
          show: showPickupTimePicker.value,
          closeOnClickOverlay: true,
          modelValue: pickupTimeValue.value
        }),
        o: common_vendor.o(($event) => form.value.pickupLocation = $event),
        p: common_vendor.p({
          placeholder: "请输入上车地点",
          modelValue: form.value.pickupLocation
        }),
        q: common_vendor.p({
          label: "上车地点"
        }),
        r: common_vendor.o(submit),
        s: common_vendor.p({
          type: "primary"
        }),
        t: common_vendor.p({
          labelPosition: "top"
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-5ab3c139"]]);
wx.createPage(MiniProgramPage);
