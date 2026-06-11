package com.llm.springai.tool;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Component
public class TimeQueryTool implements Function<TimeQueryTool.Request, String> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (EEEE)");

    public record Request(
            String query
    ) {}

    @Override
    public String apply(Request request) {
        String query = request.query;
        if (query == null) query = "now";
        String lower = query.toLowerCase();

        if (lower.contains("timezone") || lower.contains("时区")) {
            String tz = extractTimezone(query);
            if (tz != null) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of(tz));
                    return "时区 " + tz + " 当前时间：" + FMT.format(zdt);
                } catch (Exception e) {
                    return "无效时区: " + tz;
                }
            }
        }

        ZonedDateTime now = ZonedDateTime.now();
        Instant ts = Instant.now();

        if (lower.contains("timestamp") || lower.contains("时间戳")) {
            return "当前 Unix 时间戳：" + ts.getEpochSecond() + " (秒)\n当前时间：" + FMT.format(now) + " " + now.getZone();
        }

        if (lower.contains("weekday") || lower.contains("星期")) {
            String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            return "今天是 " + FMT.format(now) + " " + weekdays[now.getDayOfWeek().getValue() - 1];
        }

        if (lower.contains("date") || lower.contains("日期")) {
            return "当前日期：" + now.toLocalDate() + " (" +
                   now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE) + ")";
        }

        return "当前时间：" + FMT.format(now) + "\n时区：" + now.getZone() + "\nUnix 时间戳：" + ts.getEpochSecond();
    }

    private String extractTimezone(String query) {
        String[] known = {"Asia/Shanghai", "Asia/Tokyo", "America/New_York",
                "America/Los_Angeles", "Europe/London", "Europe/Berlin",
                "Asia/Singapore", "Australia/Sydney", "Pacific/Auckland"};
        for (String z : known) if (query.contains(z)) return z;
        String[] parts = query.split("\\s+");
        String last = parts[parts.length - 1];
        return last.contains("/") ? last : null;
    }
}