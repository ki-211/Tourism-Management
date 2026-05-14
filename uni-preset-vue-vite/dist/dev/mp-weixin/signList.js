"use strict";
const common_vendor = require("./common/vendor.js");
const utils_request = require("./utils/request.js");
if (!Array) {
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  _easycom_u_button2();
}
const _easycom_u_button = () => "./node-modules/uview-plus/components/u-button/u-button.js";
if (!Math) {
  _easycom_u_button();
}
const _sfc_main = {
  __name: "signList",
  setup(__props) {
    const taskList = common_vendor.ref([]);
    const loadTasks = async () => {
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId) {
        common_vendor.index.redirectTo({ url: "/pages/login/login" });
        return;
      }
      try {
        const res = await utils_request.request.get("/signTask/unsigned", { userId });
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        console.log("签到列表页后端返回数据:", arr);
        taskList.value = arr;
      } catch (e) {
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    };
    common_vendor.onShow(() => {
      loadTasks();
    });
    common_vendor.onMounted(loadTasks);
    const doSign = async (taskId) => {
      const userId = common_vendor.index.getStorageSync("userId");
      try {
        await utils_request.request.post("/signRecord/do", { taskId, userId });
        common_vendor.index.showToast({ title: "签到成功" });
        loadTasks();
      } catch (e) {
        common_vendor.index.showToast({ title: "签到失败", icon: "none" });
      }
    };
    const goHistory = () => {
      common_vendor.index.navigateTo({ url: "/pages/signDetail/signDetail" });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(goHistory),
        b: common_vendor.p({
          type: "primary",
          size: "mini",
          plain: true
        }),
        c: taskList.value.length === 0
      }, taskList.value.length === 0 ? {} : {}, {
        d: common_vendor.f(taskList.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.signTime),
            c: common_vendor.t(item.signed ? "已签到" : "签到"),
            d: common_vendor.o(($event) => doSign(item.id), item.id),
            e: "8344c52d-1-" + i0,
            f: common_vendor.p({
              type: "success",
              size: "mini",
              disabled: item.signed
            }),
            g: item.id
          };
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8344c52d"]]);
exports.MiniProgramPage = MiniProgramPage;
