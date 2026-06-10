package com.llm.langchain4j.skill;

import dev.langchain4j.agent.tool.Tool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeQuerySkill {

    @Tool("查询当前日期、时间、星期几")
    public String timeQuery(String question) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String dayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
        return String.format("当前时间: %s %s %s",
                now.format(dateFmt), now.format(timeFmt), dayOfWeek);
    }
}