package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Order {

    private final String id;
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private final OrderStatus status;
    private final String createdAt;

    @JsonCreator
    public Order(
            @JsonProperty("id")           String id,
            @JsonProperty("sampleId")     String sampleId,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("quantity")     int quantity,
            @JsonProperty("status")       OrderStatus status,
            @JsonProperty("createdAt")    String createdAt) {
        this.id = id;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId()           { return id; }
    public String getSampleId()     { return sampleId; }
    public String getCustomerName() { return customerName; }
    public int getQuantity()        { return quantity; }
    public OrderStatus getStatus()  { return status; }
    public String getCreatedAt()    { return createdAt; }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, sampleId, customerName, quantity, newStatus, createdAt);
    }

    @Override
    public String toString() {
        return "Order{id='" + id + "', sampleId='" + sampleId +
               "', customerName='" + customerName + "', quantity=" + quantity +
               ", status=" + status + ", createdAt='" + createdAt + "'}";
    }
}
