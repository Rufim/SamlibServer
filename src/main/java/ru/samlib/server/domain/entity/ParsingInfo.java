package ru.samlib.server.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;
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
    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("time DESC")
    private SortedSet<LogEvent> logEvents;

    public void addLogEvent(LogEvent logEvent) {
        if(logEvent != null) {
            if(logEvents == null) logEvents = new TreeSet<>();
            logEvents.add(logEvent);
        }
    }
}
