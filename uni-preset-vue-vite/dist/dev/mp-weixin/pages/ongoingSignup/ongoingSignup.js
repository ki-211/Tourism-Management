"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_list_item2 = common_vendor.resolveComponent("u-list-item");
  const _easycom_u_list2 = common_vendor.resolveComponent("u-list");
  (_easycom_u_button2 + _easycom_u_list_item2 + _easycom_u_list2)();
}
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_list_item = () => "../../node-modules/uview-plus/components/u-list-item/u-list-item.js";
const _easycom_u_list = () => "../../node-modules/uview-plus/components/u-list/u-list.js";
if (!Math) {
  (_easycom_u_button + _easycom_u_list_item + _easycom_u_list)();
}
const _sfc_main = {
  __name: "ongoingSignup",
  setup(__props) {
    const all = common_vendor.ref([]);
    const ongoingList = common_vendor.ref([]);
    function parseDate(str) {
      if (!str)
        return null;
      const [datePart, timePart = "00:00:00"] = String(str).split(" ");
      const [y, m, d] = datePart.split("-").map((n) => parseInt(n, 10));
      const [hh, mm, ss] = timePart.split(":").map((n) => parseInt(n, 10));
      return new Date(y, m - 1, d, hh || 0, mm || 0, ss || 0);
    }
    function isSignupOpen(item) {
      var _a, _b;
      const now = Date.now();
      const start = (_a = parseDate(item.signupStart)) == null ? void 0 : _a.getTime();
      const end = (_b = parseDate(item.signupEnd)) == null ? void 0 : _b.getTime();
      if (!start || !end)
        return false;
      return now >= start && now <= end;
    }
    async function loadAll() {
      try {
        const res = await utils_request.request.get("/activity/all");
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        all.value = Array.isArray(arr) ? arr : [];
        ongoingList.value = all.value.filter(isSignupOpen);
      } catch (e) {
        all.value = [];
        ongoingList.value = [];
      }
    }
    common_vendor.onShow(loadAll);
    common_vendor.onMounted(loadAll);
    function goDetail(id) {
      common_vendor.index.navigateTo({
        url: `/pages/activityDetail/activityDetail?id=${encodeURIComponent(id)}`
      });
    }
    function goSignup(id) {
      common_vendor.index.navigateTo({
        url: `/pages/signupList/signupList?id=${encodeURIComponent(id)}`
      });
    }
    function goActivityRoom(id) {
      common_vendor.index.navigateTo({
        url: `/pages/activityRoom/activityRoom?id=${encodeURIComponent(id)}`
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(ongoingList.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.startTime),
            c: common_vendor.t(item.endTime),
            d: common_vendor.t(item.signupStart),
            e: common_vendor.t(item.signupEnd),
            f: common_vendor.t(item.location),
            g: common_vendor.t(item.feeRule),
            h: item.imageUrl
          }, item.imageUrl ? {
            i: item.imageUrl
          } : {}, {
            j: common_vendor.o(($event) => goDetail(item.id), item.id),
            k: "c4b71fc8-2-" + i0 + "," + ("c4b71fc8-1-" + i0),
            l: common_vendor.o(($event) => goSignup(item.id), item.id),
            m: "c4b71fc8-3-" + i0 + "," + ("c4b71fc8-1-" + i0),
            n: common_vendor.o(($event) => goActivityRoom(item.id), item.id),
            o: "c4b71fc8-4-" + i0 + "," + ("c4b71fc8-1-" + i0),
            p: item.id,
            q: "c4b71fc8-1-" + i0 + ",c4b71fc8-0"
          });
        }),
        b: common_vendor.p({
          type: "primary",
          size: "mini"
        }),
        c: common_vendor.p({
          type: "success",
          size: "mini",
          plain: true
        }),
        d: common_vendor.p({
          type: "warning",
          size: "mini",
          plain: true
        }),
        e: ongoingList.value.length === 0
      }, ongoingList.value.length === 0 ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c4b71fc8"]]);
wx.createPage(MiniProgramPage);
