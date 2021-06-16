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

import org.springframework.stereotype.Repository;

import com.cube.examples.model.EnhancedOrder;
import com.cube.examples.model.Order;
import com.cube.examples.model.Product;
import com.cube.examples.model.Products;

@Repository
public class OrdersDAO
{
    private static Products products = new Products();

    static
    {
        products.addProduct(new Product(1, "Phone", 1000));
        products.addProduct(new Product(2, "Laptop", 2000));
        products.addProduct(new Product(3, "Watch", 500));
        products.addProduct(new Product(4, "Car", 500000));
        products.addProduct(new Product(5, "Bike", 50000));
    }
    public EnhancedOrder enhanceOrder(Order order) {
        return new EnhancedOrder(order.getId(), products.getProductById(order.getProductId()), order.getCustomer());
    }
}
