import Vue from 'vue'
import App from './App.vue'
import router from "@/router";

Vue.config.productionTip = false

import {
  getAnonymousRequest,
  getCaptcha1,
  getRequest,
  postAnonymousRequest,
  postRequest,
  putRequest,
  deleteRequest,
  putAuthorityRequest
} from "@/utils/api";
Vue.prototype.getRequest = getRequest;
Vue.prototype.postRequest = postRequest;
Vue.prototype.deleteRequest = deleteRequest;
Vue.prototype.getAnonymousRequest = getAnonymousRequest;
Vue.prototype.postAnonymousRequest = postAnonymousRequest;
Vue.prototype.getCaptcha1 = getCaptcha1;
Vue.prototype.putRequest =putRequest;
Vue.prototype.putAuthorityRequest = putAuthorityRequest;


import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
Vue.use(ElementUI);

//若未勾选“记住我”进行登录，则在页面卸载/刷新后删除jwt
window.addEventListener('beforeunload', () => {
  const checked = localStorage.getItem('rememberMe');
  if (checked !== 'true') {
    localStorage.removeItem('jwtToken');
  }
});

new Vue({
  render: h => h(App),
  router,
  created() {
    // 在页面加载时读取 JWT
    this.jwtToken = localStorage.getItem('jwtToken');
  }
}).$mount('#app')
