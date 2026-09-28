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
      <el-button type="primary" style="width: 100%; margin-top: 5px" @click="submitLogin" :disabled="passwordErrorCount >= 5">{{ passwordErrorCount >= 5 ? '账号已锁定' : '登录' }}</el-button>
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
      // 密码错误次数
      passwordErrorCount: 0,
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
    // 检查 localStorage 有无 token，有就直接跳转
    checkJwtAndRedirect() {
      const token = localStorage.getItem("jwtToken");
      const userId = localStorage.getItem("userId");

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
              const msg = resp.data.data;

              // ============== 修复：验证码错误不计数，只有密码错误才累计 ==============
              if (msg.includes("验证码")) {
                // 验证码错误 → 不计数，直接提示
                this.$message.error(msg);
              }
              // 密码/账号错误 → 才计数
              else if (msg.includes("密码") || msg.includes("账号")) {
                this.passwordErrorCount++;
                this.$message.error(`密码错误 ${this.passwordErrorCount}/5 次`);

                // 达到5次 → 锁定
                if (this.passwordErrorCount >= 5) {
                  this.postAnonymousRequest("/sysUser/lockUserByErrorPwd", {
                    account: this.loginData.account
                  }).then(() => {
                    this.$message.error("连续输错5次密码，账号已自动锁定！");
                  });
                }
              }
              else {
                this.$message.error(msg);
              }

            } else {
              // 登录成功 → 清空错误次数
              this.passwordErrorCount = 0;

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