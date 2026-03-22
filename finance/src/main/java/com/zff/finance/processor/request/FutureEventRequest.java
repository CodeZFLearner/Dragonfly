package com.zff.finance.processor.request;

import com.zff.core.processor.ProcessorRequest;

public interface FutureEventRequest extends ProcessorRequest {
    boolean enableFutureEvent();
}
