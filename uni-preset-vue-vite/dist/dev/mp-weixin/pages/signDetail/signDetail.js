"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const _sfc_main = {
  __name: "signDetail",
  setup(__props) {
    const records = common_vendor.ref([]);
    const loadRecords = async () => {
      const userId = common_vendor.index.getStorageSync("userId");
      try {
        const res = await utils_request.request.get("/signRecord/listByUser", { userId });
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        records.value = arr.map((item) => ({
          ...item,
          signTime: (item.signTime || "").replace("T", " ")
        }));
      } catch (e) {
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    };
    common_vendor.onMounted(loadRecords);
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: records.value.length === 0
      }, records.value.length === 0 ? {} : {}, {
        b: common_vendor.f(records.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.signTime),
            c: item.id
          };
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-41d7d617"]]);
wx.createPage(MiniProgramPage);
