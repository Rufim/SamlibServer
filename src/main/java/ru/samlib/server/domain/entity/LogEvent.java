package ru.samlib.server.domain.entity;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.samlib.server.util.Log;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "log_event")
public class LogEvent implements Comparable<LogEvent> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id = 0L;
    private Date time;
    @Column(columnDefinition = "TEXT")
    private String trace;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(columnDefinition = "TEXT")
    private String corruptedData;
    @Enumerated(EnumType.STRING)
    private Log.LOG_LEVEL logLevel;
    @ManyToOne
    private ParsingInfo parsingInfo;

    @Override
    public int compareTo(@NotNull LogEvent o) {
        return -time.compareTo(o.time);
    }
}
