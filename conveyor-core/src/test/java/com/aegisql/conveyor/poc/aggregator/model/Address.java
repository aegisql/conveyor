package com.aegisql.conveyor.poc.aggregator.model;

public class Address {
    String streetNumber;
    String street;
    String town;
    State state;
    String zipCode;

    public Address() {

    }

    public Address(String streetNumber, String street, String town, State state, String zipCode) {
        this.streetNumber = streetNumber;
        this.street = street;
        this.town = town;
        this.state = state;
        this.zipCode = zipCode;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Address{");
        sb.append(streetNumber);
        sb.append(", ").append(street);
        sb.append(", ").append(town);
        sb.append(", ").append(state);
        sb.append(", ").append(zipCode);
        sb.append('}');
        return sb.toString();
    }
}
