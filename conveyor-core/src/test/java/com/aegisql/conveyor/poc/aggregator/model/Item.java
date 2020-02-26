package com.aegisql.conveyor.poc.aggregator.model;

public class Item {

    String name;
    double unitPrice;
    int numberOfItems;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalPrice() {
        return unitPrice * numberOfItems;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Item{");
        sb.append(name);
        sb.append(", unitPrice=").append(unitPrice);
        sb.append(", numberOfItems=").append(numberOfItems);
        sb.append('}');
        return sb.toString();
    }
}
