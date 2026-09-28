<template>
  <div class="page-background">
    <el-form ref="loginForm" v-bind:rules="rules" v-bind:model="loginData"
             class="loginContainer">
      <h3 style="display: flex; justify-content: center">系统登录</h3>
      <el-form-item label="用户名" prop="account">
        <el-input v-model="loginData.account" placeholder="请输入用户
名..."></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input @keydown.enter.native="submitLogin" v-model="loginData.password" placeholder="请输入密码..." >
        </el-input>
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <el-input @keydown.enter.native="submitLogin" v-model="loginData.code" placeholder="请输入验证码..."></el-input>
        <el-button type="text" @click="getCaptcha">获取验证码</el-button>
        <img :src="captchaImg" alt="验证码" v-if="captchaImg" />
      </el-form-item>
      <el-checkbox label="记住我" v-model="checked"></el-checkbox>
      <el-button type="primary" style="width: 100%; margin-top: 5px" v-on:click="submitLogin">登录
      </el-button>
      <el-button type="text" @click="goToRegister">没有账号？去注册</el-button>
    </el-form>
  </div>
</template>
<script>
import JSEncrypt from 'jsencrypt';
export default {
  name: "Login",
  data(){
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
      publicKey: "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJXjjN54zuYgR9Xl/VxQu63X9PgrCENf8C9j7WcyB/+f8cy3zQmIW3h0/auw1oKrcxeNz8rctaFsBiNI7BZlTuMCAwEAAQ==" // RSA公钥
    }
  },
  methods: {
    getCaptcha() {
      this.getCaptcha1('/captcha').then(resp => {
        if (resp.data.code === 200) {
          this.captchaImg = `${resp.data.data.captcherImg}`;
          this.loginData.userKey = resp.data.data.userKey;
        }else {
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
          // 使用RSA公钥加密密码
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
              //将userId存储在 localStorage 中
              localStorage.setItem("userId", resp.data.data.userId);
              // 将 JWT 存储在 localStorage 中
              localStorage.setItem("jwtToken", resp.data.data.jwt);
              // 保存 checked 状态到 localStorage
              localStorage.setItem("rememberMe", this.checked);

              // 获取登录用户的账户名、角色和权限
              this.getRequest("/sysUser/RoleAndAndAuthority", { userId: localStorage.getItem("userId") }).then(userResp => {
                if (userResp.data.code === 200) {
                  const userAccount = userResp.data.data.account;
                  const userRoles = userResp.data.data.role;
                  const userAuthorities = userResp.data.data.authority;
                  // 将账户名和角色信息和权限信息存储在localStorage 中
                  localStorage.setItem("userAccount", JSON.stringify(userAccount));
                  localStorage.setItem("userRoles", JSON.stringify(userRoles));
                  localStorage.setItem("userAuthorities", JSON.stringify(userAuthorities));

                  console.log(localStorage.getItem("userAccount"));
                  console.log(localStorage.getItem("userRoles"));
                  console.log(localStorage.getItem("userAuthorities"));

                  this.$router.replace("/user");
                  this.$message.success("欢迎"+userAccount+"登录!");
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
      this.$router.push("/register"); // 跳转到登录页面
    }
  }
}

</script>
<style scoped>

.page-background {
  display: flex;/*设置为弹性容器*/
  flex-direction: column;/*主轴设置为垂直方向*/
  justify-content: center;/*容器内元素在主轴上居中*/
  align-items: center;/*容器内元素在交叉轴(与主轴垂直的轴)上居中*/

  background-image: url("@/assets/444.jpg");
  background-size: 100% 100%;/*图片高宽100%填充容器*/
  background-attachment: fixed; /*固定背景*/


  /*width: 1420px;
  height: 700px;
  padding: 20px;*/
  .loginContainer{
    border-radius: 15px; /*设置外边框圆角*/
    margin: 100px auto; /*外边距*/
    width: 350px; /*宽度*/
    padding: 20px 20px 35px 20px; /*上右下左内边距*/
    //background: #fff; /*背景颜色为白色*/
    background: inherit; /*继承父容器背景*/
    border: 1px solid #eaeaea; /*边框粗细,实线,颜色*/
    //box-shadow: 0 0 25px #cac6c6; /* x 偏移量 | y 偏移量 | 阴影模糊半径 | 阴影颜色*/
    box-shadow: inset 0 0 0 3000px rgba(255, 255, 255, 0.87);/*box-shadow: rgba(0, 0, 0, 0.1) 0px 15px 30px;*/
  }
}
</style>
