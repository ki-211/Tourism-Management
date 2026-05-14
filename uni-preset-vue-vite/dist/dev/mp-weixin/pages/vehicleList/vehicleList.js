"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const _sfc_main = {
  __name: "vehicleList",
  setup(__props) {
    const vehicleList = common_vendor.ref([]);
    const activityId = common_vendor.ref(null);
    function formatDateTime(datetime) {
      if (!datetime)
        return "";
      const dateObj = new Date(datetime);
      if (isNaN(dateObj))
        return datetime;
      const y = dateObj.getFullYear();
      const m = (dateObj.getMonth() + 1).toString().padStart(2, "0");
      const d = dateObj.getDate().toString().padStart(2, "0");
      const hh = dateObj.getHours().toString().padStart(2, "0");
      const mm = dateObj.getMinutes().toString().padStart(2, "0");
      return `${y}年${m}月${d}日 ${hh}时${mm}分`;
    }
    common_vendor.onLoad((options) => {
      activityId.value = options.id;
      loadVehicleList();
    });
    function loadVehicleList() {
      utils_request.request.get("/vehicle/list/" + activityId.value).then((res) => {
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        vehicleList.value = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: vehicleList.value.length === 0
      }, vehicleList.value.length === 0 ? {} : {}, {
        b: common_vendor.f(vehicleList.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.plateNumber),
            b: common_vendor.t(item.driverName),
            c: common_vendor.t(formatDateTime(item.pickupTime)),
            d: common_vendor.t(item.pickupLocation),
            e: item.id
          };
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f1d7e7f6"]]);
wx.createPage(MiniProgramPage);
