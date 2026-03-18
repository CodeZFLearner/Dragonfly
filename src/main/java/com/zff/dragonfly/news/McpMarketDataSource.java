package com.zff.dragonfly.news;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.impl.JsonFileSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class McpMarketDataSource extends AbstractDataSource {

    String SOURCE_ID = "McpMarket";
    String url = "https://mcpmarket.cn/api/servers?page=2&per_page=12&category=%E6%89%98%E7%AE%A1&sort_by=popular";
    public McpMarketDataSource() {
        super(new JsonFileSink("C:\\Users\\TEMP.DEMOFF.010\\Desktop\\servers.json"));
    }
    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        HttpResponse response = HttpUtil.createGet(url).execute();
        List<StandardRecord> records = new ArrayList<>();
        if (!response.isOk()) {
            return records;
        }

        String body = response.body();
        McpMarketPage mcpMarketPage = JSONUtil.toBean(body, McpMarketPage.class);
        if(mcpMarketPage ==null || mcpMarketPage.servers ==null){
            return records;
        }
        for (Server server : mcpMarketPage.servers) {
            String jsonStr = JSONUtil.toJsonStr(server);
            StandardRecord record = new StandardRecord();
            record.setSourceId(SOURCE_ID);
            record.setData(jsonStr);
            records.add(record);

        }
        return records;

    }

    @Data
    static class McpMarketPage {
        Integer current_page;
        List<Server> servers;
    }
    @Data
    static class Server{
        String _id;
        String alias;
        String by;
        String description;
        String logo;
    }

    public static void main(String[] args) {
        try {
            new McpMarketDataSource().run();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
