"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const _sfc_main = {
  __name: "activityRoom",
  setup(__props) {
    const activityId = common_vendor.ref(null);
    const activityTitle = common_vendor.ref("活动室");
    const currentUserId = common_vendor.ref(null);
    const isCreator = common_vendor.ref(false);
    const currentTab = common_vendor.ref(0);
    const tabList = common_vendor.ref([
      { name: "聊天" },
      { name: "签到" },
      { name: "位置" }
    ]);
    const messages = common_vendor.ref([]);
    const messageInput = common_vendor.ref("");
    const scrollTop = common_vendor.ref(0);
    const signTasks = common_vendor.ref([]);
    const participants = common_vendor.ref([]);
    const currentLocation = common_vendor.ref(null);
    const locationEnabled = common_vendor.ref(false);
    common_vendor.ref(false);
    const mapMarkers = common_vendor.ref([]);
    const mapCenter = common_vendor.ref({ latitude: 39.9042, longitude: 116.4074 });
    const selectedUserId = common_vendor.ref(null);
    const drawerExpanded = common_vendor.ref(false);
    const isGettingLocation = common_vendor.ref(false);
    const mapLoadError = common_vendor.ref(false);
    const mapErrorMessage = common_vendor.ref("");
    let locationUploadTimer = null;
    let drawerStartY = 0;
    let drawerCurrentY = 0;
    let mapContext = null;
    let messageTimer = null;
    let signTaskTimer = null;
    let locationTimer = null;
    common_vendor.onLoad((options) => {
      activityId.value = options.id ? Number(options.id) : null;
      currentUserId.value = common_vendor.index.getStorageSync("userId");
      if (!currentUserId.value) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        setTimeout(() => {
          common_vendor.index.redirectTo({ url: "/pages/login/login" });
        }, 1500);
        return;
      }
      if (!activityId.value) {
        common_vendor.index.showToast({ title: "活动ID无效", icon: "none" });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
        return;
      }
      loadActivityInfo();
      loadMessages();
      loadSignTasks();
      messageTimer = setInterval(loadMessages, 3e3);
      signTaskTimer = setInterval(loadSignTasks, 5e3);
      locationTimer = setInterval(loadParticipants, 1e4);
      setTimeout(() => {
        mapContext = common_vendor.index.createMapContext("activityMap");
      }, 500);
    });
    common_vendor.onShow(() => {
      if (activityId.value) {
        loadMessages();
        loadSignTasks();
      }
    });
    common_vendor.onMounted(() => {
      common_vendor.index.$on("refreshActivityRoom", () => {
        loadMessages();
        loadSignTasks();
      });
    });
    function onTabChange(index) {
      currentTab.value = index;
      if (index === 0) {
        loadMessages();
      } else if (index === 1) {
        loadSignTasks();
      } else if (index === 2) {
        loadParticipants();
        if (!locationEnabled.value) {
          requestLocation();
        }
      }
    }
    async function loadActivityInfo() {
      try {
        const res = await utils_request.request.get("/activity/" + activityId.value);
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const data = packet ? packet.data : res && res.data !== void 0 ? res.data : res;
        if (data) {
          activityTitle.value = data.title || "活动室";
          isCreator.value = Number(data.creatorId) === Number(currentUserId.value);
        }
      } catch (e) {
        console.error("加载活动信息失败:", e);
      }
    }
    async function loadMessages() {
      if (!activityId.value) {
        console.error("活动ID为空，无法加载消息");
        return;
      }
      try {
        const res = await utils_request.request.get("/chat/list", { activityId: activityId.value });
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        messages.value = Array.isArray(arr) ? arr : [];
        common_vendor.nextTick$1(() => {
          scrollTop.value = 999999;
        });
      } catch (e) {
        console.error("加载消息失败:", e);
      }
    }
    async function sendMessage() {
      if (!messageInput.value.trim()) {
        return common_vendor.index.showToast({ title: "请输入消息内容", icon: "none" });
      }
      if (!activityId.value) {
        return common_vendor.index.showToast({ title: "活动ID无效", icon: "none" });
      }
      try {
        await utils_request.request.post("/chat/send", {
          activityId: activityId.value,
          userId: currentUserId.value,
          content: messageInput.value.trim()
        });
        messageInput.value = "";
        await loadMessages();
      } catch (e) {
        console.error("发送消息失败:", e);
        common_vendor.index.showToast({ title: "发送失败", icon: "none" });
      }
    }
    async function loadSignTasks() {
      if (!activityId.value) {
        console.error("活动ID为空，无法加载签到任务");
        return;
      }
      try {
        const res = await utils_request.request.get("/signTask/listByActivity", { activityId: activityId.value });
        const packet = res && res.code !== void 0 ? res : res && res.data && res.data.code !== void 0 ? res.data : null;
        const arr = packet ? packet.data || [] : Array.isArray(res) ? res : res && res.data ? res.data : [];
        const tasks = Array.isArray(arr) ? arr : [];
        for (let task of tasks) {
          try {
            const signRes = await utils_request.request.get("/signRecord/list", { taskId: task.id });
            const signPacket = signRes && signRes.code !== void 0 ? signRes : signRes && signRes.data && signRes.data.code !== void 0 ? signRes.data : null;
            const signArr = signPacket ? signPacket.data || [] : Array.isArray(signRes) ? signRes : signRes && signRes.data ? signRes.data : [];
            task.signedCount = Array.isArray(signArr) ? signArr.length : 0;
            task.hasSigned = Array.isArray(signArr) ? signArr.some((r) => Number(r.userId) === Number(currentUserId.value)) : false;
          } catch (e) {
            task.signedCount = 0;
            task.hasSigned = false;
          }
        }
        signTasks.value = tasks;
      } catch (e) {
        console.error("加载签到任务失败:", e);
      }
    }
    function goCreateSign() {
      common_vendor.index.navigateTo({
        url: `/pages/signTask/signTask?id=${activityId.value}`
      });
    }
    function goTaskDetail(taskId) {
      common_vendor.index.navigateTo({
        url: `/pages/signTaskDetail/signTaskDetail?taskId=${taskId}`
      });
    }
    async function doSign(taskId) {
      try {
        await utils_request.request.post("/signRecord/sign", {
          taskId,
          userId: currentUserId.value
        });
        common_vendor.index.showToast({ title: "签到成功", icon: "success" });
        await loadSignTasks();
      } catch (e) {
        common_vendor.index.showToast({ title: "签到失败", icon: "none" });
      }
    }
    async function requestLocation() {
      isGettingLocation.value = true;
      common_vendor.index.showLoading({ title: "获取位置中..." });
      try {
        console.log("🌐 使用 uni.getLocation 获取位置...");
        const res = await new Promise((resolve, reject) => {
          common_vendor.index.getLocation({
            type: "gcj02",
            success: (loc) => {
              console.log("✅ 位置获取成功，原始数据:", loc);
              resolve(loc);
            },
            fail: (err) => {
              console.error("❌ 位置获取失败:", err);
              reject(err);
            }
          });
        });
        common_vendor.index.hideLoading();
        const lat = res.latitude;
        const lng = res.longitude;
        console.log("🎯 获取到的真实位置(uni):", lat, lng);
        const isInShenzhen = lat >= 22.4 && lat <= 22.9 && lng >= 113.7 && lng <= 114.6;
        const isInChina = lat >= 3.5 && lat <= 53.5 && lng >= 73 && lng <= 135;
        console.log("📍 位置验证:", {
          是否在深圳范围: isInShenzhen ? "✅ 是" : "❌ 否",
          是否在中国范围: isInChina ? "✅ 是" : "❌ 否",
          实际位置: isInShenzhen ? "深圳" : isInChina ? "中国其他地区" : "⚠️ 国外或错误坐标"
        });
        if (!isInChina) {
          common_vendor.index.showModal({
            title: "⚠️ 位置异常",
            content: `获取到的坐标 (${lat.toFixed(4)}, ${lng.toFixed(4)}) 不在中国境内。

建议重新获取位置。`,
            confirmText: "继续",
            cancelText: "重试",
            success: (modalRes) => {
              if (modalRes.cancel) {
                requestLocation();
                return;
              }
            }
          });
        }
        currentLocation.value = {
          latitude: lat,
          longitude: lng,
          address: res.address || "未知位置"
        };
        mapCenter.value = {
          latitude: lat,
          longitude: lng
        };
        console.log("🗺️ 更新后的地图中心:", mapCenter.value);
        locationEnabled.value = true;
        isGettingLocation.value = false;
        common_vendor.index.showToast({
          title: `位置: ${lat.toFixed(4)}, ${lng.toFixed(4)}`,
          icon: "none",
          duration: 3e3
        });
        await updateLocation();
        await loadParticipants();
        updateMarkersFromParticipants();
        setTimeout(() => {
          if (mapContext) {
            console.log("🚀 使用MapContext移动地图到:", lat, lng);
            mapContext.moveToLocation({
              latitude: lat,
              longitude: lng
            });
          }
        }, 500);
        startLocationAutoUpdate();
      } catch (e) {
        common_vendor.index.hideLoading();
        isGettingLocation.value = false;
        console.error("❌ 获取位置失败:", e);
        if (e.errMsg && e.errMsg.indexOf("auth") !== -1) {
          common_vendor.index.showModal({
            title: "位置权限",
            content: "需要获取位置权限才能使用位置功能，请在设置中开启位置权限",
            confirmText: "去设置",
            cancelText: "取消",
            success: (res) => {
              if (res.confirm) {
                common_vendor.index.openSetting({
                  success: (settingRes) => {
                    if (settingRes.authSetting["scope.userLocation"]) {
                      common_vendor.index.showToast({ title: "请重新点击开启位置", icon: "none" });
                    }
                  }
                });
              }
            }
          });
        } else {
          common_vendor.index.showModal({
            title: "定位失败",
            content: "无法获取位置信息，请确保：\n1. 已开启手机GPS定位\n2. 网络连接正常\n3. 在户外或窗边信号较好的地方",
            showCancel: false,
            confirmText: "我知道了"
          });
        }
      }
    }
    function stopLocation() {
      if (locationUploadTimer) {
        clearInterval(locationUploadTimer);
        locationUploadTimer = null;
      }
      locationEnabled.value = false;
      currentLocation.value = null;
      common_vendor.index.showToast({ title: "位置已关闭", icon: "none" });
    }
    function startLocationAutoUpdate() {
      if (locationUploadTimer)
        return;
      const uploadOnce = async () => {
        try {
          const res = await new Promise((resolve, reject) => {
            common_vendor.index.getLocation({ type: "gcj02", success: resolve, fail: reject });
          });
          currentLocation.value = {
            latitude: res.latitude,
            longitude: res.longitude,
            address: res.address || `${res.latitude.toFixed(6)}, ${res.longitude.toFixed(6)}`
          };
          await updateLocation();
          await loadParticipants();
        } catch (e) {
          console.error("自动获取/上传位置失败:", e);
        }
      };
      uploadOnce();
      locationUploadTimer = setInterval(uploadOnce, 15e3);
    }
    function updateMarkersFromParticipants() {
      const markers = [];
      for (const p of participants.value) {
        if (p.latitude && p.longitude) {
          markers.push({
            id: Number(p.userId || p.user_id || 0),
            latitude: Number(p.latitude),
            longitude: Number(p.longitude),
            width: 34,
            height: 34,
            iconPath: "/static/icons/marker.svg",
            callout: {
              content: `${p.nickname || "用户" + p.userId}
${p.address || ""}`,
              color: "#000",
              fontSize: 12,
              borderRadius: 6,
              padding: 6,
              display: "BYCLICK"
            }
          });
        }
      }
      if (currentLocation.value && currentLocation.value.latitude && currentLocation.value.longitude) {
        markers.unshift({
          id: Number(currentUserId.value || 0) * 1e5,
          latitude: Number(currentLocation.value.latitude),
          longitude: Number(currentLocation.value.longitude),
          width: 36,
          height: 36,
          iconPath: "/static/icons/marker-me.svg",
          callout: {
            content: "我\n" + (currentLocation.value.address || ""),
            color: "#000",
            fontSize: 12,
            borderRadius: 6,
            padding: 6,
            display: "BYCLICK"
          }
        });
        mapCenter.value.latitude = Number(currentLocation.value.latitude);
        mapCenter.value.longitude = Number(currentLocation.value.longitude);
      } else if (markers.length > 0) {
        mapCenter.value.latitude = markers[0].latitude;
        mapCenter.value.longitude = markers[0].longitude;
      }
      mapMarkers.value = markers;
    }
    function onMarkerTap(e) {
      const markerId = e && e.markerId || e && e.detail && e.detail.markerId || (e && e.detail && e.detail.markerId === 0 ? 0 : void 0);
      if (markerId === void 0)
        return;
      const marker = mapMarkers.value.find((m) => Number(m.id) === Number(markerId));
      if (!marker)
        return;
      const participant = participants.value.find((p) => Number(p.userId || p.user_id) === Number(markerId) || Number(p.userId || 0) * 1e5 === Number(markerId));
      const name = participant ? participant.nickname || "用户" + participant.userId : "位置";
      const address = participant ? participant.address || "" : "";
      common_vendor.index.showModal({
        title: name,
        content: address || "查看位置",
        confirmText: "导航",
        cancelText: "关闭",
        success: (res) => {
          if (res.confirm) {
            common_vendor.index.openLocation({
              latitude: Number(marker.latitude),
              longitude: Number(marker.longitude),
              name,
              address,
              scale: 18
            });
          }
        }
      });
    }
    function onMapUpdated(e) {
      console.log("🗺️ 地图已更新:", e);
      mapLoadError.value = false;
    }
    function onMapError(e) {
      console.error("❌ 地图加载错误:", e);
      mapLoadError.value = true;
      const lat = mapCenter.value.latitude;
      const lng = mapCenter.value.longitude;
      if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        mapErrorMessage.value = "坐标超出有效范围";
      } else if (lat > 53.5 || lat < 3.5 || lng > 135 || lng < 73) {
        mapErrorMessage.value = "坐标可能超出地图服务范围";
      } else {
        mapErrorMessage.value = "请检查网络连接或刷新页面";
      }
      common_vendor.index.showToast({
        title: "地图加载失败",
        icon: "none",
        duration: 2e3
      });
    }
    async function updateLocation() {
      if (!currentLocation.value || !activityId.value) {
        console.error("❌ 无法更新位置:", {
          hasLocation: !!currentLocation.value,
          hasActivityId: !!activityId.value
        });
        return;
      }
      try {
        const uploadData = {
          activityId: activityId.value,
          userId: currentUserId.value,
          latitude: currentLocation.value.latitude,
          longitude: currentLocation.value.longitude,
          address: currentLocation.value.address
        };
        console.log("📤 上传位置到服务器:", uploadData);
        const res = await utils_request.request.post("/location/update", uploadData);
        console.log("✅ 位置上传成功，服务器响应:", res);
        updateMarkersFromParticipants();
      } catch (e) {
        console.error("❌ 更新位置失败:", e);
        common_vendor.index.showToast({
          title: "位置上传失败: " + e.message,
          icon: "none",
          duration: 2e3
        });
      }
    }
    async function loadParticipants() {
      if (!activityId.value)
        return;
      try {
        console.log("📥 从服务器加载参与者位置...");
        const res = await utils_request.request.get("/location/list", { activityId: activityId.value });
        console.log("📍 位置列表响应:", res);
        let data = null;
        if (res && res.success && res.data) {
          data = res.data;
        } else if (res && res.code === 0 && res.data) {
          data = res.data;
        } else if (Array.isArray(res)) {
          data = res;
        } else if (res && res.data && Array.isArray(res.data)) {
          data = res.data;
        }
        participants.value = Array.isArray(data) ? data : [];
        console.log("✅ 参与者位置列表:", participants.value);
        if (participants.value.length > 0) {
          participants.value.forEach((p) => {
            console.log(`  - ${p.nickname || "用户" + p.userId}: (${p.latitude}, ${p.longitude})`);
          });
        }
        updateMarkersFromParticipants();
      } catch (e) {
        console.error("❌ 加载参与者位置失败:", e);
        participants.value = [];
      }
    }
    function calculateDistance(lat1, lon1, lat2, lon2) {
      const R = 6371;
      const dLat = (lat2 - lat1) * Math.PI / 180;
      const dLon = (lon2 - lon1) * Math.PI / 180;
      const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      const distance = R * c;
      if (distance < 1) {
        return Math.round(distance * 1e3) + "m";
      } else {
        return distance.toFixed(1) + "km";
      }
    }
    function getDistanceFromMe(participant) {
      if (!currentLocation.value || !participant.latitude || !participant.longitude) {
        return "未知距离";
      }
      return calculateDistance(
        currentLocation.value.latitude,
        currentLocation.value.longitude,
        participant.latitude,
        participant.longitude
      );
    }
    function formatTime(timeStr) {
      if (!timeStr)
        return "";
      const time = new Date(timeStr);
      const now = /* @__PURE__ */ new Date();
      const diff = now - time;
      if (diff < 6e4)
        return "刚刚";
      if (diff < 36e5)
        return Math.floor(diff / 6e4) + "分钟前";
      if (diff < 864e5)
        return Math.floor(diff / 36e5) + "小时前";
      const month = String(time.getMonth() + 1).padStart(2, "0");
      const day = String(time.getDate()).padStart(2, "0");
      const hour = String(time.getHours()).padStart(2, "0");
      const minute = String(time.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hour}:${minute}`;
    }
    function onParticipantClick(participant) {
      selectedUserId.value = participant.userId;
      if (participant.latitude && participant.longitude) {
        const lat = Number(participant.latitude);
        const lng = Number(participant.longitude);
        mapCenter.value = { latitude: lat, longitude: lng };
        if (mapContext) {
          mapContext.moveToLocation({
            latitude: lat,
            longitude: lng,
            success: () => {
              console.log("地图移动成功");
            }
          });
          mapContext.getCenterLocation({
            success: (res) => {
              console.log("当前地图中心:", res);
            }
          });
        }
        common_vendor.index.showToast({
          title: `定位到${participant.nickname || "该用户"}`,
          icon: "none",
          duration: 1500
        });
      }
      drawerExpanded.value = false;
    }
    function centerToMyLocation() {
      if (currentLocation.value && currentLocation.value.latitude && currentLocation.value.longitude) {
        const lat = Number(currentLocation.value.latitude);
        const lng = Number(currentLocation.value.longitude);
        mapCenter.value = { latitude: lat, longitude: lng };
        selectedUserId.value = currentUserId.value;
        if (mapContext) {
          mapContext.moveToLocation({
            latitude: lat,
            longitude: lng
          });
        }
        common_vendor.index.showToast({
          title: "已定位到我的位置",
          icon: "none",
          duration: 1500
        });
      } else {
        common_vendor.index.showToast({
          title: "当前位置未获取",
          icon: "none"
        });
      }
    }
    function toggleDrawer() {
      drawerExpanded.value = !drawerExpanded.value;
    }
    function showManualLocationPicker() {
      const shenzhenAreas = [
        { name: "坪山区", lat: 22.7089, lng: 114.35 },
        { name: "福田区", lat: 22.5474, lng: 114.0549 },
        { name: "南山区", lat: 22.5329, lng: 113.9303 },
        { name: "罗湖区", lat: 22.5551, lng: 114.1249 },
        { name: "龙岗区", lat: 22.7209, lng: 114.2472 },
        { name: "宝安区", lat: 22.554, lng: 113.8832 },
        { name: "龙华区", lat: 22.6568, lng: 114.0364 },
        { name: "盐田区", lat: 22.5574, lng: 114.2361 },
        { name: "光明区", lat: 22.7492, lng: 113.938 },
        { name: "大鹏新区", lat: 22.5942, lng: 114.4779 }
      ];
      const itemList = shenzhenAreas.map((area) => `深圳市${area.name}`);
      common_vendor.index.showActionSheet({
        itemList,
        success: async (res) => {
          const selectedArea = shenzhenAreas[res.tapIndex];
          currentLocation.value = {
            latitude: selectedArea.lat,
            longitude: selectedArea.lng,
            address: `深圳市${selectedArea.name}`
          };
          mapCenter.value = {
            latitude: selectedArea.lat,
            longitude: selectedArea.lng
          };
          locationEnabled.value = true;
          common_vendor.index.showToast({
            title: `已设置为${selectedArea.name}`,
            icon: "success"
          });
          console.log("📍 手动设置位置:", selectedArea.name, selectedArea.lat, selectedArea.lng);
          await updateLocation();
          await loadParticipants();
          updateMarkersFromParticipants();
          setTimeout(() => {
            if (mapContext) {
              mapContext.moveToLocation({
                latitude: selectedArea.lat,
                longitude: selectedArea.lng
              });
            }
          }, 500);
          startLocationAutoUpdate();
        }
      });
    }
    function onDrawerTouchStart(e) {
      drawerStartY = e.touches[0].clientY;
    }
    function onDrawerTouchMove(e) {
      drawerCurrentY = e.touches[0].clientY;
    }
    function onDrawerTouchEnd() {
      const deltaY = drawerStartY - drawerCurrentY;
      if (deltaY > 50) {
        drawerExpanded.value = true;
      } else if (deltaY < -50) {
        drawerExpanded.value = false;
      }
      drawerStartY = 0;
      drawerCurrentY = 0;
    }
    common_vendor.onUnmounted(() => {
      if (messageTimer)
        clearInterval(messageTimer);
      if (signTaskTimer)
        clearInterval(signTaskTimer);
      if (locationTimer)
        clearInterval(locationTimer);
      if (locationUploadTimer)
        clearInterval(locationUploadTimer);
      common_vendor.index.$off("refreshActivityRoom");
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(activityTitle.value),
        b: common_vendor.f(tabList.value, (tab, index, i0) => {
          return {
            a: common_vendor.t(tab.name),
            b: index,
            c: currentTab.value === index ? 1 : "",
            d: common_vendor.o(($event) => onTabChange(index), index)
          };
        }),
        c: messages.value.length === 0
      }, messages.value.length === 0 ? {} : {}, {
        d: common_vendor.f(messages.value, (msg, k0, i0) => {
          return {
            a: common_vendor.t(msg.nickname || "用户" + msg.userId),
            b: common_vendor.t(formatTime(msg.createTime)),
            c: common_vendor.t(msg.content),
            d: msg.id,
            e: msg.userId == currentUserId.value ? 1 : ""
          };
        }),
        e: scrollTop.value,
        f: common_vendor.o(sendMessage),
        g: messageInput.value,
        h: common_vendor.o(($event) => messageInput.value = $event.detail.value),
        i: common_vendor.o(sendMessage),
        j: currentTab.value === 0,
        k: isCreator.value
      }, isCreator.value ? {
        l: common_vendor.o(goCreateSign)
      } : {}, {
        m: signTasks.value.length === 0
      }, signTasks.value.length === 0 ? {} : {}, {
        n: common_vendor.f(signTasks.value, (task, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(task.title),
            b: common_vendor.t(formatTime(task.createTime)),
            c: task.description
          }, task.description ? {
            d: common_vendor.t(task.description)
          } : {}, {
            e: common_vendor.t(task.signedCount || 0),
            f: common_vendor.o(($event) => goTaskDetail(task.id), task.id),
            g: !task.hasSigned
          }, !task.hasSigned ? {
            h: common_vendor.o(($event) => doSign(task.id), task.id)
          } : {}, {
            i: task.id
          });
        }),
        o: currentTab.value === 1,
        p: !locationEnabled.value
      }, !locationEnabled.value ? {
        q: common_vendor.o(requestLocation),
        r: common_vendor.o(showManualLocationPicker)
      } : common_vendor.e({
        s: isGettingLocation.value || mapMarkers.value.length === 0
      }, isGettingLocation.value || mapMarkers.value.length === 0 ? {
        t: common_vendor.t(isGettingLocation.value ? "正在获取你的位置..." : "等待位置信息...")
      } : {}, {
        v: mapLoadError.value
      }, mapLoadError.value ? {
        w: common_vendor.t(mapErrorMessage.value)
      } : {}, {
        x: mapCenter.value.latitude,
        y: mapCenter.value.longitude,
        z: mapMarkers.value,
        A: selectedUserId.value ? 16 : 14,
        B: common_vendor.o(onMarkerTap),
        C: common_vendor.o(onMapUpdated),
        D: common_vendor.o(onMapError),
        E: common_vendor.o(centerToMyLocation),
        F: common_vendor.o(stopLocation),
        G: common_vendor.t(participants.value.length),
        H: common_vendor.o(toggleDrawer),
        I: participants.value.length === 0
      }, participants.value.length === 0 ? {} : {}, {
        J: common_vendor.f(participants.value, (participant, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(participant.nickname ? participant.nickname.charAt(0) : "用"),
            b: participant.userId == currentUserId.value
          }, participant.userId == currentUserId.value ? {} : {}, {
            c: participant.userId == currentUserId.value ? 1 : "",
            d: common_vendor.t(participant.nickname || "用户" + participant.userId),
            e: participant.userId == currentUserId.value
          }, participant.userId == currentUserId.value ? {} : {}, {
            f: common_vendor.t(participant.address || "位置未知"),
            g: common_vendor.t(formatTime(participant.updateTime)),
            h: common_vendor.t(getDistanceFromMe(participant)),
            i: participant.userId,
            j: selectedUserId.value === participant.userId ? 1 : "",
            k: common_vendor.o(($event) => onParticipantClick(participant), participant.userId)
          });
        }),
        K: drawerExpanded.value ? "60vh" : "35vh",
        L: drawerExpanded.value ? 1 : "",
        M: common_vendor.o(onDrawerTouchStart),
        N: common_vendor.o(onDrawerTouchMove),
        O: common_vendor.o(onDrawerTouchEnd)
      }), {
        P: currentTab.value === 2
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-76c5ea80"]]);
wx.createPage(MiniProgramPage);
