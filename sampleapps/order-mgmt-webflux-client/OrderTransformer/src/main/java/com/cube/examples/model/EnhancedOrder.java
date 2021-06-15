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

package com.cube.examples.model;

import com.cube.examples.model.Order.Customer;

public class EnhancedOrder {

  private Integer id;
  private Product product;
  private Order.Customer customer;

  public EnhancedOrder() {

  }

  public EnhancedOrder(Integer id, Product product, Order.Customer customer) {
    super();
    this.id = id;
    this.product = product;
    this.customer = customer;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  @Override
  public String toString() {
    return "Order [id=" + id + ", firstName=" + getCustomer().getFirstName() + ", lastName=" + getCustomer().getLastName() + ", email=" + getCustomer().getEmail() + "]";
  }
}
