package com.example.myapplication;

public class CartItem {
    private String productName;
    private int quantity;
    private int price;

    public CartItem() {
    }

    public CartItem(String productName, int quantity, int price) {
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }
}
