package com.cube.examples.model;

public class Product {
  private Integer Id;

  public Product(Integer id, String productName, Integer price) {
    Id = id;
    this.productName = productName;
    this.price = price;
  }

  public Integer getId() {
    return Id;
  }

  public void setId(Integer id) {
    Id = id;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public Integer getPrice() {
    return price;
  }

  public void setPrice(Integer price) {
    this.price = price;
  }

  private String productName;
  private Integer price;
}
