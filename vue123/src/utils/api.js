import axios from "axios";
axios.defaults.withCredentials = true;
//let baseUrl = "http://localhost:8082";
let baseUrl = `http://${window.location.hostname}:8082`;
export const getRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    console.log(jwtToken);
    return axios({
        method: "get",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            Authorization: jwtToken
        }
    })
}
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
export const postRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    return axios({
        method: "post",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            Authorization: jwtToken
        }
    })
}
export const putRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    return axios({
        method: "put",
        url: `${baseUrl}${url}`,
        data: params,
        headers: {
            Authorization: jwtToken
        }
    })
}
export const putAuthorityRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    return axios({
        method: "put",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            Authorization: jwtToken
        }
    })
}
export const deleteRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    console.log(jwtToken);
    return axios({
        method: "delete",
        url: `${baseUrl}${url}`,
        data: params, // 注意：delete 请求的 body 数据放在 data 字段中
        headers: {
            Authorization: jwtToken
        }
    })
}
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
export const getAnonymousRequest = (url, params) => {
    const jwtToken = localStorage.getItem('jwtToken');
    console.log(jwtToken);
    return axios({
        method: "get",
        url: `${baseUrl}${url}`,
        params: params,
        headers: {
            "Content-Type": "application/json"
        }
    })
}
