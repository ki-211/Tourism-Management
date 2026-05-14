"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const _sfc_main = {
  __name: "signTaskDetail",
  setup(__props) {
    const taskId = common_vendor.ref(null);
    const taskInfo = common_vendor.ref({});
    const currentTab = common_vendor.ref(0);
    const signedUsers = common_vendor.ref([]);
    const unsignedUsers = common_vendor.ref([]);
    common_vendor.onLoad((options) => {
      taskId.value = options.taskId;
      if (!taskId.value) {
        common_vendor.index.showToast({ title: "任务ID无效", icon: "none" });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
        return;
      }
      loadTaskDetail();
    });
    async function loadTaskDetail() {
      try {
        const res = await utils_request.request.get("/signRecord/taskDetail", { taskId: taskId.value });
        const data = res.data || {};
        taskInfo.value = {
          taskId: data.taskId,
          taskTitle: data.taskTitle,
          taskDescription: data.taskDescription,
          createTime: data.createTime,
          activityId: data.activityId,
          activityTitle: data.activityTitle
        };
        signedUsers.value = data.signedUsers ? typeof data.signedUsers === "string" ? JSON.parse(data.signedUsers) : data.signedUsers : [];
        unsignedUsers.value = data.unsignedUsers ? typeof data.unsignedUsers === "string" ? JSON.parse(data.unsignedUsers) : data.unsignedUsers : [];
      } catch (e) {
        console.error("加载签到任务详情失败:", e);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    }
    function formatTime(timeStr) {
      if (!timeStr)
        return "";
      const time = new Date(timeStr);
      if (isNaN(time.getTime()))
        return timeStr;
      const month = String(time.getMonth() + 1).padStart(2, "0");
      const day = String(time.getDate()).padStart(2, "0");
      const hour = String(time.getHours()).padStart(2, "0");
      const minute = String(time.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hour}:${minute}`;
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(taskInfo.value.taskTitle),
        b: taskInfo.value.taskDescription
      }, taskInfo.value.taskDescription ? {
        c: common_vendor.t(taskInfo.value.taskDescription)
      } : {}, {
        d: common_vendor.t(formatTime(taskInfo.value.createTime)),
        e: common_vendor.t(signedUsers.value.length),
        f: currentTab.value === 0 ? 1 : "",
        g: common_vendor.o(($event) => currentTab.value = 0),
        h: common_vendor.t(unsignedUsers.value.length),
        i: currentTab.value === 1 ? 1 : "",
        j: common_vendor.o(($event) => currentTab.value = 1),
        k: signedUsers.value.length === 0
      }, signedUsers.value.length === 0 ? {} : {}, {
        l: common_vendor.f(signedUsers.value, (user, k0, i0) => {
          return {
            a: common_vendor.t(user.nickname ? user.nickname.charAt(0) : "用"),
            b: common_vendor.t(user.nickname || "用户" + user.userId),
            c: common_vendor.t(formatTime(user.signTime)),
            d: user.userId
          };
        }),
        m: currentTab.value === 0,
        n: unsignedUsers.value.length === 0
      }, unsignedUsers.value.length === 0 ? {} : {}, {
        o: common_vendor.f(unsignedUsers.value, (user, k0, i0) => {
          return {
            a: common_vendor.t(user.nickname ? user.nickname.charAt(0) : "用"),
            b: common_vendor.t(user.nickname || "用户" + user.userId),
            c: user.userId
          };
        }),
        p: currentTab.value === 1
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-92d4fe2b"]]);
wx.createPage(MiniProgramPage);
