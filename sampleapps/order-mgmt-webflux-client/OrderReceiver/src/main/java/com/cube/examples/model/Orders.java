package com.cube.examples.model;

import java.util.ArrayList;
import java.util.List;

public class Orders
{
    private List<Order> orderList;
    
    public List<Order> getOrderList() {
        if(orderList == null) {
            orderList = new ArrayList<>();
        }
        return orderList;
    }
 
    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList;
    }
}
