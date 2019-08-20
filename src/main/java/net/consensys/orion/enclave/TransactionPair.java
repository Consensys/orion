package net.consensys.orion.enclave;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionPair {

    private final String id;
    private final String value;


    public TransactionPair(@JsonProperty("id") String id, @JsonProperty("value") String value) {
        this.id = id;
        this.value = value;
    }

    @JsonProperty("value")
    public String value() {
        return value;
    }

    @JsonProperty("id")
    public String id() {
        return id;
    }
}
