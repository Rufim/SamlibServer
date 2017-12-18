package ru.samlib.server.domain;

import org.intellij.lang.annotations.RegExp;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Created by Rufim on 07.01.2015.
 */

@ConfigurationProperties("settings.server")
public class Constants {

    public static class Net {
        public static final String BASE_SCHEME = "http";
        public static final String BASE_HOST = "samlib.ru";
        public static final String BASE_DOMAIN = BASE_SCHEME + "://" + BASE_HOST;
        public static final String USER_AGENT = "Mozilla";
        public static final String LOG_PATH = BASE_DOMAIN + "/logs";
    }

    public static class Pattern {
        public static final String TIME_PATTERN = "HH:mm:ss";
        public static final String DATA_PATTERN = "dd/MM/yyyy";
        public static final String DATA_TIME_PATTERN = "dd-MM-yyyy HH:mm:ss";
        public static final String DATA_ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        public static final String DATA_ISO_8601_24H_FULL_FORMAT_WITHOUT_MC = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        @RegExp
        public static final String WORK_URL_REGEXP = "/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+\\.shtml";
        @RegExp
        public static final String AUTHOR_URL_REGEXP = "/*[a-z]/+[a-z_0-9]+(/*)";
        @RegExp
        public static final String COMMENTS_URL_REGEXP = "/comment/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+";
        @RegExp
        public static final String ILLUSTRATIONS_URL_REGEXP = "/img/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+/index\\.shtml";
    }

    private String firstLogDay;
    private Integer logsPerDay;

    public String getFirstLogDay() {
        return firstLogDay;
    }

    public void setFirstLogDay(String firstLogDay) {
        this.firstLogDay = firstLogDay;
    }

    public Integer getLogsPerDay() {
        return logsPerDay;
    }

    public void setLogsPerDay(Integer logsPerDay) {
        this.logsPerDay = logsPerDay;
    }
}
