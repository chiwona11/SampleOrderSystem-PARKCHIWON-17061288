package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductionItem {

    private final String orderId;
    private final String sampleId;
    private final int requiredQuantity;
    private final int actualProduction;
    private final long totalProductionTime;
    private final String enqueuedAt;

    @JsonCreator
    public ProductionItem(
            @JsonProperty("orderId")             String orderId,
            @JsonProperty("sampleId")            String sampleId,
            @JsonProperty("requiredQuantity")    int requiredQuantity,
            @JsonProperty("actualProduction")    int actualProduction,
            @JsonProperty("totalProductionTime") long totalProductionTime,
            @JsonProperty("enqueuedAt")          String enqueuedAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.requiredQuantity = requiredQuantity;
        this.actualProduction = actualProduction;
        this.totalProductionTime = totalProductionTime;
        this.enqueuedAt = enqueuedAt;
    }

    public String getOrderId()            { return orderId; }
    public String getSampleId()           { return sampleId; }
    public int getRequiredQuantity()      { return requiredQuantity; }
    public int getActualProduction()      { return actualProduction; }
    public long getTotalProductionTime()  { return totalProductionTime; }
    public String getEnqueuedAt()         { return enqueuedAt; }

    @Override
    public String toString() {
        return "ProductionItem{orderId='" + orderId + "', sampleId='" + sampleId +
               "', requiredQuantity=" + requiredQuantity +
               ", actualProduction=" + actualProduction +
               ", totalProductionTime=" + totalProductionTime +
               ", enqueuedAt='" + enqueuedAt + "'}";
    }
}
