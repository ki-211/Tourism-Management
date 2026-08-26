export interface ApiEnvelope<T>{code:string;message:string;data:T;requestId:string} export interface PageData<T>{items:T[];page:number;size:number;total:number}
export interface User{id:number;username:string;nickname:string;role:string}
export interface Activity{id:number;title:string;description?:string;location:string;startTime:string;endTime:string;signupStart:string;signupEnd:string;visibility:'PUBLIC'|'INVITE_ONLY';invitationCode?:string;feeRule?:string;creatorId:number;creatorName:string;coverUrl?:string;joined:boolean;creator:boolean;createdAt:string}
export interface Member{userId:number;username:string;nickname:string;grade?:string;passengerCount?:number;remark?:string;joinedAt:string}
export interface Message{id:number;userId:number;nickname:string;content:string;createdAt:string} export interface Photo{id:number;url:string;uploaderId:number;uploaderName:string;createdAt:string}
export interface Location{userId:number;nickname:string;latitude:number;longitude:number;address?:string;updatedAt:string;expiresAt:string}
export interface SignTask{id:number;activityId:number;title:string;description?:string;createdAt:string;signedCount:number;signed:boolean}
export interface Vehicle{id:number;plateNumber:string;driverName:string;pickupTime:string;pickupLocation:string;createdAt:string}
