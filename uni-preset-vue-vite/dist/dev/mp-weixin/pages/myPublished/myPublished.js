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
  __name: "myPublished",
  setup(__props) {
    const list = common_vendor.ref([]);
    const loadPublished = async () => {
      const userId = common_vendor.index.getStorageSync("userId");
      if (!userId) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      try {
        const res = await utils_request.request.get(`/activity/published?userId=${userId}`);
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        list.value = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
      } catch (e) {
        list.value = [];
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    };
    common_vendor.onShow(() => {
      loadPublished();
    });
    common_vendor.onMounted(() => {
      loadPublished();
    });
    function goDetail(id) {
      common_vendor.index.navigateTo({
        url: "/pages/activityDetail/activityDetail?id=" + id
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(list.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.startTime),
            c: common_vendor.t(item.endTime),
            d: common_vendor.t(item.location),
            e: common_vendor.t(item.description),
            f: common_vendor.o(($event) => goDetail(item.id), item.id),
            g: "1b83397f-2-" + i0 + "," + ("1b83397f-1-" + i0),
            h: item.id,
            i: "1b83397f-1-" + i0 + ",1b83397f-0"
          };
        }),
        b: common_vendor.p({
          type: "primary",
          size: "mini"
        }),
        c: list.value.length === 0
      }, list.value.length === 0 ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1b83397f"]]);
wx.createPage(MiniProgramPage);
