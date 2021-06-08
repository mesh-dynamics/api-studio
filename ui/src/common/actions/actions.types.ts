import { IStoreState } from "../reducers/state.types";


export type GetStateAction = () => IStoreState; 
export type IDispatch = any; //need to find proper type of dispatch function

export type ReturnActionType = ({type: string, data?: any});
export type DispatchActionType = (dispatch: any, getState: ()=> IStoreState) => void;
export interface IActionsType {
    [key:string] : (arg1?: any, arg2?: any, arg3?: any,arg4?: any, arg5?: any, arg6?: any, arg7?: any, arg8?: any) => 
    (DispatchActionType | ReturnActionType)
     
}
