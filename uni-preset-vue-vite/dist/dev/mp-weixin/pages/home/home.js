"use strict";
const common_vendor = require("../../common/vendor.js");
if (!Array) {
  const _easycom_u_tabbar_item2 = common_vendor.resolveComponent("u-tabbar-item");
  const _easycom_u_tabbar2 = common_vendor.resolveComponent("u-tabbar");
  (_easycom_u_tabbar_item2 + _easycom_u_tabbar2)();
}
const _easycom_u_tabbar_item = () => "../../node-modules/uview-plus/components/u-tabbar-item/u-tabbar-item.js";
const _easycom_u_tabbar = () => "../../node-modules/uview-plus/components/u-tabbar/u-tabbar.js";
if (!Math) {
  (_easycom_u_tabbar_item + _easycom_u_tabbar + ActivityList + SignIn + MyActivity + Personal)();
}
const ActivityList = () => "../activityList/activityList2.js";
const SignIn = () => "../signList/signList2.js";
const MyActivity = () => "../myActivity/myActivity2.js";
const Personal = () => "../personal/personal2.js";
const _sfc_main = {
  __name: "home",
  setup(__props) {
    const active = common_vendor.ref(0);
    function onChange(index) {
      active.value = index;
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.p({
          icon: "list",
          title: "活动"
        }),
        b: common_vendor.p({
          icon: "checkmark-circle",
          title: "签到"
        }),
        c: common_vendor.p({
          icon: "myActivity",
          title: "参加的活动"
        }),
        d: common_vendor.p({
          icon: "user",
          title: "个人"
        }),
        e: common_vendor.o(onChange),
        f: common_vendor.p({
          active: active.value
        }),
        g: active.value === 0,
        h: active.value === 1,
        i: active.value === 2,
        j: active.value === 3
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0cd09a48"]]);
wx.createPage(MiniProgramPage);
