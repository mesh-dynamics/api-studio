/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { IStoreState } from "../reducers/state.types";


export type GetStateAction = () => IStoreState; 
export type IDispatch = any; //need to find proper type of dispatch function

export type ReturnActionType = ({type: string, data?: any});
export type DispatchActionType = (dispatch: any, getState: ()=> IStoreState) => void;
export interface IActionsType {
    [key:string] : (arg1?: any, arg2?: any, arg3?: any,arg4?: any, arg5?: any, arg6?: any, arg7?: any, arg8?: any) => 
    (DispatchActionType | ReturnActionType)
     
}
