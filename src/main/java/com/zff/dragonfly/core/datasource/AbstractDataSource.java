package com.zff.dragonfly.core.datasource;


import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.entity.StandardRecord;

import java.util.List;

public abstract class AbstractDataSource {
    DataSink sink;
    public AbstractDataSource(DataSink sink) {
        this.sink = sink;
    }
    // 模板方法：定义流程
    public final void run() {
        long start = System.currentTimeMillis();
        try {

            // 2. 转换数据 (由子类或通用逻辑实现)
            List<StandardRecord> records = doFetch();
            // 3. 写入下游 (调用 Sink)
            sink.init();
            sink.transform(records);
            // 【监控埋点】成功
//            MetricsCollector.recordSuccess(records.size(), System.currentTimeMillis() - start);

        } catch (Exception e) {
            e.printStackTrace();
            // 【监控埋点】失败
//            MetricsCollector.recordFailure(e);
        }finally {
            sink.close();
        }
    }

    // 子类只需关注具体怎么取数据
    protected abstract List<StandardRecord> doFetch() throws Exception;
}
