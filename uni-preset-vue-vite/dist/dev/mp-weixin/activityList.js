"use strict";
const common_vendor = require("./common/vendor.js");
const utils_request = require("./utils/request.js");
if (!Array) {
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  const _easycom_u_list_item2 = common_vendor.resolveComponent("u-list-item");
  const _easycom_u_list2 = common_vendor.resolveComponent("u-list");
  (_easycom_u_button2 + _easycom_u_list_item2 + _easycom_u_list2)();
}
const _easycom_u_button = () => "./node-modules/uview-plus/components/u-button/u-button.js";
const _easycom_u_list_item = () => "./node-modules/uview-plus/components/u-list-item/u-list-item.js";
const _easycom_u_list = () => "./node-modules/uview-plus/components/u-list/u-list.js";
if (!Math) {
  (_easycom_u_button + _easycom_u_list_item + _easycom_u_list)();
}
const _sfc_main = {
  __name: "activityList",
  setup(__props) {
    const list = common_vendor.ref([]);
    const loadList = async () => {
      try {
        const res = await utils_request.request.get("/activity/all");
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        list.value = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
      } catch (e) {
        console.error("加载活动列表失败:", e);
      }
    };
    common_vendor.onShow(() => {
      loadList();
    });
    common_vendor.onMounted(() => {
      loadList();
    });
    function goDetail(id) {
      common_vendor.index.navigateTo({
        url: `/pages/activityDetail/activityDetail?id=${encodeURIComponent(id)}`
      });
    }
    function toCreate() {
      common_vendor.index.navigateTo({ url: "/pages/activityCreate/activityCreate" });
    }
    function toOngoing() {
      common_vendor.index.navigateTo({ url: "/pages/ongoingSignup/ongoingSignup" });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(toOngoing),
        b: common_vendor.p({
          type: "success",
          size: "mini",
          plain: true
        }),
        c: common_vendor.o(toCreate),
        d: common_vendor.p({
          type: "primary",
          size: "mini"
        }),
        e: common_vendor.f(list.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.startTime),
            c: common_vendor.t(item.endTime),
            d: common_vendor.t(item.location),
            e: common_vendor.t(item.signupStart),
            f: common_vendor.t(item.signupEnd),
            g: common_vendor.t(item.feeRule),
            h: common_vendor.t(item.description),
            i: item.imageUrl
          }, item.imageUrl ? {
            j: item.imageUrl
          } : {}, {
            k: common_vendor.o(($event) => goDetail(item.id), item.id),
            l: "e52301db-4-" + i0 + "," + ("e52301db-3-" + i0),
            m: item.id,
            n: "e52301db-3-" + i0 + ",e52301db-2"
          });
        }),
        f: common_vendor.p({
          type: "primary",
          size: "mini"
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e52301db"]]);
exports.MiniProgramPage = MiniProgramPage;
