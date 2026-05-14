"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_input2 = common_vendor.resolveComponent("u-input");
  const _easycom_u_form_item2 = common_vendor.resolveComponent("u-form-item");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_form2 = common_vendor.resolveComponent("u-form");
  const _easycom_u_tag2 = common_vendor.resolveComponent("u-tag");
  (_easycom_u_input2 + _easycom_u_form_item2 + _easycom_u_button2 + _easycom_u_form2 + _easycom_u_tag2)();
}
const _easycom_u_input = () => "../../node-modules/uview-plus/components/u-input/u-input.js";
const _easycom_u_form_item = () => "../../node-modules/uview-plus/components/u-form-item/u-form-item.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_form = () => "../../node-modules/uview-plus/components/u-form/u-form.js";
const _easycom_u_tag = () => "../../node-modules/uview-plus/components/u-tag/u-tag.js";
if (!Math) {
  (_easycom_u_input + _easycom_u_form_item + _easycom_u_button + _easycom_u_form + _easycom_u_tag)();
}
const _sfc_main = {
  __name: "signupList",
  setup(__props) {
    const list = common_vendor.ref([]);
    const form = common_vendor.ref({
      grade: "",
      passengerCount: "",
      remark: ""
    });
    const hasSignedUp = common_vendor.ref(false);
    const isCreator = common_vendor.ref(false);
    const activityId = common_vendor.ref(null);
    const signStatus = common_vendor.ref({});
    const taskList = common_vendor.ref([]);
    const taskSignedMap = common_vendor.ref({});
    common_vendor.onLoad((options) => {
      activityId.value = options.id;
      fetchList();
    });
    function fetchList() {
      utils_request.request.get("/signup/list/" + activityId.value).then((res) => {
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        list.value = arr;
        const uid = common_vendor.index.getStorageSync("userId");
        hasSignedUp.value = Array.isArray(arr) ? arr.some((i) => i.userId === uid) : false;
        utils_request.request.get("/activity/" + activityId.value).then((activity) => {
          const apacket = activity && activity.code !== void 0 ? activity : activity && activity.data && activity.data.code !== void 0 ? activity.data : null;
          const adata = apacket ? apacket.data : activity && activity.data !== void 0 ? activity.data : activity;
          isCreator.value = adata && Number(adata.creatorId) === Number(uid);
          if (isCreator.value) {
            loadSignStatus();
            loadTasksAndStatus();
          }
        });
      });
    }
    async function loadSignStatus() {
      try {
        const res = await utils_request.request.get("/signRecord/listByActivity", { activityId: activityId.value });
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        const map = {};
        (arr || []).forEach((i) => {
          map[i.userId] = { signedCount: i.signedCount || 0, totalTasks: i.totalTasks || 0 };
        });
        signStatus.value = map;
      } catch (e) {
        signStatus.value = {};
      }
    }
    async function loadTasksAndStatus() {
      try {
        const [tasksRes, statusRes] = await Promise.all([
          utils_request.request.get("/signTask/listByActivity", { activityId: activityId.value }),
          utils_request.request.get("/signRecord/statusByActivity", { activityId: activityId.value })
        ]);
        const tPacket = tasksRes && tasksRes.code !== void 0 ? tasksRes : tasksRes && tasksRes.data && tasksRes.data.code !== void 0 ? tasksRes.data : null;
        const sPacket = statusRes && statusRes.code !== void 0 ? statusRes : statusRes && statusRes.data && statusRes.data.code !== void 0 ? statusRes.data : null;
        taskList.value = tPacket ? tPacket.data || [] : Array.isArray(tasksRes) ? tasksRes : tasksRes && tasksRes.data ? tasksRes.data : [];
        const statusArr = sPacket ? sPacket.data || [] : Array.isArray(statusRes) ? statusRes : statusRes && statusRes.data ? statusRes.data : [];
        const map = {};
        (statusArr || []).forEach((i) => {
          map[`${i.userId}-${i.taskId}`] = true;
        });
        taskSignedMap.value = map;
      } catch (e) {
        taskList.value = [];
        taskSignedMap.value = {};
      }
    }
    function doSignup() {
      const uid = common_vendor.index.getStorageSync("userId");
      const payload = {
        ...form.value,
        userId: uid,
        activityId: activityId.value
      };
      utils_request.request.post(`/signup/${activityId.value}`, payload).then(() => {
        common_vendor.index.showToast({ title: "报名成功" });
        fetchList();
      });
    }
    function goVehicleAdd() {
      common_vendor.index.navigateTo({
        url: `/pages/vehicleAdd/vehicleAdd?id=${activityId.value}`
      });
    }
    function goVehicleList() {
      common_vendor.index.navigateTo({
        url: `/pages/vehicleList/vehicleList?id=${activityId.value}`
      });
    }
    function goSignTask() {
      common_vendor.index.navigateTo({
        url: `/pages/signTask/signTask?id=${activityId.value}`
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: !hasSignedUp.value
      }, !hasSignedUp.value ? {
        b: common_vendor.o(($event) => form.value.grade = $event),
        c: common_vendor.p({
          placeholder: "请输入年级",
          modelValue: form.value.grade
        }),
        d: common_vendor.p({
          label: "年级"
        }),
        e: common_vendor.o(($event) => form.value.passengerCount = $event),
        f: common_vendor.p({
          type: "number",
          placeholder: "请输入乘车人数",
          modelValue: form.value.passengerCount
        }),
        g: common_vendor.p({
          label: "乘车人数"
        }),
        h: common_vendor.o(($event) => form.value.remark = $event),
        i: common_vendor.p({
          type: "textarea",
          placeholder: "请输入备注",
          modelValue: form.value.remark
        }),
        j: common_vendor.p({
          label: "备注"
        }),
        k: common_vendor.o(doSignup),
        l: common_vendor.p({
          type: "primary"
        }),
        m: common_vendor.p({
          model: form.value
        })
      } : {
        n: common_vendor.p({
          text: "已报名",
          type: "success"
        }),
        o: common_vendor.o(goVehicleList),
        p: common_vendor.p({
          type: "primary",
          size: "mini",
          plain: true
        })
      }, {
        q: isCreator.value
      }, isCreator.value ? {
        r: common_vendor.o(goVehicleAdd),
        s: common_vendor.p({
          type: "primary",
          size: "mini",
          plain: true
        })
      } : {}, {
        t: common_vendor.o(goSignTask),
        v: common_vendor.p({
          type: "warning",
          size: "mini",
          plain: true
        }),
        w: isCreator.value
      }, isCreator.value ? {
        x: common_vendor.f(list.value, (item, k0, i0) => {
          var _a, _b, _c;
          return {
            a: common_vendor.t(item.userId),
            b: common_vendor.t(item.grade || "未填写"),
            c: common_vendor.t(item.passengerCount || "未填写"),
            d: common_vendor.t(item.remark || "无"),
            e: common_vendor.t((((_a = signStatus.value[item.userId]) == null ? void 0 : _a.totalTasks) || 0) === 0 ? "无签到任务" : (((_b = signStatus.value[item.userId]) == null ? void 0 : _b.signedCount) || 0) + "/" + (((_c = signStatus.value[item.userId]) == null ? void 0 : _c.totalTasks) || 0)),
            f: common_vendor.f(taskList.value, (t, k1, i1) => {
              return {
                a: common_vendor.t(t.title),
                b: common_vendor.t(taskSignedMap.value[item.userId + "-" + t.id] ? "已签" : "未签"),
                c: t.id
              };
            }),
            g: item.id
          };
        })
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-664cc11a"]]);
wx.createPage(MiniProgramPage);
