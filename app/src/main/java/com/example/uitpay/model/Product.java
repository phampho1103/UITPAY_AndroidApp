package com.example.uitpay.model;

public class Product {
    private String productId;
    private String name;
    private double price;
    private String productImage;
    private int quantity;

    public Product() {
        this.quantity = 1;
    }

    public Product(String productId, String name, double price, String productImage) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.productImage = productImage;
        this.quantity = 1;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getProductImage() { return productImage; }
    public int getQuantity() { return quantity; }

    public void setProductId(String productId) { this.productId = productId; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public void incrementQuantity() {
        this.quantity++;
    }
} 