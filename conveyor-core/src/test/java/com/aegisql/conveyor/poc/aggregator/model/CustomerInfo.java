package com.aegisql.conveyor.poc.aggregator.model;

import java.util.List;

public class CustomerInfo {

    private String firstName;
    private String lastName;
    private Address billingAddress;
    private Address shippingAddress;
    private List<String> phones;

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

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CustomerInfo{");
        sb.append(firstName);
        sb.append(" ").append(lastName);
        sb.append(", billingAddress=").append(billingAddress);
        sb.append(", shippingAddress=").append(shippingAddress);
        sb.append(", phones=").append(phones);
        sb.append('}');
        return sb.toString();
    }
}
