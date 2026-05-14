"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
if (!Array) {
  const _easycom_u_album2 = common_vendor.resolveComponent("u-album");
  const _easycom_u_upload2 = common_vendor.resolveComponent("u-upload");
  const _easycom_u_button2 = common_vendor.resolveComponent("u-button");
  (_easycom_u_album2 + _easycom_u_upload2 + _easycom_u_button2)();
}
const _easycom_u_album = () => "../../node-modules/uview-plus/components/u-album/u-album.js";
const _easycom_u_upload = () => "../../node-modules/uview-plus/components/u-upload/u-upload.js";
const _easycom_u_button = () => "../../node-modules/uview-plus/components/u-button/u-button.js";
if (!Math) {
  (_easycom_u_album + _easycom_u_upload + _easycom_u_button)();
}
const _sfc_main = {
  __name: "activityDetail",
  setup(__props) {
    const activity = common_vendor.ref({});
    const activityId = common_vendor.ref(null);
    const creatorName = common_vendor.ref("");
    const galleryUrls = common_vendor.ref([]);
    const uploadList = common_vendor.ref([]);
    const hasSignedUp = common_vendor.ref(false);
    common_vendor.onLoad((options) => {
      activityId.value = options.id;
      loadActivity();
      loadGallery();
      checkSignup();
    });
    function loadActivity() {
      utils_request.request.get("/activity/" + activityId.value).then((res) => {
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        activity.value = packet ? packet.data : res && res.data !== void 0 ? res.data : res;
        if (activity.value && activity.value.creatorId) {
          utils_request.request.get(`/auth/info?userId=${activity.value.creatorId}`).then((info) => {
            const ipacket = info && info.code !== void 0 ? info : info && info.data && info.data.code !== void 0 ? info.data : null;
            const idata = ipacket ? ipacket.data : info && info.data !== void 0 ? info.data : info;
            creatorName.value = idata && (idata.nickname || idata.username) || "";
          });
        }
      }).catch((err) => {
        console.error("加载活动详情失败:", err);
      });
    }
    async function loadGallery() {
      try {
        const res = await utils_request.request.get("/album/list/" + activityId.value);
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        galleryUrls.value = (arr || []).map((i) => i.url || i.imageUrl || i);
      } catch (e) {
        galleryUrls.value = [];
      }
    }
    function checkSignup() {
      utils_request.request.get("/signup/list/" + activityId.value).then((res) => {
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        const uid = common_vendor.index.getStorageSync("userId");
        hasSignedUp.value = Array.isArray(arr) ? arr.some((i) => Number(i.userId) === Number(uid)) : false;
      }).catch(() => {
        hasSignedUp.value = false;
      });
    }
    function afterReadPhoto(event) {
      const file = event.file;
      const token = common_vendor.index.getStorageSync("token");
      common_vendor.index.uploadFile({
        url: "http://localhost:8080/api/upload/image",
        filePath: file.url,
        name: "file",
        header: token ? { Authorization: "Bearer " + token } : {},
        success(res) {
          let data = {};
          try {
            data = JSON.parse(res.data);
          } catch (e) {
          }
          const url = data.url || data.data || "";
          if (!url) {
            common_vendor.index.showToast({ title: "上传失败", icon: "none" });
            return;
          }
          utils_request.request.post("/album/add", {
            activityId: Number(activityId.value),
            userId: Number(common_vendor.index.getStorageSync("userId")),
            url
          }).then(() => {
            uploadList.value = [];
            common_vendor.index.showToast({ title: "上传成功", icon: "success" });
            loadGallery();
          }).catch(() => {
            common_vendor.index.showToast({ title: "保存失败", icon: "none" });
          });
        },
        fail() {
          common_vendor.index.showToast({ title: "上传失败", icon: "none" });
        }
      });
    }
    function goSignup() {
      common_vendor.index.navigateTo({
        url: "/pages/signupList/signupList?id=" + activityId.value
      });
    }
    function goActivityRoom() {
      common_vendor.index.navigateTo({
        url: "/pages/activityRoom/activityRoom?id=" + activityId.value
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(activity.value.title),
        b: common_vendor.t(activity.value.startTime),
        c: common_vendor.t(activity.value.endTime),
        d: common_vendor.t(activity.value.signupStart),
        e: common_vendor.t(activity.value.signupEnd),
        f: common_vendor.t(activity.value.location),
        g: common_vendor.t(activity.value.feeRule),
        h: common_vendor.t(creatorName.value || activity.value.creatorId),
        i: common_vendor.t(activity.value.description),
        j: common_vendor.p({
          urls: galleryUrls.value
        }),
        k: hasSignedUp.value
      }, hasSignedUp.value ? {
        l: common_vendor.o(afterReadPhoto),
        m: common_vendor.p({
          ["max-count"]: 1,
          ["file-list"]: uploadList.value
        })
      } : {}, {
        n: common_vendor.o(goSignup),
        o: common_vendor.p({
          type: "primary"
        }),
        p: hasSignedUp.value
      }, hasSignedUp.value ? {
        q: common_vendor.o(goActivityRoom),
        r: common_vendor.p({
          type: "success"
        })
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1536b735"]]);
wx.createPage(MiniProgramPage);
