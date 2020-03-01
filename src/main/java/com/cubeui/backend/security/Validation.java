package com.cubeui.backend.security;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.web.exception.CustomerIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class Validation {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    //validates the customerId from URI and token
    public void validateCustomerName(HttpServletRequest request, String customerId) {
        final Customer customer = jwtTokenProvider.getCustomer(request);
        if(!customerId.equalsIgnoreCase(customer.getName())) {
            log.error("Invalid Customer Id");
            throw new CustomerIdException("CustomerId=" + customerId + " not matching to Token customerId=" + customer.getName());
        }
    }
}
