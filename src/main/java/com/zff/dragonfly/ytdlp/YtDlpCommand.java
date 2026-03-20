package com.zff.dragonfly.ytdlp;

import lombok.Builder;
import lombok.Singular;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * yt-dlp 命令构建器
 * <p>
 * 设计原则：
 * 1. 不可变性 (Immutable): 所有字段为 final，构建后不可修改。
 * 2. 普适性: 涵盖下载、提取、元数据、网络、播放列表、反爬等全场景。
 * 3. 智能组装: toCommandArgs() 自动处理参数冲突和默认值逻辑。
 */
@Slf4j
@Getter
@Builder(toBuilder = true)
@ToString
public class YtDlpCommand {

    // ================= 1. 核心目标 =================

    private final String ytPath;
    /** 视频/播放列表 URL (必填) */
    private final String url;

    /**
     * [任务1] 仅列出格式信息，不下载任何内容
     * 对应: --list-formats
     * 优先级：若为 true，将忽略 extractAudio, writeSubs 等下载类参数
     */
    @Builder.Default
    private final Boolean listFormatsOnly = false;

    /**
     * [任务5] 仅提取音频，丢弃视频流
     * 对应: -x (--extract-audio)
     */
    @Builder.Default
    private final Boolean extractAudio = false;

    /** 音频转换格式 (配合 extractAudio)
     * 对应: --audio-format (mp3, m4a, wav, flac, etc.)
     */
    private final String audioFormat;

    /** 音频音质 (0-9, 0最好)
     * 对应: --audio-quality
     */
    @Builder.Default
    private final String audioQuality = "5";

    // ================= 2. 格式选择 =================

    /**
     * 选择视频/音频格式代码
     * 对应: -f (--format)
     * 默认: bestvideo+bestaudio/best
     */
    @Builder.Default
    private final String format = "bestvideo+bestaudio/best";

    /**
     * 强制合并后的容器格式
     * 对应: --merge-output-format (mp4, mkv, etc.)
     */
    private final String mergeOutputFormat;

    // ================= 3. 字幕控制 =================

    /**
     * [任务2] 下载字幕文件
     * 对应: --write-sub
     */
    @Builder.Default
    private final Boolean writeSubs = false;

    /** 下载自动生成的字幕
     * 对应: --write-auto-sub
     */
    @Builder.Default
    private final Boolean writeAutoSubs = false;

    /** 将字幕嵌入到视频/音频文件中
     * 对应: --embed-subs
     */
    @Builder.Default
    private final Boolean embedSubs = false;

    /**
     * 指定字幕语言列表
     * 对应: --sub-langs
     * 用法: .subLang("zh-Hans").subLang("en")
     */
    @Singular
    private final List<String> subLangs;

    // ================= 4. 归档与去重 =================

    /**
     * [任务3] 下载归档文件路径
     * 对应: --download-archive <file>
     * 作用: 记录已下载的视频 ID，下次自动跳过
     */
    private final String downloadArchive;

    /** 不覆盖已存在的文件
     * 对应: --no-overwrites
     */
    @Builder.Default
    private final Boolean noOverwrites = false;

    // ================= 5. 网络与反爬 =================

    /**
     * [任务4] 绕过地域限制
     * 对应: --geo-bypass
     */
    @Builder.Default
    private final Boolean geoBypass = false;

    /** 指定国家代码 (配合 geoBypass)
     * 对应: --geo-bypass-country
     */
    private final String geoBypassCountry;

    /** 从浏览器读取 Cookie
     * 对应: --cookies-from-browser (chrome, firefox, edge, etc.)
     */
    private final String cookiesFromBrowser;

    /** 代理服务器地址
     * 对应: --proxy (http://host:port)
     */
    private final String proxy;

    /** 用户代理字符串
     * 对应: --user-agent
     */
    private final String userAgent;

    /** 重试次数
     * 对应: --retries
     */
    @Builder.Default
    private final Integer retries = 3;

    /** 限制下载速度
     * 对应: --limit-rate (e.g., "50K", "4.2M")
     */
    private final String limitRate;

    // ================= 6. 播放列表与过滤 =================

    /** 播放列表起始索引 (1-based)
     * 对应: --playlist-start
     */
    private final Integer playlistStart;

    /** 播放列表结束索引
     * 对应: --playlist-end
     */
    private final Integer playlistEnd;

    /** 只下载指定日期之后的视频
     * 对应: --dateafter (YYYYMMDD or (now-INTEGER[dwy]))
     */
    private final String dateAfter;

    /** 高级匹配过滤器
     * 对应: --match-filters
     */
    private final String matchFilter;

    // ================= 7. 输出与调试 =================

    /** 输出文件名模板
     * 对应: -o (--output)
     * 默认: %(title)s.%(ext)s
     */
    @Builder.Default
    private final String outputPath = "%(title)s.%(ext)s";

    /** 限制文件名只包含 ASCII 字符
     * 对应: --restrict-filenames
     */
    @Builder.Default
    private final Boolean restrictFilenames = false;

    /** 打印详细调试日志
     * 对应: --verbose
     */
    @Builder.Default
    private final Boolean verbose = false;

    /** 静默模式 (不打印进度条，仅错误/最终信息)
     * 对应: --quiet
     */
    @Builder.Default
    private final Boolean quiet = false;

    // ================= 核心方法：构建命令行 =================

    /**
     * 将当前配置转换为 yt-dlp 命令行参数列表
     * 逻辑处理：
     * 1. 处理互斥参数 (如 listFormatsOnly 优先)
     * 2. 处理默认值忽略 (null 或 false 的布尔值不添加)
     * 3. 保证参数顺序合理
     */
    public List<String> toCommandArgs() {
        List<String> args = new ArrayList<>();
        if(ytPath == null || ytPath.isEmpty()) throw new IllegalArgumentException("ytPath cannot be null or empty");
        args.add(ytPath);

        // --- 全局标志 ---
        if (Boolean.TRUE.equals(verbose)) args.add("--verbose");
        if (Boolean.TRUE.equals(quiet)) args.add("--quiet");

        // --- 任务 1: 仅列出格式 (高优先级) ---
        if (Boolean.TRUE.equals(listFormatsOnly)) {
            args.add("--list-formats");
            // 列出格式时，通常不需要其他下载参数，直接加 URL 返回即可
            // 但为了保险，我们保留 proxy/cookie 等网络设置，以防视频需要鉴权才能看格式
            addNetworkArgs(args);
            args.add(url);
            return args;
        }

        // --- 任务 5: 音频提取 vs 普通视频下载 ---
        if (Boolean.TRUE.equals(extractAudio)) {
            args.add("-x"); // --extract-audio
            if (audioFormat != null && !audioFormat.isEmpty()) {
                args.add("--audio-format");
                args.add(audioFormat);
            }
            if (audioQuality != null) {
                args.add("--audio-quality");
                args.add(audioQuality);
            }
            // 注意：提取音频时，-f (format) 参数通常被 yt-dlp 忽略或自动调整，
            // 但为了明确意图，如果用户指定了特定的音频流选择，仍可保留 -f，
            // 这里为了简洁，若 extractAudio=true，我们忽略通用的 video format 逻辑，
            // 除非用户想通过 -f 指定特定音轨。此处策略：若 extractAudio，则不使用默认的 video format。
            if (!"bestvideo+bestaudio/best".equals(format)) {
                // 如果用户自定义了 format (例如只要 m4a 音轨)，则保留
                args.add("-f");
                args.add(format);
            }
        } else {
            // 普通视频下载
            if (format != null && !format.isEmpty()) {
                args.add("-f");
                args.add(format);
            }
            if (mergeOutputFormat != null && !mergeOutputFormat.isEmpty()) {
                args.add("--merge-output-format");
                args.add(mergeOutputFormat);
            }
        }

        // --- 任务 2: 字幕 ---
        if (Boolean.TRUE.equals(writeSubs)) args.add("--write-sub");
        if (Boolean.TRUE.equals(writeAutoSubs)) args.add("--write-auto-sub");
        if (Boolean.TRUE.equals(embedSubs)) args.add("--embed-subs");

        if (subLangs != null && !subLangs.isEmpty()) {
            args.add("--sub-langs");
            args.add(String.join(",", subLangs));
        }

        // --- 任务 3: 归档与文件保护 ---
        if (downloadArchive != null && !downloadArchive.isEmpty()) {
            args.add("--download-archive");
            args.add(downloadArchive);
        }
        if (Boolean.TRUE.equals(noOverwrites)) {
            args.add("--no-overwrites");
        }

        // --- 任务 4: 网络与反爬 ---
        addNetworkArgs(args);

        // --- 播放列表与过滤 ---
        if (playlistStart != null) {
            args.add("--playlist-start");
            args.add(String.valueOf(playlistStart));
        }
        if (playlistEnd != null) {
            args.add("--playlist-end");
            args.add(String.valueOf(playlistEnd));
        }
        if (dateAfter != null && !dateAfter.isEmpty()) {
            args.add("--dateafter");
            args.add(dateAfter);
        }
        if (matchFilter != null && !matchFilter.isEmpty()) {
            args.add("--match-filters");
            args.add(matchFilter);
        }

        // --- 输出控制 ---
        if (outputPath != null && !outputPath.isEmpty()) {
            args.add("-o");
            args.add(outputPath);
        }
        if (Boolean.TRUE.equals(restrictFilenames)) {
            args.add("--restrict-filenames");
        }
        if (limitRate != null && !limitRate.isEmpty()) {
            args.add("--limit-rate");
            args.add(limitRate);
        }
        if (retries != null) {
            args.add("--retries");
            args.add(String.valueOf(retries));
        }

        // --- 最后添加 URL ---
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("URL is required for YtDlpRequest");
        }
        args.add(url);

        return args;
    }

    /**
     * 抽取网络相关参数，因为无论是否 listFormats 都可能需要网络配置
     */
    private void addNetworkArgs(List<String> args) {
        if (Boolean.TRUE.equals(geoBypass)) {
            args.add("--geo-bypass");
            if (geoBypassCountry != null && !geoBypassCountry.isEmpty()) {
                args.add("--geo-bypass-country");
                args.add(geoBypassCountry);
            }
        }
        if (cookiesFromBrowser != null && !cookiesFromBrowser.isEmpty()) {
            args.add("--cookies-from-browser");
            args.add(cookiesFromBrowser);
        }
        if (proxy != null && !proxy.isEmpty()) {
            args.add("--proxy");
            args.add(proxy);
        }
        if (userAgent != null && !userAgent.isEmpty()) {
            args.add("--user-agent");
            args.add(userAgent);
        }
    }

    /**
     * 静态工厂方法：快速创建一个基础请求
     */
    public static YtDlpCommand of(String url) {
        return YtDlpCommand.builder().url(url).ytPath("yt-dlp.exe").build();
    }
}
