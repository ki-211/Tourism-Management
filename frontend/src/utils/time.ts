export const displayTime=(v?:string)=>v?String(v).replace('T',' ').slice(0,16):'--'
export const apiTime=(v:string)=>v.length===16?v+':00':v
export const isSignupOpen=(start:string,end:string,now=new Date())=>now>=new Date(start)&&now<=new Date(end)
