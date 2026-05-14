"use strict";
const common_vendor = require("../../../../common/vendor.js");
const _sfc_main = {
  name: "u-list",
  mixins: [common_vendor.mpMixin, common_vendor.mixin, common_vendor.props$14],
  watch: {
    scrollIntoView(n) {
      this.scrollIntoViewById(n);
    }
  },
  data() {
    return {
      // 记录内部滚动的距离
      innerScrollTop: 0,
      // vue下，scroll-view在上拉加载时的偏移值
      offset: 0,
      sys: common_vendor.getWindowInfo()
    };
  },
  computed: {
    listStyle() {
      const style = {};
      if (this.width != 0)
        style.width = common_vendor.addUnit(this.width);
      if (this.height != 0)
        style.height = common_vendor.addUnit(this.height);
      if (!style.height)
        style.height = common_vendor.addUnit(this.sys.windowHeight, "px");
      return common_vendor.deepMerge(style, common_vendor.addStyle(this.customStyle));
    }
  },
  provide() {
    return {
      uList: this
    };
  },
  created() {
    this.refs = [];
    this.children = [];
    this.anchors = [];
  },
  mounted() {
  },
  emits: [
    "scroll",
    "scrolltolower",
    "scrolltoupper",
    "refresherpulling",
    "refresherrefresh",
    "refresherrestore",
    "refresherabort"
  ],
  methods: {
    updateOffsetFromChild(top) {
      this.offset = top;
    },
    onScroll(e) {
      let scrollTop = 0;
      scrollTop = e.detail.scrollTop;
      this.innerScrollTop = scrollTop;
      this.$emit("scroll", scrollTop);
    },
    scrollIntoViewById(id) {
    },
    // 滚动到底部触发事件
    scrolltolower(e) {
      common_vendor.sleep(30).then(() => {
        this.$emit("scrolltolower");
      });
    },
    // 滚动到底部时触发，非nvue有效
    scrolltoupper(e) {
      common_vendor.sleep(30).then(() => {
        this.$emit("scrolltoupper");
        this.offset = 0;
      });
    },
    refresherpulling(e) {
      this.$emit("refresherpulling", e);
    },
    refresherrefresh(e) {
      this.$emit("refresherrefresh", e);
    },
    refresherrestore(e) {
      this.$emit("refresherrestore", e);
    },
    refresherabort(e) {
      this.$emit("refresherabort", e);
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: _ctx.scrollIntoView,
    b: common_vendor.s($options.listStyle),
    c: _ctx.scrollable,
    d: Number(_ctx.scrollTop),
    e: Number(_ctx.lowerThreshold),
    f: Number(_ctx.upperThreshold),
    g: _ctx.showScrollbar,
    h: _ctx.enableBackToTop,
    i: _ctx.scrollWithAnimation,
    j: common_vendor.o((...args) => $options.onScroll && $options.onScroll(...args)),
    k: common_vendor.o((...args) => $options.scrolltolower && $options.scrolltolower(...args)),
    l: common_vendor.o((...args) => $options.scrolltoupper && $options.scrolltoupper(...args)),
    m: _ctx.refresherEnabled,
    n: _ctx.refresherThreshold,
    o: _ctx.refresherDefaultStyle,
    p: _ctx.refresherBackground,
    q: _ctx.refresherTriggered,
    r: common_vendor.o((...args) => $options.refresherpulling && $options.refresherpulling(...args)),
    s: common_vendor.o((...args) => $options.refresherrefresh && $options.refresherrefresh(...args)),
    t: common_vendor.o((...args) => $options.refresherrestore && $options.refresherrestore(...args)),
    v: common_vendor.o((...args) => $options.refresherabort && $options.refresherabort(...args))
  };
}
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-9ad03670"]]);
wx.createComponent(Component);
