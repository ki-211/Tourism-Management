export interface SessionUser{id:number;username:string;nickname:string;role:string}
const ACCESS='tourism.accessToken',REFRESH='tourism.refreshToken',USER='tourism.user'
export const accessToken=()=>uni.getStorageSync(ACCESS) as string||''
export const refreshToken=()=>uni.getStorageSync(REFRESH) as string||''
export const currentUser=():SessionUser|null=>uni.getStorageSync(USER)||null
export const saveSession=(d:{accessToken:string;refreshToken:string;user:SessionUser})=>{uni.setStorageSync(ACCESS,d.accessToken);uni.setStorageSync(REFRESH,d.refreshToken);uni.setStorageSync(USER,d.user)}
export const clearSession=()=>{uni.removeStorageSync(ACCESS);uni.removeStorageSync(REFRESH);uni.removeStorageSync(USER)}
export const hasSession=()=>Boolean(accessToken()&&refreshToken()&&currentUser())
export const requireSession=()=>{if(!hasSession()){uni.reLaunch({url:'/pages/login/login'});return false}return true}
