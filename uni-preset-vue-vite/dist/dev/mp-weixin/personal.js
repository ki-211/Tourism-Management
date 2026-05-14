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
  __name: "personal",
  setup(__props) {
    const userInfo = common_vendor.ref({});
    function loadUserInfo() {
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId) {
        common_vendor.index.redirectTo({ url: "/pages/login/login" });
        return;
      }
      utils_request.request.get(`/auth/info?userId=${userId}`).then((res) => {
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        userInfo.value = packet ? packet.data || {} : res && res.data !== void 0 ? res.data || {} : res || {};
      });
    }
    common_vendor.onMounted(loadUserInfo);
    common_vendor.onShow(loadUserInfo);
    function toEdit() {
      common_vendor.index.navigateTo({ url: "/pages/editInfo/editInfo" });
    }
    function logout() {
      common_vendor.index.removeStorageSync("token");
      common_vendor.index.removeStorageSync("userId");
      common_vendor.index.redirectTo({ url: "/pages/login/login" });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(logout),
        b: common_vendor.p({
          type: "error",
          size: "small"
        }),
        c: common_vendor.t(userInfo.value.nickname || "未命名用户"),
        d: common_vendor.t(userInfo.value.username || "未知"),
        e: common_vendor.o(toEdit),
        f: common_vendor.p({
          type: "primary",
          size: "mini"
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-73f30864"]]);
exports.MiniProgramPage = MiniProgramPage;
