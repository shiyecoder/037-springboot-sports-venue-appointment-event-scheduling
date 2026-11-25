package com.grad.svs.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局时间格式化：全系统统一 yyyy-MM-dd HH:mm:ss，禁止各页面自行拼格式。
 */
public final class TimeUtil {

    public static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeUtil() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(FULL);
    }
}
