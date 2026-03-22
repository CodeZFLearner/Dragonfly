package com.zff.core.processor;

public enum ProcessorType {

    FASTNEWS("fast news"),
    FUTUREEVENT("future event"),;

    private String description;

    ProcessorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
