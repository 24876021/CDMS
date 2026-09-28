<template>
  <div class="page-background">
    <el-form ref="loginForm" :rules="rules" :model="loginData" class="loginContainer">
      <h3 style="display: flex; justify-content: center">系统登录</h3>
      <el-form-item label="用户名" prop="account">
        <el-input v-model="loginData.account" placeholder="请输入用户名..."></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
            v-model="loginData.password"
            placeholder="请输入密码..."
            show-password
            v-password-tooltip
            @keydown.enter.native="submitLogin"
        ></el-input>
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <el-input
            v-model="loginData.code"
            placeholder="请输入验证码..."
            @keydown.enter.native="submitLogin"
        ></el-input>
        <el-button type="text" @click="getCaptcha">获取验证码</el-button>
        <img :src="captchaImg" alt="验证码" v-if="captchaImg" />
      </el-form-item>
      <el-checkbox label="记住我" v-model="checked"></el-checkbox>
      <el-button type="primary" style="width: 100%; margin-top: 5px" @click="submitLogin">登录</el-button>
      <el-button type="text" @click="goToRegister">没有账号？去注册</el-button>
    </el-form>
  </div>
</template>

<script>
import JSEncrypt from 'jsencrypt';
export default {
  name: "Login",
  directives: {
    'password-tooltip': {
      inserted(el) {
        // 1. 找到密码输入框的原生input元素和眼睛图标
        const inputEl = el.querySelector('input');
        const iconEl = el.querySelector('.el-input__icon');

        if (!inputEl || !iconEl) {
          // 兜底：监听DOM变化，确保找到元素
          const observer = new MutationObserver(() => {
            const newInput = el.querySelector('input');
            const newIcon = el.querySelector('.el-input__icon');
            if (newInput && newIcon) {
              bindTooltip(newInput, newIcon);
              observer.disconnect();
            }
          });
          observer.observe(el, { childList: true, subtree: true });
        } else {
          bindTooltip(inputEl, iconEl);
        }

        // 核心：绑定提示文字逻辑
        function bindTooltip(input, icon) {
          // 初始化提示文字
          updateTooltip();

          // 监听input的type属性变化（Element UI切换显示/隐藏时会改这个）
          const inputObserver = new MutationObserver(() => {
            updateTooltip();
          });
          inputObserver.observe(input, { attributes: true, attributeFilter: ['type'] });

          // 监听图标的鼠标移入事件（确保hover时实时更新）
          icon.addEventListener('mouseenter', updateTooltip);

          // 更新提示文字的核心函数
          function updateTooltip() {
            // 判断依据：input的type是password → 密码隐藏；是text → 密码显示
            const isHidden = input.type === 'password';
            icon.title = isHidden ? '显示密码' : '隐藏密码';
            icon.style.cursor = 'pointer';
            icon.style.zIndex = 999;
          }
        }
      }
    }
  },
  data() {
    return {
      rules: {
        account: [{ required: true, message: "请输入用户名", trigger: 'blur' }],
        password: [{ required: true, message: "请输入密码", trigger: 'blur' }],
        code: [{ required: true, message: "请输入验证码", trigger: 'blur' }]
      },
      loginData: {
        account: "",
        password: "",
        code: "",
        userKey: ""
      },
      checked: false,
      captchaImg: "",
      publicKey: "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJXjjN54zuYgR9Xl/VxQu63X9PgrCENf8C9j7WcyB/+f8cy3zQmIW3h0/auw1oKrcxeNz8rctaFsBiNI7BZlTuMCAwEAAQ=="
    }
  },
  methods: {
    getCaptcha() {
      this.getCaptcha1('/captcha').then(resp => {
        if (resp.data.code === 200) {
          this.captchaImg = `${resp.data.data.captcherImg}`;
          this.loginData.userKey = resp.data.data.userKey;
        } else {
          this.$message.error(resp.data.msg);
        }
      }).catch(error => {
        console.error('无响应:', error);
        this.$message.error('获取验证码失败');
      });
    },
    submitLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          const encrypt = new JSEncrypt();
          encrypt.setPublicKey(this.publicKey);
          const encryptedPassword = encrypt.encrypt(this.loginData.password);

          const loginRequest = {
            account: this.loginData.account,
            password: encryptedPassword,
            code: this.loginData.code,
            userKey: this.loginData.userKey
          };

          this.postAnonymousRequest("/login", loginRequest).then(resp => {
            if (resp.data.code === 400) {
              this.$message.error(resp.data.data);
            } else {
              localStorage.setItem("userId", resp.data.data.userId);
              localStorage.setItem("jwtToken", resp.data.data.jwt);
              localStorage.setItem("rememberMe", this.checked);

              this.getRequest("/sysUser/RoleAndAndAuthority", { userId: localStorage.getItem("userId") }).then(userResp => {
                if (userResp.data.code === 200) {
                  const userAccount = userResp.data.data.account;
                  const userRoles = userResp.data.data.role;
                  const userAuthorities = userResp.data.data.authority;

                  localStorage.setItem("userAccount", JSON.stringify(userAccount));
                  localStorage.setItem("userRoles", JSON.stringify(userRoles));
                  localStorage.setItem("userAuthorities", JSON.stringify(userAuthorities));

                  this.$router.replace("/user");
                  this.$message.success("欢迎" + userAccount + "登录!");
                } else {
                  this.$message.error("获取用户角色和权限失败");
                }
              }).catch(error => {
                console.error('获取用户角色和权限失败:', error);
                this.$message.error('获取用户角色和权限失败');
              });
            }
          });
        } else {
          this.$message.error('请填写所有信息');
          return false;
        }
      });
    },
    goToRegister() {
      this.$router.push("/register");
    }
  }
}
</script>

<style scoped>
.page-background {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-image: url("@/assets/444.jpg");
  background-size: 100% 100%;
  background-attachment: fixed;

  .loginContainer {
    border-radius: 15px;
    margin: 100px auto;
    width: 350px;
    padding: 20px 20px 35px 20px;
    background: inherit;
    border: 1px solid #eaeaea;
    box-shadow: inset 0 0 0 3000px rgba(255, 255, 255, 0.87);
  }
}

::v-deep .el-input--show-password .el-input__icon {
  cursor: pointer !important;
  z-index: 999 !important;
  pointer-events: auto !important;
}
</style>