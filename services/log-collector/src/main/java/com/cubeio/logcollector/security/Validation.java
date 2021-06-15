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
