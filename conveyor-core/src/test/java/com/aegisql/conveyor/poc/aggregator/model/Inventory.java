package com.aegisql.conveyor.poc.aggregator.model;

import java.util.Map;

public class Inventory {

    Map<String, Item> items;


    public Map<String, Item> getItems() {
        return items;
    }

    public void setItems(Map<String, Item> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Inventory{");
        sb.append("items=").append(items);
        sb.append('}');
        return sb.toString();
    }



}
