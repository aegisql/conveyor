package com.aegisql.conveyor.poc.aggregator.model;

import java.util.Date;
import java.util.List;

public class Order {

    String orderNumber;
    Date date;
    CustomerInfo customerInfo;
    List<Item> items;


}
