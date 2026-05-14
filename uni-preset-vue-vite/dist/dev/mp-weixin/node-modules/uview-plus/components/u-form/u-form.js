"use strict";
const common_vendor = require("../../../../common/vendor.js");
common_vendor.Schema.warning = function() {
};
const _sfc_main = {
  name: "u-form",
  mixins: [common_vendor.mpMixin, common_vendor.mixin, common_vendor.props$5],
  provide() {
    return {
      uForm: this
    };
  },
  data() {
    return {
      formRules: {},
      // 规则校验器
      validator: {},
      // 原始的model快照，用于resetFields方法重置表单时使用
      originalModel: null
    };
  },
  watch: {
    // 监听规则的变化
    rules: {
      immediate: true,
      handler(n) {
        this.setRules(n);
      }
    },
    // 监听属性的变化，通知子组件u-form-item重新获取信息
    propsChange(n) {
      var _a;
      if ((_a = this.children) == null ? void 0 : _a.length) {
        this.children.map((child) => {
          typeof child.updateParentData == "function" && child.updateParentData();
        });
      }
    },
    // 监听model的初始值作为重置表单的快照
    model: {
      immediate: true,
      handler(n) {
        if (!this.originalModel) {
          this.originalModel = common_vendor.deepClone(n);
        }
      }
    }
  },
  computed: {
    propsChange() {
      return [
        this.errorType,
        this.borderBottom,
        this.labelPosition,
        this.labelWidth,
        this.labelAlign,
        this.labelStyle
      ];
    }
  },
  created() {
    this.children = [];
  },
  methods: {
    // 手动设置校验的规则，如果规则中有函数的话，微信小程序中会过滤掉，所以只能手动调用设置规则
    setRules(rules) {
      if (Object.keys(rules).length === 0)
        return;
      if (Object.keys(this.model).length === 0) {
        common_vendor.error("设置rules，model必须设置！如果已经设置，请刷新页面。");
        return;
      }
      this.formRules = rules;
      this.validator = new common_vendor.Schema(rules);
    },
    // 清空所有u-form-item组件的内容，本质上是调用了u-form-item组件中的resetField()方法
    resetFields() {
      this.resetModel();
    },
    // 重置model为初始值的快照
    resetModel(obj) {
      this.children.map((child) => {
        const prop = child == null ? void 0 : child.prop;
        const value = common_vendor.getProperty(this.originalModel, prop);
        common_vendor.setProperty(this.model, prop, value);
      });
    },
    // 清空校验结果
    clearValidate(props2) {
      props2 = [].concat(props2);
      this.children.map((child) => {
        if (props2[0] === void 0 || props2.includes(child.prop)) {
          child.message = null;
        }
      });
    },
    // 对部分表单字段进行校验
    async validateField(value, callback, event = null, options) {
      this.$nextTick(() => {
        const errorsRes = [];
        value = [].concat(value);
        let promises = this.children.map((child) => {
          return new Promise((resolve, reject) => {
            const childErrors = [];
            if (value.includes(child.prop)) {
              const propertyVal = common_vendor.getProperty(
                this.model,
                child.prop
              );
              const propertyChain = child.prop.split(".");
              const propertyName = propertyChain[propertyChain.length - 1];
              let rule = [];
              if (child.itemRules && child.itemRules.length > 0) {
                rule = child.itemRules;
              } else {
                rule = this.formRules[child.prop];
              }
              if (!rule) {
                resolve();
                return;
              }
              const rules = [].concat(rule);
              if (!rules.length) {
                resolve();
              }
              for (let i = 0; i < rules.length; i++) {
                const ruleItem = rules[i];
                const trigger = [].concat(ruleItem == null ? void 0 : ruleItem.trigger);
                if (event && !trigger.includes(event)) {
                  resolve();
                  continue;
                }
                const validator = new common_vendor.Schema({
                  [propertyName]: ruleItem
                });
                validator.validate(
                  {
                    [propertyName]: propertyVal
                  },
                  (errors, fields) => {
                    var _a;
                    if (common_vendor.test.array(errors)) {
                      errors.forEach((element) => {
                        element.prop = child.prop;
                      });
                      errorsRes.push(...errors);
                      childErrors.push(...errors);
                    }
                    if (!options || (options == null ? void 0 : options.showErrorMsg) == true) {
                      child.message = ((_a = childErrors[0]) == null ? void 0 : _a.message) ? childErrors[0].message : null;
                    }
                    if (i == rules.length - 1) {
                      resolve(errorsRes);
                    }
                  }
                );
              }
            } else {
              resolve({});
            }
          });
        });
        Promise.all(promises).then((results) => {
          typeof callback === "function" && callback(errorsRes);
        }).catch((error2) => {
          console.error("An error occurred:", error2);
        });
      });
    },
    /**
     * 校验全部数据
     * @param {Object} options
     * @param {Boolean} options.showErrorMsg -是否显示校验信息，
     */
    validate(options) {
      if (Object.keys(this.formRules).length === 0) {
        common_vendor.error("未设置rules，请看文档说明！如果已经设置，请刷新页面。");
        return;
      }
      return new Promise((resolve, reject) => {
        this.$nextTick(() => {
          const formItemProps = this.children.map(
            (item) => item.prop
          );
          this.validateField(formItemProps, (errors) => {
            if (errors.length) {
              this.errorType === "toast" && common_vendor.toast(errors[0].message);
              reject(errors);
            } else {
              resolve(true);
            }
          }, null, options);
        });
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {};
}
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createComponent(Component);
