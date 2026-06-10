package com.llm.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间查询工具 —— 获取当前时间、日期、时区信息等
 */
public class TimeQueryTool implements Tool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (EEEE)");

    @Override
    public String getName() {
        return "time_query";
    }

    @Override
    public String getDescription() {
        return "查询当前时间、日期、星期、时区等信息。当需要知道现在是什么时间、今天是几号、星期几时使用。";
    }

    @Override
    public String getParametersJsonSchema() {
        return "{" +
                "\"type\":\"object\"," +
                "\"properties\":{" +
                    "\"query\":{" +
                        "\"type\":\"string\"," +
                        "\"description\":\"查询内容，如 'now'（现在）、'date'（日期）、'weekday'（星期几）、'timestamp'（时间戳）、'timezone Asia/Shanghai'（时区时间）\"" +
                    "}" +
                "}," +
                "\"required\":[\"query\"]" +
                "}";
    }

    @Override
    public String execute(String argumentsJson) {
        String query = SearchTool.extractStringField(argumentsJson, "query");
        if (query == null) query = "now";

        String lower = query.toLowerCase();

        // 解析时区查询
        if (lower.contains("timezone") || lower.contains("时区")) {
            String tz = extractTimezone(query);
            if (tz != null) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of(tz));
                    return "时区 " + tz + " 当前时间：" + FMT.format(zdt);
                } catch (Exception e) {
                    return "无效时区: " + tz + "。可用时区示例：Asia/Shanghai, America/New_York, Europe/London";
                }
            }
        }

        ZonedDateTime now = ZonedDateTime.now();
        Instant ts = Instant.now();

        if (lower.contains("timestamp") || lower.contains("时间戳")) {
            return "当前 Unix 时间戳：" + ts.getEpochSecond() + " (秒)\n" +
                   "当前时间：" + FMT.format(now) + " " + now.getZone();
        }

        if (lower.contains("weekday") || lower.contains("星期")) {
            String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            return "今天是 " + FMT.format(now) + " " + weekdays[now.getDayOfWeek().getValue() - 1];
        }

        if (lower.contains("date") || lower.contains("日期")) {
            return "当前日期：" + now.toLocalDate() + " (" +
                   now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE) + ")";
        }

        // 默认返回完整时间信息
        return "当前时间：" + FMT.format(now) + "\n" +
               "时区：" + now.getZone() + "\n" +
               "Unix 时间戳：" + ts.getEpochSecond();
    }

    private String extractTimezone(String query) {
        // 匹配 timezone XXX 或 时区 XXX
        String[] knownZones = {"Asia/Shanghai", "Asia/Tokyo", "America/New_York",
                "America/Los_Angeles", "Europe/London", "Europe/Berlin",
                "Asia/Singapore", "Australia/Sydney", "Pacific/Auckland"};
        for (String zone : knownZones) {
            if (query.contains(zone)) return zone;
        }
        // 尝试从查询中提取最后一个词作为时区
        String[] parts = query.split("\\s+");
        String last = parts[parts.length - 1];
        if (last.contains("/")) return last;
        return null;
    }
}