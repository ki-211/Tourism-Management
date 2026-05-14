"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
if (!Math) {
  "./pages/login/login.js";
  "./pages/register/register.js";
  "./pages/home/home.js";
  "./pages/activityCreate/activityCreate.js";
  "./pages/activityDetail/activityDetail.js";
  "./pages/activityList/activityList.js";
  "./pages/signupList/signupList.js";
  "./pages/vehicleAdd/vehicleAdd.js";
  "./pages/vehicleList/vehicleList.js";
  "./pages/myActivity/myActivity.js";
  "./pages/myPublished/myPublished.js";
  "./pages/personal/personal.js";
  "./pages/editInfo/editInfo.js";
  "./pages/signList/signList.js";
  "./pages/signTask/signTask.js";
  "./pages/signDetail/signDetail.js";
  "./pages/ongoingSignup/ongoingSignup.js";
  "./pages/activityRoom/activityRoom.js";
  "./pages/signTaskDetail/signTaskDetail.js";
}
const _sfc_main = {
  onLaunch: function() {
    console.log("App Launch");
  },
  onShow: function() {
    console.log("App Show");
  },
  onHide: function() {
    console.log("App Hide");
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  app.use(common_vendor.uView);
  return { app };
}
createApp().app.mount("#app");
exports.createApp = createApp;
