package com.zff.dragonfly.core.datasink;



import com.zff.dragonfly.core.entity.StandardRecord;

import java.util.List;

public interface DataSink {
    void init();
    List<StandardRecord> transform(List<StandardRecord> records);
    void close();
}
