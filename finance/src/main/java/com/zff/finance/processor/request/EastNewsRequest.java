package com.zff.finance.processor.request;

public interface EastNewsRequest extends FastNewsRequest {

     boolean enableEastNews();
     String getCode();
     Integer getPageSize();
}
