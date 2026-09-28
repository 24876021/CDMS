import Vue from 'vue'
import App from './App.vue'
import router from "@/router";

Vue.config.productionTip = false

import {
  getAnonymousRequest,
  getUserId,
  getCaptcha1,
  getRequest,
  postAnonymousRequest,
  postRequest,
  putRequest,
  deleteRequest,
  putAuthorityRequest,
} from "@/utils/api";
Vue.prototype.getRequest = getRequest;
Vue.prototype.postRequest = postRequest;
Vue.prototype.deleteRequest = deleteRequest;
Vue.prototype.getAnonymousRequest = getAnonymousRequest;
Vue.prototype.postAnonymousRequest = postAnonymousRequest;
Vue.prototype.getUserId = getUserId;
Vue.prototype.getCaptcha1 = getCaptcha1;
Vue.prototype.putRequest =putRequest;
Vue.prototype.putAuthorityRequest = putAuthorityRequest;


import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
Vue.use(ElementUI);

new Vue({
  render: h => h(App),
  router,
  created() {
    // 页面加载时读取JWT（优先会话存储，再本地存储）
    this.jwtToken = sessionStorage.getItem('jwtToken') || localStorage.getItem('jwtToken');
  }
}).$mount('#app')