"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_input2 = common_vendor.resolveComponent("u-input");
  const _easycom_u_form_item2 = common_vendor.resolveComponent("u-form-item");
  const _easycom_u_datetime_picker2 = common_vendor.resolveComponent("u-datetime-picker");
  const _easycom_u_radio2 = common_vendor.resolveComponent("u-radio");
  const _easycom_u_radio_group2 = common_vendor.resolveComponent("u-radio-group");
  const _easycom_u_upload2 = common_vendor.resolveComponent("u-upload");
  const _easycom_u_form2 = common_vendor.resolveComponent("u-form");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  (_easycom_u_input2 + _easycom_u_form_item2 + _easycom_u_datetime_picker2 + _easycom_u_radio2 + _easycom_u_radio_group2 + _easycom_u_upload2 + _easycom_u_form2 + _easycom_u_button2)();
}
const _easycom_u_input = () => "../../node-modules/uview-plus/components/u-input/u-input.js";
const _easycom_u_form_item = () => "../../node-modules/uview-plus/components/u-form-item/u-form-item.js";
const _easycom_u_datetime_picker = () => "../../node-modules/uview-plus/components/u-datetime-picker/u-datetime-picker.js";
const _easycom_u_radio = () => "../../node-modules/uview-plus/components/u-radio/u-radio.js";
const _easycom_u_radio_group = () => "../../node-modules/uview-plus/components/u-radio-group/u-radio-group.js";
const _easycom_u_upload = () => "../../node-modules/uview-plus/components/u-upload/u-upload.js";
const _easycom_u_form = () => "../../node-modules/uview-plus/components/u-form/u-form.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
if (!Math) {
  (_easycom_u_input + _easycom_u_form_item + _easycom_u_datetime_picker + _easycom_u_radio + _easycom_u_radio_group + _easycom_u_upload + _easycom_u_form + _easycom_u_button)();
}
const _sfc_main = {
  __name: "activityCreate",
  setup(__props) {
    const form = common_vendor.reactive({
      title: "",
      location: "",
      feeRule: "",
      visibleToTeam: true,
      description: "",
      imageUrl: "",
      creatorId: ""
    });
    const fileList = common_vendor.ref([]);
    const startTimeValue = common_vendor.ref(Date.now());
    const endTimeValue = common_vendor.ref(Date.now());
    const signupStartValue = common_vendor.ref(Date.now());
    const signupEndValue = common_vendor.ref(Date.now());
    const formattedStartTime = common_vendor.ref("");
    const formattedEndTime = common_vendor.ref("");
    const formattedSignupStart = common_vendor.ref("");
    const formattedSignupEnd = common_vendor.ref("");
    const showStartPicker = common_vendor.ref(false);
    const showEndPicker = common_vendor.ref(false);
    const showSignupStartPicker = common_vendor.ref(false);
    const showSignupEndPicker = common_vendor.ref(false);
    function afterRead(event) {
      const file = event.file;
      common_vendor.index.uploadFile({
        url: "http://localhost:8080/api/upload/image",
        filePath: file.url,
        name: "file",
        success(res) {
          const data = JSON.parse(res.data);
          form.imageUrl = data.url;
          fileList.value = [{ url: data.url }];
        },
        fail() {
          common_vendor.index.showToast({ title: "上传失败", icon: "none" });
        }
      });
    }
    function formatDate(ts) {
      const d = new Date(ts);
      const pad = (n) => n < 10 ? "0" + n : n;
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }
    function onStartConfirm(e) {
      showStartPicker.value = false;
      startTimeValue.value = e.value;
      formattedStartTime.value = formatDate(e.value);
    }
    function onEndConfirm(e) {
      showEndPicker.value = false;
      endTimeValue.value = e.value;
      formattedEndTime.value = formatDate(e.value);
    }
    function onSignupStartConfirm(e) {
      showSignupStartPicker.value = false;
      signupStartValue.value = e.value;
      formattedSignupStart.value = formatDate(e.value);
    }
    function onSignupEndConfirm(e) {
      showSignupEndPicker.value = false;
      signupEndValue.value = e.value;
      formattedSignupEnd.value = formatDate(e.value);
    }
    function submit() {
      const token = common_vendor.index.getStorageSync("token");
      const uid = common_vendor.index.getStorageSync("userId");
      if (!token)
        return common_vendor.index.showToast({ title: "未登录，请先登录", icon: "none" });
      if (!uid)
        return common_vendor.index.showToast({ title: "未获取到用户信息", icon: "none" });
      form.creatorId = uid;
      if (!form.title || !formattedStartTime.value || !formattedEndTime.value) {
        return common_vendor.index.showToast({ title: "请完整填写活动名称和时间", icon: "none" });
      }
      form.startTime = formattedStartTime.value;
      form.endTime = formattedEndTime.value;
      form.signupStart = formattedSignupStart.value;
      form.signupEnd = formattedSignupEnd.value;
      utils_request.request.post("/activity/create", form).then(() => {
        common_vendor.index.showToast({ title: "发布成功", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 1e3);
      }).catch((err) => {
        common_vendor.index.showToast({ title: err.msg || "发布失败", icon: "none" });
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => form.title = $event),
        b: common_vendor.p({
          placeholder: "请输入活动名称",
          modelValue: form.title
        }),
        c: common_vendor.p({
          label: "活动名称"
        }),
        d: common_vendor.o(($event) => form.location = $event),
        e: common_vendor.p({
          placeholder: "请输入地点",
          modelValue: form.location
        }),
        f: common_vendor.p({
          label: "活动地点"
        }),
        g: common_vendor.o(($event) => formattedStartTime.value = $event),
        h: common_vendor.p({
          placeholder: "请选择开始时间",
          readonly: true,
          prefixIcon: "calendar",
          modelValue: formattedStartTime.value
        }),
        i: common_vendor.o(($event) => showStartPicker.value = true),
        j: common_vendor.p({
          label: "开始时间"
        }),
        k: common_vendor.o(onStartConfirm),
        l: common_vendor.o(($event) => showStartPicker.value = false),
        m: common_vendor.o(($event) => startTimeValue.value = $event),
        n: common_vendor.p({
          mode: "datetime",
          show: showStartPicker.value,
          closeOnClickOverlay: true,
          modelValue: startTimeValue.value
        }),
        o: common_vendor.o(($event) => formattedEndTime.value = $event),
        p: common_vendor.p({
          placeholder: "请选择结束时间",
          readonly: true,
          prefixIcon: "calendar",
          modelValue: formattedEndTime.value
        }),
        q: common_vendor.o(($event) => showEndPicker.value = true),
        r: common_vendor.p({
          label: "结束时间"
        }),
        s: common_vendor.o(onEndConfirm),
        t: common_vendor.o(($event) => showEndPicker.value = false),
        v: common_vendor.o(($event) => endTimeValue.value = $event),
        w: common_vendor.p({
          mode: "datetime",
          show: showEndPicker.value,
          closeOnClickOverlay: true,
          modelValue: endTimeValue.value
        }),
        x: common_vendor.o(($event) => formattedSignupStart.value = $event),
        y: common_vendor.p({
          placeholder: "请选择报名开始",
          readonly: true,
          prefixIcon: "calendar",
          modelValue: formattedSignupStart.value
        }),
        z: common_vendor.o(($event) => showSignupStartPicker.value = true),
        A: common_vendor.p({
          label: "报名开始"
        }),
        B: common_vendor.o(onSignupStartConfirm),
        C: common_vendor.o(($event) => showSignupStartPicker.value = false),
        D: common_vendor.o(($event) => signupStartValue.value = $event),
        E: common_vendor.p({
          mode: "datetime",
          show: showSignupStartPicker.value,
          closeOnClickOverlay: true,
          modelValue: signupStartValue.value
        }),
        F: common_vendor.o(($event) => formattedSignupEnd.value = $event),
        G: common_vendor.p({
          placeholder: "请选择报名结束",
          readonly: true,
          prefixIcon: "calendar",
          modelValue: formattedSignupEnd.value
        }),
        H: common_vendor.o(($event) => showSignupEndPicker.value = true),
        I: common_vendor.p({
          label: "报名结束"
        }),
        J: common_vendor.o(onSignupEndConfirm),
        K: common_vendor.o(($event) => showSignupEndPicker.value = false),
        L: common_vendor.o(($event) => signupEndValue.value = $event),
        M: common_vendor.p({
          mode: "datetime",
          show: showSignupEndPicker.value,
          closeOnClickOverlay: true,
          modelValue: signupEndValue.value
        }),
        N: common_vendor.o(($event) => form.feeRule = $event),
        O: common_vendor.p({
          type: "textarea",
          placeholder: "请输入费用说明",
          modelValue: form.feeRule
        }),
        P: common_vendor.p({
          label: "费用规则"
        }),
        Q: common_vendor.p({
          name: true
        }),
        R: common_vendor.p({
          name: false
        }),
        S: common_vendor.o(($event) => form.visibleToTeam = $event),
        T: common_vendor.p({
          modelValue: form.visibleToTeam
        }),
        U: common_vendor.p({
          label: "可见性"
        }),
        V: common_vendor.o(afterRead),
        W: common_vendor.p({
          ["file-list"]: fileList.value,
          ["max-count"]: 1
        }),
        X: common_vendor.p({
          label: "上传图片"
        }),
        Y: common_vendor.o(($event) => form.description = $event),
        Z: common_vendor.p({
          type: "textarea",
          placeholder: "请输入说明",
          modelValue: form.description
        }),
        aa: common_vendor.p({
          label: "活动说明"
        }),
        ab: common_vendor.p({
          ["label-width"]: "80"
        }),
        ac: common_vendor.o(submit),
        ad: common_vendor.p({
          type: "primary"
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2468f62a"]]);
wx.createPage(MiniProgramPage);
