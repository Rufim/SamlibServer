package ru.samlib.server.domain.entity;


import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class CategoryId implements Serializable {
      String authorLink;
      String title;

    public CategoryId() {
    }

    public CategoryId(String authorLink, String title) {
        this.authorLink = authorLink;
        this.title = title;
    }
}                               
