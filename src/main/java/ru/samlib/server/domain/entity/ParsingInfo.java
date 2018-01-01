package ru.samlib.server.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Data
@Entity
@Table(name = "parsing_info")
@EqualsAndHashCode(exclude = "logEvents")
public class ParsingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id = 0L;
    private Date parseDate;
    private Date logDate;
    private boolean parsed = false;
    private String link;
    private boolean withoutExceptions = false;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parsingInfo")
    @OrderBy("time DESC")
    private Set<LogEvent> logEvents;

    public void addLogEvent(LogEvent logEvent) {
        if(logEvent != null) {
            if(logEvents == null) logEvents = new TreeSet<>();
            logEvents.add(logEvent);
        }
    }

    public ParsingInfo() {
    }

    public ParsingInfo(String link) {
        this.link = link;
        this.parseDate = new Date();
        this.logEvents = new TreeSet<>();
    }

    public ParsingInfo(Date logDate, String link) {
        this(link);
        this.logDate = logDate;
    }
}
