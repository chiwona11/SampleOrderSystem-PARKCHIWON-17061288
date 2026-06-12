package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Inventory {

    private final String sampleId;
    private final int stock;

    @JsonCreator
    public Inventory(
            @JsonProperty("sampleId") String sampleId,
            @JsonProperty("stock")    int stock) {
        this.sampleId = sampleId;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public int getStock()       { return stock; }

    public Inventory withStock(int newStock) {
        return new Inventory(sampleId, newStock);
    }

    @Override
    public String toString() {
        return "Inventory{sampleId='" + sampleId + "', stock=" + stock + "}";
    }
}
