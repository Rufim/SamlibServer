package ru.samlib.server.domain.dao;

import lombok.Getter;


public enum SortWorksBy {
    ACTIVITY("Активности"),RATING("Рейтингу"),VIEWS("Просмотрам");

    @Getter final String title;

    SortWorksBy(String title) {
        this.title = title;
    }

}
