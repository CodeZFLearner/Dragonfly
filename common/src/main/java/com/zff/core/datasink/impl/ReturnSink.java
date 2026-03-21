package com.zff.core.datasink.impl;

import com.zff.core.datasink.DataSink;
import com.zff.core.entity.StandardRecord;

import java.util.List;

public class ReturnSink implements DataSink {
    @Override
    public void init() {

    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        return records;
    }

    @Override
    public void close() {

    }
}
