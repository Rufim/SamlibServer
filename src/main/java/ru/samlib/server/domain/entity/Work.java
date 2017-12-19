package ru.samlib.server.domain.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.samlib.server.domain.Linkable;
import ru.samlib.server.domain.Validatable;
import ru.samlib.server.util.TextUtils;

import javax.persistence.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;


/**
 * Created by Rufim on 22.05.2014.
 */
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@ToString(exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@Entity
@Table(name = "work")
public class Work implements Serializable, Linkable, Validatable {

    private static final long serialVersionUID = -2705011939329628695L;
    public static final String HTML_SUFFIX = ".shtml";
    public static final String FB2_SUFFIX = ".fb2.zip";

    public static final String COMMENT_PREFIX = "/comment";
    public static final String ILLUSTRATION_PREFIX = "/img";

    @Id
    String link;
    String title;
    @ManyToOne
    Category category;
    @ManyToOne
    Author author;
    String imageLink;
    Integer size;
    Integer sizeDiff;
    BigDecimal rate;
    Integer kudoed;
    BigDecimal expertRate;
    Integer expertKudoed;
    Integer views;
    @ElementCollection(targetClass = Genre.class)
    @CollectionTable(name = "genres")
    @Column(name = "genre", nullable = false)
    @Enumerated(EnumType.STRING)
    List<Genre> genres = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    Type type = Type.OTHER;
    @Column(columnDefinition = "TEXT")
    String annotation;
    Date createDate;
    Date updateDate;
    Date cachedDate;
    Date changedDate;
    String description;
    boolean hasIllustration = false;
    boolean hasComments = false;
    boolean hasRate = false;
    boolean changed = false;
    boolean recommendation = false;
    boolean rootWork = false;

    String md5;


    public Work(){}

    public Work(String link) {
        setLink(link);
    }

    public void setLink(String link) {
        if (link == null) return;
        link = TextUtils.eraseHost(link);
        if (link.contains("/")) {
            if (author == null) {
                author = new Author(link.substring(0, link.lastIndexOf("/")));
            }
            this.link = (author.getLink() + link.substring(link.lastIndexOf("/")));
        } else {
            this.link = "/" + link;
        }
        this.link = this.link.replaceAll("/+", "/");
    }

    public String getLink() {
        if (link != null && !link.contains(getAuthor().getLink())) {
            link = (author.getLink() + link).replaceAll("/+", "/");
        }
        return link;
    }


    public boolean isNotSamlib() {
        return getLink() == null;
    }

    public String getLinkWithoutSuffix() {
        return getLink().replace(HTML_SUFFIX, "");
    }

    public Author getAuthor() {
        if (author == null) {
            if (getCategory() != null) {
                return author = getCategory().getAuthor();
            }
        }
        return author;
    }

    public Link getIllustrationsLink() {
        return new Link(ILLUSTRATION_PREFIX + getLink().replace(HTML_SUFFIX, "/index" + HTML_SUFFIX));
    }

    public Link getCommentsLink() {
        return new Link(COMMENT_PREFIX + getLinkWithoutSuffix());
    }

    public String getTypeName() {
        if (getCategory() != null) {
            return getCategory().getTitle();
        } else {
            return getType().getTitle();
        }
    }

    public String printGenres() {
        if (getGenres().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Genre genre : getGenres()) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append(genre.getTitle());
        }
        return builder.toString();
    }

    @Transient
    public void setGenresAsString(String genres) {
        if (getGenres() == null) {
            setGenres(new ArrayList<>());
        } else {
            getGenres().clear();
        }
        for (String genre : genres.split(",")) {
            addGenre(genre);
        }
    }

    public void addGenre(String genre) {
        Genre tryGenre = Genre.parseGenre(genre);
        if (Collections.emptyList().equals(getGenres())) {
            setGenres(new ArrayList<>());
        }
        if (tryGenre != null) {

            getGenres().add(tryGenre);
        } else {
            getGenres().add(Genre.EMPTY);
        }
    }

    public void addGenre(Genre genre) {
        if (Collections.emptyList().equals(getGenres())) {
            setGenres(new ArrayList<>());
        }
        if (getGenres() == null) {
            setGenres(new ArrayList<>());
        }
        if(genre != null) {
            getGenres().add(genre);
        }
    }

    public String getAnnotation() {
        return annotation;
    }

    public void addAnnotation(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean validate() {
        return author != null && author.validate() && title != null && link != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Work)) return false;

        Work work = (Work) o;

        if (work.getLink() == null && getLink() == null) return true;
        if (work.getLink() == null || getLink() == null) return false;
        return TextUtils.trim(getLink()).equalsIgnoreCase(TextUtils.trim(work.getLink()));
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

}
