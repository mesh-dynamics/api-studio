package com.cubeio.logcollector.security;

import com.cubeio.logcollector.domain.Customer;
import com.cubeio.logcollector.domain.User;
import com.cubeio.logcollector.domain.exception.CustomerIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Validation {

    //validates the customerId from URI and token
    public void validateCustomerName(Authentication authentication, String customerId) {
        //if(true) return;
        final User user = (User) authentication.getPrincipal();
        final Customer customer = user.getCustomer();
        if(customerId == null || !customerId.equalsIgnoreCase(customer.getName())) {
            log.error("Invalid Customer Id "+customerId + " for customer "+customer.getName());
            throw new CustomerIdException("CustomerId=" + customerId + " not matching to Token customerId=" + customer.getName());
        }
    }
}
