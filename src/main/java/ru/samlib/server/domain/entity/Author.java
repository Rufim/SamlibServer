package ru.samlib.server.domain.entity;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.samlib.server.domain.Linkable;
import ru.samlib.server.domain.Validatable;
import ru.samlib.server.util.TextUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;


/**
 * Created by Rufim on 22.05.2014.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "author")
public class Author implements Serializable, Linkable, Validatable {

    private static final long serialVersionUID = -2312409864781561240L;

    private static final String AVATAR = ".photo2.jpg";

    @Id
    String link;
    String fullName;
    String shortName;
    String email;
    @Column(columnDefinition = "TEXT")
    String annotation;
    @Enumerated(EnumType.STRING)
    @Access(AccessType.PROPERTY)
    Gender gender;
    Date dateBirth;
    String address;
    String authorSiteUrl;
    boolean hasAvatar = false;
    boolean hasAbout = false;
    boolean hasUpdates = false;
    boolean newest = false;
    boolean notNotified = false;
    boolean deleted = false;
    Date lastUpdateDate;
    Integer size;
    Integer workCount;
    BigDecimal rate;
    Integer kudoed;
    Integer views;
    String about;
    String sectionAnnotation;
    @OneToMany(orphanRemoval = true)
    List<Category> categories = new LinkedList<>();
    @OneToMany(orphanRemoval = true)
    List<Link> links = new LinkedList<>();
    @OneToMany(orphanRemoval = true)
    List<Work> works = new LinkedList<>();

    @Transient
    List<Author> friendList = new ArrayList<>();
    @Transient
    List<Author> friendOfList = new ArrayList<>();
    Integer friends;
    Integer friendsOf;


    public List<Work> getWorks() {
        return works;
    }

    public List<Link> getLinks() {
        return links;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Author() {
    }

    public Author(Author other) {
        this.link = other.getLink();
        this.fullName = other.getFullName();
        this.shortName = other.getShortName();
        this.email = other.getEmail();
        this.annotation = other.getAnnotation();
        this.gender = other.getGender();
        this.dateBirth = other.getDateBirth();
        this.address = other.getAddress();
        this.authorSiteUrl = other.getAuthorSiteUrl();
        this.hasAvatar = other.isHasAbout();
        this.hasAbout = other.isHasAbout();
        this.hasUpdates = other.isHasUpdates();
        this.lastUpdateDate = other.getLastUpdateDate();
        this.newest = other.isNewest();
        this.size = other.getSize();
        this.workCount = other.getWorkCount();
        this.rate = other.getRate();
        this.kudoed = other.getKudoed();
        this.views = other.getViews();
        this.about = other.getAbout();
        this.sectionAnnotation = other.sectionAnnotation;
        this.categories = other.getCategories();
        this.friendList = other.getFriendList();
        this.friendOfList = other.getFriendOfList();
        this.friends = other.getFriends();
        this.friendsOf = other.getFriendsOf();
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Author(String link) {
        this();
        setLink(link);
    }

    public void setLink(String link) {
        if (link == null) return;
        link = TextUtils.eraseHost(link);
        if (link.contains("/")) {
            if (link.startsWith(Work.COMMENT_PREFIX)) {
                link = link.replace(Work.COMMENT_PREFIX, "");
            }
            if (link.startsWith(Work.ILLUSTRATION_PREFIX)) {
                link = link.replace(Work.ILLUSTRATION_PREFIX, "");
            }
            if (link.contains(Work.HTML_SUFFIX)) {
                this.link = new Work(link).getAuthor().getLink();
            } else {
                this.link = link;
            }
        }
        if (this.link != null && !this.link.endsWith("/")) {
            this.link += "/";
        }
        this.link = this.link.replaceAll("/+", "/");
    }

    public Gender getGender() {
        if (gender != null) return gender;
        String lastName = null;
        if (fullName != null) {
            String names[] = fullName.split(" ");
            lastName = names[0];
        }
        if (shortName != null && lastName == null) {
            String names[] = shortName.split(" ");
            lastName = names[0];
        }
        gender = Gender.parseLastName(lastName);
        return gender;
    }

    public String getShortName() {
        if (shortName == null && fullName != null) {
            StringBuilder builder = new StringBuilder();
            String authors[] = fullName.split(",");
            for (int i = 0; i < authors.length; i++) {
                String names[] = authors[i].split(" ");
                builder.append(names[0]);
                for (int j = 1; j < names.length; j++) {
                    if (!names[j].isEmpty()) {
                        builder.append(" " + names[j].charAt(0) + ".");
                    }
                }
                if (i + 1 < authors.length) {
                    builder.append(",");
                }
            }
            return shortName = builder.toString();
        }
        return shortName;
    }

    @Transient
    public List<Category> getLinkableCategory() {
        return Stream.of(getCategories())
                .filter(sec -> sec.getLink() != null)
                .collect(Collectors.toList());
    }

    @Transient
    public List<Category> getStaticCategory() {
        return Stream.of(getCategories())
                .filter(sec -> sec.getLink() == null)
                .collect(Collectors.toList());
    }

    @Transient
    public List<Work> getUpdates() {
        List<Work> updates = new ArrayList<>();
        for (Category category : getCategories()) {
            for (Work work : category.getWorks()) {
                if (work.isChanged()) {
                    updates.add(work);
                }
            }
        }
        Collections.sort(updates, (o1, o2) -> {
            if (o1.getCachedDate() == null) {
                return -1;
            }
            return o1.getCachedDate().compareTo(o2.getCachedDate());
        });
        return updates;
    }

    public void addCategory(Category category) {
        this.getCategories().add(category);
    }

    public void addFriend(Author friend) {
        this.getFriendList().add(friend);
    }

    public void addFriendOf(Author friend) {
        this.getFriendOfList().add(friend);
    }

    public void addRecommendation(Work work) {
        Map<String, Work> all = getAllWorks();
        if (all.containsKey(work.getLink())) {
            all.get(work.getLink()).setRecommendation(true);
        } else {
            work.setRecommendation(true);
            work.setCategory(null);
            this.getWorks().add(work);
        }
    }

    @Transient
    public Map<String, Work> getAllWorks() {
        LinkedHashMap<String, Work> map = new LinkedHashMap<>();
        for (Work work : getWorks()) {
            map.put(work.getLink(), work);
        }
        for (Category category : getCategories()) {
            for (Work work : category.getWorks()) {
                map.put(work.getLink(), work);
            }
        }
        return map;
    }

    public void addRootLink(Linkable linkable) {
        if (linkable instanceof Work) {
            Work exist = Stream.of(getWorks()).filter(work -> work.equals(linkable)).findFirst().orElse(null);
            if (exist != null) {
                exist.setRootWork(true);
            } else {
                ((Work) linkable).setRootWork(true);
                this.getWorks().add((Work) linkable);
            }
        }
        if (linkable instanceof Link) {
            Link exist = Stream.of(getLinks()).filter(link -> link.equals(linkable)).findFirst().orElse(null);
            if (exist != null) {
                exist.setRootLink(true);
            } else {
                ((Link) linkable).setRootLink(true);
                this.getLinks().add((Link) linkable);
            }
        }
    }

    public List<Linkable> getLinkables() {
        List<Linkable> linkables = new ArrayList<>();
        if (getRootLinks() != null) {
            linkables.addAll(getRootWorks());
        }
        if (getRootWorks() != null) {
            linkables.addAll(getRootLinks());
        }
        return linkables;
    }

    @Transient
    public List<Link> getRootLinks() {
        return Stream.of(getLinks()).filter(Link::isRootLink).collect(Collectors.toList());
    }

    @Transient
    public List<Work> getRootWorks() {
        return Stream.of(getWorks()).filter(Work::isRootWork).collect(Collectors.toList());
    }

    @Override
    public boolean validate() {
        return (fullName != null || shortName != null) && link != null;
    }

    @Override
    public String toString() {
        return getShortName();
    }


    @Override
    public String getTitle() {
        return getShortName();
    }

    public String getImageLink() {
        return getFullLink() + AVATAR;
    }


    public boolean hasCategory(Category category) {
        for (Category next : getCategories()) {
            if (next.getTitle() != null ? !next.getTitle().equals(category.getTitle()) : category.getTitle() != null) continue;
            if (next.annotation != null ? !next.annotation.equals(category.annotation) : category.annotation != null)
                continue;
            if (next.author != null ? !next.author.equals(category.author) : category.author != null) continue;
            if (next.type != category.type) continue;
            if (!(next.link != null ? next.link.equals(category.link) : category.link == null)) {
                return true;
            }
        }
        return false;
    }

    public void hasNewUpdates() {
        setHasUpdates(true);
        setNotNotified(true);
    }

    public void setRootLinks(List<Link> rootLinks) {
        for (Link rootLink : rootLinks) {
            rootLink.setRootLink(true);
        }
        if (getLinks() == null) setLinks(new ArrayList<>());
        getLinks().addAll(rootLinks);
    }

    public void setRootWorks(List<Work> rootWorks) {
        for (Work rootLink : rootWorks) {
            rootLink.setRootWork(true);
        }
        if (getWorks() == null) setWorks(new ArrayList<>());
        getWorks().addAll(rootWorks);
    }

    public List<Work> getRecommendations() {
        return Stream.of(getAllWorks().entrySet()).map(Map.Entry::getValue).filter(Work::isRecommendation).collect(Collectors.toList());
    }
}
