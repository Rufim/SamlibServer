package ru.samlib.server.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import ru.samlib.server.domain.Linkable;
import ru.samlib.server.util.TextUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
@Data
@Entity
@Table(name = "category")
public class Category implements Linkable, Serializable {

    private static final long serialVersionUID = 6549621729790810154L;

    @AttributeOverrides({
            @AttributeOverride(name = "authorLink", column = @Column(name = "author_link")),
            @AttributeOverride(name = "title", column = @Column(name = "title"))
    })
    @EmbeddedId
    CategoryId id;

    String annotation;
    @MapsId("authorLink")
    @ManyToOne
    @JsonIgnore
    Author author;
    @Enumerated(EnumType.STRING)
    Type type = Type.OTHER;
    @OneToMany(orphanRemoval = true, mappedBy = "category")
    @JsonIgnore
    List<Work> works = new LinkedList<>();
    @OneToMany(orphanRemoval = true, mappedBy = "category")
    @JsonIgnore
    List<Link> links = new LinkedList<>();
    String link;

    public Category() {
    }

    public Category(CategoryId categoryId) {
        id = categoryId;
    }

    public List<Work> getWorks() {
        return works;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setTitle(String title) {
        if (title == null) return;
        title = TextUtils.trim(title);
        if (title.endsWith(":")) {
            title = title.substring(0, title.length() - 1);
        }
        if(id != null)
        this.id.title = title;
    }

    @Transient
    public String getLink() {
        if (link != null && !link.contains(author.getLink())) {
            link = author.getLink() + "/" + link;
        }
        return link;
    }

    public void addLink(Linkable linkable) {
        if (linkable instanceof Work) {
            this.getWorks().add((Work) linkable);
        }
        if (linkable instanceof Link) {
            this.getLinks().add((Link) linkable);
        }
    }

    @Transient
    @JsonIgnore
    public Linkable getLinkable() {
        if (Type.OTHER.equals(type)) {
            String title = id == null ? "" : id.title;
            if (link == null) return new Link(title, "", annotation);
            else
                return new Link(title, getLink(), annotation);
        } else {
            return type;
        }
    }

    @Transient
    @JsonIgnore
    public List<Linkable> getLinkables() {
        List<Linkable> linkables = new ArrayList<>();
        linkables.addAll(getWorks());
        linkables.addAll(getLinks());
        return linkables;
    }


    public String getTitle() {
        return getLinkable().getTitle();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return isTitleEquals(this, category) && isLinkEquals(this, category);
    }

    @Override
    public int hashCode() {
        String title = id == null ? "" : id.title;
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (link != null ? link.hashCode() : 0);
        return result;
    }

    public static boolean isTitleEquals(Category one, Category two) {
        if (one.getTitle() == null && two.getTitle() == null) {
            return true;
        }
        if (one.getTitle() == null || two.getTitle() == null) {
            return false;
        }
        return TextUtils.trim(one.getTitle()).equalsIgnoreCase(TextUtils.trim(two.getTitle()));
    }

    public static boolean isLinkEquals(Category one, Category two) {
        if (one.getLink() == null && two.getLink() == null) {
            return true;
        }
        if (one.getLink() == null || two.getLink() == null) {
            return false;
        }
        return TextUtils.trim(one.getLink()).equalsIgnoreCase(TextUtils.trim(two.getLink()));
    }

    @Transient
    public boolean isHasUpdates() {
        for (Work work : getWorks()) {
            if(work.isChanged() || (work.getSizeDiff() != null && work.getSizeDiff() > 0)) {
                return true;
            }
        }
        return false;
    }
}
