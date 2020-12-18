package com.cubeui.backend.security;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.domain.enums.Role;
import com.cubeui.backend.web.exception.CustomerIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Validation {

    //validates the customerId from URI and token
    public void validateCustomerName(Authentication authentication, String customerId) {
        final User user = (User) authentication.getPrincipal();
        final Customer customer = user.getCustomer();
        if(customerId == null  || (!user.getRoles().contains(Role.ROLE_ADMIN.toString()) && !customerId.equalsIgnoreCase(customer.getName()))) {
            log.error("Invalid Customer Id");
            throw new CustomerIdException("CustomerId=" + customerId + " not matching to Token customerId=" + customer.getName());
        }
    }
}
