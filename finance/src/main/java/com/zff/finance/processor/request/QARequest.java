package com.zff.finance.processor.request;

public interface QARequest extends FastNewsRequest {
     boolean enableQA();
     String getCode();
     Integer getPageSize();
}
