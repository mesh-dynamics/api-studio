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
