package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Sample {

    private final String id;
    private final String name;
    private final long avgProductionTime;
    private final double yield;

    @JsonCreator
    public Sample(
            @JsonProperty("id")                String id,
            @JsonProperty("name")              String name,
            @JsonProperty("avgProductionTime") long avgProductionTime,
            @JsonProperty("yield")             double yield) {
        this.id = id;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
    }

    public String getId()               { return id; }
    public String getName()             { return name; }
    public long getAvgProductionTime()  { return avgProductionTime; }
    public double getYield()            { return yield; }

    @Override
    public String toString() {
        return "Sample{id='" + id + "', name='" + name +
               "', avgProductionTime=" + avgProductionTime +
               ", yield=" + yield + "}";
    }
}
