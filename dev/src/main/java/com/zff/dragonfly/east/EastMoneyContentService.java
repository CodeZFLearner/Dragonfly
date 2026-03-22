package com.zff.dragonfly.east;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EastMoneyContentService {

    // 线程池 (替代 goroutine)
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // --- 数据模型 (对应 Go Structs) ---

    @Data
    public static class NewsResponse {
        private String reqTrace;
        private String code;
        private String message;
        private NewsData data;
    }

    @Data
    public static class NewsData {
        private int totalHits;
        private String sortEnd;
        private int pageSize;
        private List<NewsItem> news;
    }

    @Data
    public static class NewsItem {
        private String code;
        private String title;
        private String npDst;
        private String showTime;
        private Object interact;
        private String mediaName;
        private String content; // 手动填充
    }

    @Data
    public static class OrgResponse {
        private List<OrgReplyItem> re;
        private int intelligentReply;
        private int classicReply;

        @Data
        public static class OrgReplyItem {
            private long postId;
            private PostUser postUser;
            private String postTitle;
            private String postAbstract;
            private String postLastTime;
            private int postClickCount;
            private int postCommentCount;
            private Object postAddress;

            @Data
            public static class PostUser {
                private String userId;
                private String userNickname;
                // ... 其他字段按需添加
            }
        }
    }

    @Data
    public static class GubaResponse {
        private PostDetail post;
        private int rc;
        private String me;

        @Data
        public static class PostDetail {
            private long postId;
            private String postTitle;
            private String postContent;
            private String postAbstract;
            private String postPublishTime;
            private String postLastTime;
            private String postDisplayTime;
            private int postClickCount;
            private int postForwardCount;
            private int postCommentCount;
            private int postCommentAuthority;
            private int postLikeCount;
            private boolean postIsLike;
            private String postIpAddress;
        }
    }

    @Data
    public static class QAResponse {
        private List<QAItem> re;
        private int totalPage;
        private int count;
        private int pageIndex;
        private int pageSize;
        private String currentSecretary;
        private String stockName;

        @Data
        public static class QAItem {
            private long postId;
            private String userId;
            private String userNickname;
            private int postClickCount;
            private int postForwardCount;
            private int postCommentCount;
            private int postLikeCount;
            private int postState;
            private int postFromNum;
            private String askQuestion;
            private String askAnswer;
            private boolean postIsCollected;
            private int replyCount;
        }
    }

    // --- 业务方法 ---

    /**
     * 获取新闻列表 (HandleNews)
     */
    public NewsResponse handleNews(String midCode, short pageSize) {
        String url = "https://np-seclist.eastmoney.com/sec/getQuoteNews";

        JSONObject requestBody = new JSONObject();
        requestBody.set("appKey", "fd374bf183b866ce5cf7b00b92bb9858");
        requestBody.set("biz", "sec_quote_news");
        requestBody.set("client", "sec_android");
        requestBody.set("clientVersion", "10.23");
        requestBody.set("deviceId", "");
        requestBody.set("midCode", midCode);
        requestBody.set("pageSize", pageSize);
        requestBody.set("req_trace", "fac289aa-39ed-4bf1-b8b6-fd999a7d1b2a");
        requestBody.set("sortEnd", "");

        try {
            String respBody = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute()
                    .body();
            return JSONUtil.toBean(respBody, NewsResponse.class);
        } catch (Exception e) {
            log.error("HandleNews failed for code: {}", midCode, e);
            NewsResponse errResp = new NewsResponse();
            errResp.setMessage(e.getMessage());
            return errResp;
        }
    }


    /**
     * 获取股吧帖子内容 (guba)
     */
    public GubaResponse getGubaPost(String postId) {
        String paramTemplate = "ctoken=&utoken=&deviceid=$IP$&version=10023000&product=EastMoney&plat=Android&deviceId=88f2572d23a9e2b97d277a16fb8b759b||iemi_tluafed_me&postid=%s&IsMatch=false&type=0&cutword=true&paytext=true";
        String url = "https://emcreative.eastmoney.com/FortuneApi/GuBaApi/common";

        JSONObject requestBody = new JSONObject();
        requestBody.set("url", "content/api/Post/ArticleContent");
        requestBody.set("type", "post");
        requestBody.set("sumit", "form");
        requestBody.set("parm", String.format(paramTemplate, postId));

        try {
            String respBody = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute()
                    .body();

            GubaResponse response = JSONUtil.toBean(respBody, GubaResponse.class);

            // 清洗 HTML 标签
            if (response.getPost() != null && StrUtil.isNotEmpty(response.getPost().getPostContent())) {
                String content = response.getPost().getPostContent();
                content = ReUtil.replaceAll(content, "<.*?>", ""); // 去除标签
                content = content.replace("&nbsp;", " ");          // 替换空格
                response.getPost().setPostContent(content);
                log.debug("Cleaned content for post {}: {}", postId, content.substring(0, Math.min(50, content.length())));
            }
            return response;
        } catch (Exception e) {
            log.error("GetGubaPost failed for id: {}", postId, e);
            return new GubaResponse();
        }
    }

    /**
     * 获取热评 - 机构 (HandleOrgList)
     */
    public OrgResponse handleOrgList(String code, short pageSize) {
        String url = "https://gbapi-pj.emhudong.cn/apparticlelistv2/api/Hot/Articlelist";

        // 构造 Form 表单数据 (application/x-www-form-urlencoded)
        String baseParam = "ctoken=&p=1&product=StockWay&randomtext=-orAWxN_q3vguTIJ7PvjWHoRvK7dRWYzD9BaoR1gzTfpxPn9GHAViLuQW0tamOlGrOioZ_fcj1KhmlxRNt2X8A&ps=%s&code=%s&pi=&utoken=&plat=Android&type=2&deviceid=f0cca1bbe2ddb67cfd232a85e7820e6a&version=10023000";
        String data = String.format(baseParam, pageSize, code);

        try {
            String respBody = HttpRequest.post(url)
                    .header("EM-OS", "Android")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(data)
                    .execute()
                    .body();
            return JSONUtil.toBean(respBody, OrgResponse.class);
        } catch (Exception e) {
            log.error("HandleOrgList failed for code: {}", code, e);
            return new OrgResponse();
        }
    }

    /**
     * 获取董秘问答 (QA)
     */
    public QAResponse getQA(String code, short pageSize) {
        String url = "https://mgubaqa.eastmoney.com/interface/GetData.aspx";

        JSONObject requestBody = new JSONObject();
        requestBody.set("param", String.format("code=%s&qatype=1&p=1&ps=%d&keyword=&questioner=", code, pageSize));
        requestBody.set("path", "question/api/info/Search");

        try {
            String respBody = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute()
                    .body();
            return JSONUtil.toBean(respBody, QAResponse.class);
        } catch (Exception e) {
            log.error("GetQA failed for code: {}", code, e);
            return new QAResponse();
        }
    }


    // 关闭线程池 (应用关闭时调用)
    public void shutdown() {
        executor.shutdown();
    }
}
