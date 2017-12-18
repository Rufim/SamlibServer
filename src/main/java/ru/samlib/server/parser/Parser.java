package ru.samlib.server.parser;

import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.entity.Genre;
import ru.samlib.server.domain.entity.LogEvent;
import ru.samlib.server.domain.entity.ParsingInfo;
import ru.samlib.server.domain.entity.Type;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;


public class Parser {
    private static final String TAG = "Parser";

    private boolean logEvents;
    private ParsingInfo info;
    private SortedSet<LogEvent> events;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat(Constants.Pattern.DATA_TIME_PATTERN);
    private SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN);


    public Parser(Date logDay) {
        this(logDay, true);
    }

    public Parser(Date logDay, boolean logEvents) {
        this.logEvents = logEvents;
        info = new ParsingInfo();
        info.setLogDate(logDay);
        info.setParseDate(new Date());
        info.setLogEvents(new TreeSet<>());
        events = info.getLogEvents();
    }

    public ParsingInfo getInfo() {
        return info;
    }

    public List<DataCommand> parseInput(InputStream inputStream) {
        ArrayList<DataCommand> dataCommands = new ArrayList<>();
        try (final InputStream is = inputStream;
             final InputStreamReader isr = new InputStreamReader(is, "CP1251");
             final BufferedReader reader = new BufferedReader(isr)) {
            String line = "";
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.charAt(0) == '/') {
                    String fullLine = TextUtils.trim(builder.toString());
                    addLine(fullLine, dataCommands);
                    builder = new StringBuilder();
                }
                builder.append(line);
            }
            addLine(builder.toString(), dataCommands);
        } catch (Exception e) {
            addLog(Log.LOG_LEVEL.ERROR, e, "Error while read log input");
        }
        info.setParsed(true);
        info.setWithoutExceptions(info.getLogEvents().size() == 0);
        return dataCommands;
    }


    private void addLine(String fullLine, List<DataCommand> dataCommands) {
        if (TextUtils.notEmpty(fullLine)) {
            if (fullLine.startsWith("/") && fullLine.endsWith("k")) {
                dataCommands.add(parseLine(fullLine));
            } else {
                addLog(Log.LOG_LEVEL.WARN, new Exception("Invalid line"), fullLine);
            }
        }
    }

    //     0         1            2             3     4      5    6    7     8     9       10             11
    // имя файла|тег oперации|таймштамп-MySQL|title|author|type|janr|annot|date|img_cnt|update-unixtime|size kb
    // /m/maksimowa_alina/dymchatyjsiluettebja|EDT|2015-06-30 20:16:07|Дымчатый силуэт тебя|Максимова Алина|Роман|Фантастика|       Новенький баскетболист в университете произвёл на Софию особое впечатление. Да и Дашке он приглянулся. Что делать, |02/06/2015|1|1433273100|695k
    private DataCommand parseLine(String line) {
        if (TextUtils.notEmpty(line)) {
            try {
                String[] fields = line.split("\\|");
                DataCommand dataCommand = new DataCommand();
                dataCommand.setLink(fields[0]);
                String command = fields[1];
                String workName = fields[3];
                if (TextUtils.notEmpty(fields[1])) {
                    try {
                        if (command.length() > 3) {
                            dataCommand.setCommand(Command.valueOf(fields[1].substring(0, 3)));
                            workName = fields[1].substring(4, fields[1].length());
                        } else {
                            dataCommand.setCommand(Command.valueOf(fields[1]));
                        }
                    } catch (IllegalArgumentException ex) {
                        addLog(Log.LOG_LEVEL.WARN, new Exception("Invalid command - " + fields[1]), line);
                    }
                } else {
                    addLog(Log.LOG_LEVEL.WARN, new Exception("Empty command"), line);
                }
                dataCommand.setWorkName(workName);
                if (TextUtils.notEmpty(fields[2])) dataCommand.setCommandDate(dateTimeFormat.parse(fields[2]));
                dataCommand.setAuthorName(fields[4]);
                dataCommand.setType(Type.parseType(fields[5]));
                dataCommand.setGenre(Genre.parseGenre(fields[6]));
                dataCommand.setAnnotation(fields[7]);
                if (TextUtils.notEmpty(fields[8])) dataCommand.setCreateDate(dateFormat.parse(fields[8]));
                dataCommand.setImageCount(fields[9].isEmpty() ? 0 : Integer.parseInt(fields[9]));
                dataCommand.setUnixtime(Long.parseLong(fields[10]));
                if (TextUtils.notEmpty(fields[11]))
                    dataCommand.setSize(Integer.parseInt(fields[11].substring(0, fields[11].length() - 1)));
                return dataCommand;
            } catch (Exception ex) {
                addLog(Log.LOG_LEVEL.ERROR, ex, line);
            }
        }
        return null;
    }


    private void addLog(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData) {
        if (logEvents) {
            events.add(Log.generateLogEvent(logLevel, ex, corruptedData, info));
        }
    }

}
