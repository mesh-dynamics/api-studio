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

/**
 * This file contains field validations for inputs used across the application
 */
import { isEmail, isEmpty, isAlpha, isLength, isNumeric } from 'validator';
const containsSpecialChar = (value)=>{
    var format = /[`!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~]/;
    return format.test(value);
}

const validateName = (value, nameType) => {

    const fieldStatus = {
        isValid: true,
        errorMessages: []
    };

    if(isEmpty(value)){
        
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push(`${nameType || "Name"} is required`);

        return fieldStatus;
    }

    if(!isLength(value, { min: 2})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Minimum 2 characters required")

        return fieldStatus;
    }

    if(!isLength(value, { max: 50})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Maximum limit is 50 characters")

        return fieldStatus;
    }

    if(!isAlpha(value)){
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push(`${nameType} must not contain numbers or special characters`)

        return fieldStatus;
    }

    return fieldStatus;
};

const validateEmail = (value) => {
    const fieldStatus = {
        isValid: true,
        errorMessages: []
    };

    if(isEmpty(value)){
        
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Email is required");

        return fieldStatus;
    }

    if(!isEmail(value)) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Email is invalid")

        return fieldStatus;
    }

    return fieldStatus;

};

const validatePassword = (value) => {
    const fieldStatus = {
        isValid: true,
        errorMessages: []
    };

    if(isEmpty(value)){
        
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Password is required");

        return fieldStatus;
    }

    if(!isLength(value, { min: 7})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Minimum 7 characters required")

        return fieldStatus;
    }

    if(!(!isAlpha(value) && !isNumeric(value) && containsSpecialChar(value))) {

        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Password must contain letters, numbers and special characters")

        return fieldStatus;
    }

    return fieldStatus;
};

export {
    validateName,
    validateEmail,
    validatePassword
};