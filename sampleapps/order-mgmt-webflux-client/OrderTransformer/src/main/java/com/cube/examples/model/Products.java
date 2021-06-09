package com.cube.examples.model;

import java.util.HashMap;

public class Products {

  private HashMap<Integer, Product> productIdVsProduct = new HashMap();

  public Product getProductById(Integer productId) {
    if (productIdVsProduct.containsKey(productId)) {
      return productIdVsProduct.get(productId);
    } else {
      throw new IllegalArgumentException("Product id not found");
    }
  }

  public void addProduct (Product product) {
    productIdVsProduct.put(product.getId(), product);
  }
}
