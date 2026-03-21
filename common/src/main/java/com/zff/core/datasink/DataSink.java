package com.zff.core.datasink;




import com.zff.core.entity.StandardRecord;

import java.util.List;

public interface DataSink {
    void init();
    List<StandardRecord> transform(List<StandardRecord> records);
    void close();
}
