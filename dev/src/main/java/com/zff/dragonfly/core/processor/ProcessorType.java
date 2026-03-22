package com.zff.dragonfly.core.processor;

public enum ProcessorType {

    COMPOSITE("composite"),
    USER("user");

    private String type;

    ProcessorType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
