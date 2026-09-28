<template>
  <div class="page-background">
    <el-form ref="loginForm" :rules="rules" :model="loginData" class="loginContainer">
      <h3 style="display: flex; justify-content: center">系统登录</h3>
      <el-form-item label="账户名" prop="account">
        <el-input v-model="loginData.account" placeholder="请输入账户名..." ></el-input>
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
      </el-form-item>
      <div class="captcha-box">
        <el-button type="text" @click="getCaptcha">获取验证码</el-button>
        <img :src="captchaImg" alt="验证码" v-if="captchaImg" class="captcha-img"/>
      </div>
      <el-checkbox label="记住我" v-model="checked"></el-checkbox>
      <!-- 移除禁用逻辑，正常显示登录按钮 -->
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
        // 持续监听，防止DOM重建后失效
        const observer = new MutationObserver(() => {
          const input = el.querySelector('input');
          const icon = el.querySelector('.el-input__icon');
          if (input && icon) {
            bindTooltip(input, icon);
          }
        });

        observer.observe(el, {
          childList: true,
          subtree: true,
          attributes: true
        });

        function bindTooltip(input, icon) {
          // 避免重复绑定
          if (icon._tooltipBound) return;
          icon._tooltipBound = true;

          updateTooltip();

          // 监听输入框类型变化
          const inputObs = new MutationObserver(updateTooltip);
          inputObs.observe(input, { attributes: true, attributeFilter: ['type'] });

          icon.addEventListener('mouseenter', updateTooltip);
          icon.style.cursor = 'pointer';
          icon.style.zIndex = 999;

          function updateTooltip() {
            const isHidden = input.type === 'password';
            icon.title = isHidden ? '显示密码' : '隐藏密码';
          }
        }
      }
    }
  },
  data() {
    return {
      rules: {
        account: [{ required: true, message: "请输入账户名", trigger: 'blur' }],
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
  // 页面创建时自动检查 JWT
  created() {
    this.checkJwtAndRedirect();
  },
  methods: {
    // 检查 会话存储/本地存储 有无token，有就直接跳转
    checkJwtAndRedirect() {
      const token = sessionStorage.getItem("jwtToken") || localStorage.getItem("jwtToken");
      const userId = sessionStorage.getItem("userId") || localStorage.getItem("userId");

      // 如果同时存在 token 和 userId，说明已登录
      if (token && userId) {
        this.$router.replace("/user");
      }
    },
    getCaptcha() {
      this.getCaptcha1('/captcha').then(resp => {
        if (resp.data.code === 200) {
          this.captchaImg = `${resp.data.data.captcherImg}`;
          this.loginData.userKey = resp.data.data.userKey;
        } else {
          this.$message.error(resp.data.data);
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
              this.passwordErrorCount = 0;
              const token = resp.data.data.jwt;
              const userId = resp.data.data.userId;
              const rememberMe = this.checked;

              // ====================== 核心修复：区分存储位置 ======================
              // 勾选记住我 → localStorage（持久化，关闭浏览器不丢失）
              // 不勾选记住我 → sessionStorage（刷新不丢失，关闭浏览器清空）
              if (rememberMe) {
                localStorage.setItem("userId", userId);
                localStorage.setItem("jwtToken", token);
                localStorage.setItem("rememberMe", "true");
              } else {
                sessionStorage.setItem("userId", userId);
                sessionStorage.setItem("jwtToken", token);
                localStorage.setItem("rememberMe", "false");
              }

              // 获取角色权限并存储
              this.getRequest("/sysUser/RoleAndAndAuthority", { userId: userId }).then(userResp => {
                if (userResp.data.code === 200) {
                  const data = userResp.data.data;
                  // 根据记住我状态选择存储位置
                  const storage = rememberMe ? localStorage : sessionStorage;

                  storage.setItem("userAccount", JSON.stringify(data.account));
                  storage.setItem("userRoles", JSON.stringify(data.role));
                  storage.setItem("userAuthorities", JSON.stringify(data.authority));

                  this.$router.replace("/user");
                  this.$message.success("欢迎" + data.account + "登录!");
                } else {
                  this.$message.error("获取用户角色和权限失败");
                }
              }).catch(error => {
                console.error('获取用户角色和权限失败:', error);
                this.$message.error('获取用户角色和权限失败');
              });
            }
          }).catch(err => {
            this.$message.error('网络异常，请重试');
            console.error(err);
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

  /*容器大小*/
  width: 99vw;
  height: 99vh;
  position: fixed;

  background-image: url("@/assets/444.jpg");
  background-size: 100% 100%;
  background-attachment: fixed;

  .loginContainer {
    border-radius: 15px;
    margin: 100px auto;
    width: 350px;
    padding: 20px 50px 35px 50px;
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

/* 验证码容器样式 */
::v-deep .captcha-box {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;  /* 左右分开 */
  align-items: center;
  width: 350px;     /* 和输入框一样宽 */

}
.captcha-img {
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
}
</style>