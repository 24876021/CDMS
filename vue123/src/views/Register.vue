<template>
  <div class="page-background">
    <el-form ref="registerForm" v-bind:rules="rules" v-bind:model="registerData"
             class="registerContainer">
      <h3 style="display: flex; justify-content: center">系统注册</h3>
      <el-form-item label="昵称" prop="name">
        <el-input v-model="registerData.name" placeholder="请输入昵称..."></el-input>
      </el-form-item>
      <el-form-item label="账户名" prop="account">
        <el-input v-model="registerData.account" placeholder="请输入账户名..."></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input type="password" v-model="registerData.password" placeholder="请输入密码..."></el-input>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input @keydown.enter.native="submitRegister" type="password" v-model="registerData.confirmPassword" placeholder="请确认密码..."></el-input>
      </el-form-item>
      <el-button type="primary" style="width: 100%; margin-top: 5px" v-on:click="submitRegister">注册
      </el-button>
      <el-button type="text" @click="goToLogin">已有账号？去登录</el-button>
    </el-form>
  </div>
</template>

<script>
import JSEncrypt from 'jsencrypt';
import axios from "axios";

export default {
  name: "Register",
  data() {
    return {
      rules: {
        account: [{ required: true, message: "请输入账户", trigger: 'blur' },
                  { type: 'string', pattern: /^\d{10}$/, message: "账户名必须是10位数字", trigger: 'blur' }
        ],
        password: [{ required: true, message: "请输入密码", trigger: 'blur' },
          { min: 8, max: 12, message: "密码长度必须在8到12位之间", trigger: 'blur' },
          { type: 'string', pattern: /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]*$/, message: "密码必须包含至少一个数字和一个字母", trigger: 'blur' }
        ],
        name: [{ required: true, message: "请输入昵称", trigger: 'blur' }],
        confirmPassword: [{ required: true, message: "请确认密码", trigger: 'blur' }]
      },
      registerData: {
        account: "",
        name:"",
        password: "",
        confirmPassword: ""
      },
      publicKey: "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJXjjN54zuYgR9Xl/VxQu63X9PgrCENf8C9j7WcyB/+f8cy3zQmIW3h0/auw1oKrcxeNz8rctaFsBiNI7BZlTuMCAwEAAQ==" // RSA公钥
    }
  },
  methods: {
    submitRegister() {
      this.$refs.registerForm.validate((valid) => {
        if (valid) {
          if (this.registerData.password !== this.registerData.confirmPassword) {
            this.$message.error('两次输入的密码不一致');
            return false;
          }

          // 使用RSA公钥加密密码
          const encrypt = new JSEncrypt();
          encrypt.setPublicKey(this.publicKey);
          const encryptedPassword = encrypt.encrypt(this.registerData.password);

          const registerRequest = {
            account: this.registerData.account,
            name: this.registerData.name,
            password: encryptedPassword
          };

          axios.post('http://localhost:8082/sysUser/register', registerRequest).then(resp => {
            if (resp.data.code === 200) {
              this.$message.success("注册成功!");
              this.$router.push("/"); // 跳转到登录页面
            } else {
              this.$message.error(resp.data.data);
            }
          }).catch(error => {
            console.error('无响应:', error);
            this.$message.error('注册失败');
          });
        } else {
          this.$message.error('请填入所有信息');
          return false;
        }
      });
    },
    goToLogin() {
      this.$router.push("/"); // 跳转到登录页面
    }
  }
}
</script>

<style scoped>

.page-background {
  display: flex; /*设置为弹性容器*/
  flex-direction: column; /*主轴设置为垂直方向*/
  justify-content: center; /*容器内元素在主轴上居中*/
  align-items: center; /*容器内元素在交叉轴(与主轴垂直的轴)上居中*/

  background-image: url("@/assets/444.jpg");
  background-size: 100% 100%; /*图片高宽100%填充容器*/
  background-attachment: fixed; /*固定背景*/

  .registerContainer {
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