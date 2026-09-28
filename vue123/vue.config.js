const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  //本地测试可以不要下面的代码块，不用的话默认只有本机能访问且默认使用8080端口
  devServer: {
    host: '0.0.0.0', // 允许局域网所有设备访问，有些电脑要先在防火墙添加一条入站规则开放运行端口允许专用网络访问才行
    allowedHosts: 'all', //允许所有主机访问（解决Network unavailable）
    port: 8081,      // 自定义运行端口
    open: false       // 可选：启动后自动打开浏览器
  }
})
