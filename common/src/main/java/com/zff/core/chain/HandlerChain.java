package com.zff.core.chain;




import com.zff.core.datasink.DataSink;
import com.zff.core.entity.StandardRecord;

import java.util.ArrayList;
import java.util.List;

public class HandlerChain {
    private List<DataSink> handlers = new ArrayList<>();

    public void addHandler(DataSink handler) {
        this.handlers.add(handler);
    }

    public List<StandardRecord> handle(List<StandardRecord> records) {
        for (DataSink handler : handlers) {
            records = handler.transform(records);
        }
        return records;
    }
}
