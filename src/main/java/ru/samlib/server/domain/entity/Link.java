package ru.samlib.server.domain.entity;



import lombok.Data;
import ru.samlib.server.domain.Linkable;
import ru.samlib.server.domain.Validatable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Rufim on 01.07.2015.
 */
@Data
@Entity
@Table(name = "link")
public class Link implements Validatable, Linkable, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id = 0L;
    @ManyToOne
    Author author;
    @ManyToOne
    Category category;

    boolean rootLink = false;

    String title;
    String link;
    String annotation;

    public Link(){}

    public Link(String title, String link, String annotation) {
        this.title = title;
        this.link = link;
        this.annotation = annotation;
    }

    public Author getAuthor() {
        if(author == null) {
            if(getCategory() != null) {
                return author = getCategory().getAuthor();
            }
        }
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Link)) return false;
        Link link = (Link) o;
        return this.link == null ? link.link == null : this.link.equalsIgnoreCase(link.link);
    }


    public Link(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return link;
    }

    @Override
    public boolean validate() {
        return link != null && title != null;
    }

}
