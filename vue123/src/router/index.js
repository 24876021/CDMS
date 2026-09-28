import Vue from "vue";
import VueRouter from "vue-router";
Vue.use(VueRouter);
const router = new VueRouter({
    routes:[
        {
            path: "/",
            component: ()=>import("@/views/Login.vue")
        },
        {
            path: "/register",
            component: ()=>import("@/views/Register.vue")
        },
        {
            path: "/user",
            component: ()=>import("@/views/user.vue")
        }
    ]


});
export default router;