package com.cube.examples.model;

public class Order {

    public Order() {

    }

    public Order(Integer id, Integer productId, Customer customer) {
        super();
        this.id = id;
        this.productId = productId;
        this.customer = customer;
    }
 
    private Integer id;
    private Integer productId;
    private Customer customer;

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

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", firstName=" + customer.firstName + ", lastName=" + customer.lastName + ", email=" + customer.email + "]";
    }

    public static class Customer {

        private String firstName;
        private String lastName;
        private String email;

        public Customer (String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
