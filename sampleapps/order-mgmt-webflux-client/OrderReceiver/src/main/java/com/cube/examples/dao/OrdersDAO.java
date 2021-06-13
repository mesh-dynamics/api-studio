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

package com.cube.examples.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.cube.examples.model.Order;
import com.cube.examples.model.Orders;

@Repository
public class OrdersDAO
{
    private static Orders list = new Orders();

    private static final Logger LOGGER = LogManager.getLogger(OrdersDAO.class);
    static 
    {
        list.getOrderList().add(new Order(1, 1, new Order.Customer("Lokesh", "Gupta", "xyz@gmail.com")));
        list.getOrderList().add(new Order(2, 2, new Order.Customer("Alex", "Kolenchiskey", "abc@gmail.com")));
        list.getOrderList().add(new Order(3, 3, new Order.Customer("David", "Kameron", "test@gmail.com")));
    }
    
    public Orders getAllOrders()
    {
        LOGGER.info("getAllOrders DAO call received and the size of the orders to be returned is: " + list.getOrderList().size());
        return list;
    }
    
    public void placeOrder(Order order) {
        list.getOrderList().add(order);
    }
}
