import { isEmail, isEmpty, isAlpha, isLength, isAlphanumeric } from 'validator';

const validateFirstName = (value) => {

    const fieldStatus = {
        isValid: true,
        errorMessages: []
    };

    if(isEmpty(value)){
        
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Firstname is required");

        return fieldStatus;
    }

    if(!isLength(value, { min: 3})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Minimum 3 characters required")

        return fieldStatus;
    }

    if(!isAlpha(value)){
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Firstname must not contain numbers")

        return fieldStatus;
    }

    return fieldStatus;
};

const validateLastName = (value) => {
    const fieldStatus = {
        isValid: true,
        errorMessages: []
    };

    if(!isEmpty(value) && !isLength(value, { min: 3})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Minimum 3 characters required")

        return fieldStatus;
    }

    if(!isEmpty(value) && !isAlpha(value)){
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Lastname must not contain numbers")

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

    if(!isLength(value, { min: 6})) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Minimum 6 characters required")

        return fieldStatus;
    }

    if(!isAlphanumeric(value)) {
        fieldStatus.isValid =  false;
        fieldStatus.errorMessages.push("Password must contain letters and numbers")

        return fieldStatus;
    }

    return fieldStatus;
};

export {
    validateFirstName,
    validateLastName,
    validateEmail,
    validatePassword
};