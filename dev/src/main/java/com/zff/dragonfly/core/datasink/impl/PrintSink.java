package com.zff.dragonfly.core.datasink.impl;

import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.entity.StandardRecord;

import java.util.List;

public class PrintSink implements DataSink {
    @Override
    public void init() {

    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        if (records != null) {
            for (StandardRecord record : records) {
                System.out.println(record);
            }
        }
        return List.of();
    }

    @Override
    public void close() {

    }
}
