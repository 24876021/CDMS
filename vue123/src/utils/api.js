import axios from "axios";
axios.defaults.withCredentials = true;
let baseUrl = `http://${window.location.hostname}:8082`;

// 优先从sessionStorage获取（未勾选记住我），其次从localStorage获取（勾选记住我）
function getToken() {
    return sessionStorage.getItem('jwtToken') || localStorage.getItem('jwtToken');
}

export function getUserId() {
    return sessionStorage.getItem('userId') || localStorage.getItem('userId');
}

// 通用GET请求
export const getRequest = (url, params) => {
    const jwtToken = getToken();
    //console.log(jwtToken);
    return axios({
        method: "get",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            Authorization: jwtToken
        }
    })
}

// 获取验证码请求（无token）
export const getCaptcha1 = (url, params) => {
    return axios({
        method: "get",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            "Content-Type": "application/json"
        }
    })
}

// 通用POST请求
export const postRequest = (url, params) => {
    const jwtToken = getToken();
    return axios({
        method: "post",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            Authorization: jwtToken
        }
    })
}

// 通用PUT请求
export const putRequest = (url, params) => {
    const jwtToken = getToken();
    return axios({
        method: "put",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            Authorization: jwtToken
        }
    })
}

// 权限修改专用PUT请求
export const putAuthorityRequest = (url, params) => {
    const jwtToken = getToken();
    return axios({
        method: "put",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            Authorization: jwtToken
        }
    })
}

// 通用DELETE请求
export const deleteRequest = (url, params) => {
    const jwtToken = getToken();
    return axios({
        method: "delete",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            Authorization: jwtToken
        }
    })
}

// 匿名POST请求（登录/注册使用）
export const postAnonymousRequest = (url, params) => {
    return axios({
        method: "post",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            "Content-Type": "application/json"
        }
    });
};

// 匿名GET请求
export const getAnonymousRequest = (url, params) => {
    return axios({
        method: "get",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            "Content-Type": "application/json"
        }
    })
}