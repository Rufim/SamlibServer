package ru.samlib.server.domain.entity;

import lombok.Data;
import org.hibernate.annotations.SortComparator;

import javax.persistence.*;
import java.util.Date;
import java.util.SortedSet;

@Data
@Entity
@Table(name = "parsing_info")
public class ParsingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id = 0L;
    private Date parseDate;
    private Date logDate;
    private boolean parsed = false;
    private boolean withoutExceptions = true;
    @OneToMany
    @OrderBy("time DESC")
    private SortedSet<LogEvent> logEvents;
}
