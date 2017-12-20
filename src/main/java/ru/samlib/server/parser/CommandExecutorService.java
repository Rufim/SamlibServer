package ru.samlib.server.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.*;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CommandExecutorService {

    private static final String TAG = CommandExecutorService.class.getSimpleName();
            /*
            http://samlib.ru/cgi-bin/areader?q=razdel&order=date&object=/s/saharow_w_i/
             */
    @Autowired
    private Constants constants;
    @Autowired
    private AuthorDao authorDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private WorkDao workDao;
    @Autowired
    private ParsingInfoDao infoDao;
    @Autowired
    private LogEventDao logEventDao;

    private final RestTemplate logTemplate;

    SimpleDateFormat urlLogDate = new SimpleDateFormat("/yyyy/MM-dd'.log'");


    public CommandExecutorService(RestTemplateBuilder restTemplateBuilder) {
        this.logTemplate = restTemplateBuilder.rootUri(Constants.Net.LOG_PATH)
                .setConnectTimeout(15000)
                .setReadTimeout(15000)
                .build();
    }

    @Scheduled(cron = "*/30 * * * * *")  //2 реквеста в минуту
    public void scheduledExecution() {
        synchronized (CommandExecutorService.class) {
            Calendar calendar = Calendar.getInstance();
            Date lastParsedDay;
            ParsingInfo info = infoDao.findFirstByParsedTrueOrderByLogDateDesc();
            if (info != null) {
                lastParsedDay = info.getLogDate();
            } else {
                try {
                    lastParsedDay = new SimpleDateFormat("yyyy-MM-dd").parse(constants.getFirstLogDay());
                } catch (Exception e) {
                    LogEvent event = new LogEvent();
                    event.setCorruptedData("Cannot found last parse log day");
                    logEventDao.save(event);
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }
            }
            Calendar dayToParse = Calendar.getInstance();
            dayToParse.setTime(lastParsedDay);
            dayToParse.add(Calendar.DAY_OF_YEAR, 1);
            int daysParsed = 0;
            while ((calendar.get(Calendar.YEAR) > dayToParse.get(Calendar.YEAR)
                    || calendar.get(Calendar.DAY_OF_YEAR) > dayToParse.get(Calendar.DAY_OF_YEAR)) && daysParsed < constants.getLogsPerDay()) {
                parseLogDay(dayToParse.getTime());
                daysParsed++;
                dayToParse.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }


    public void parseLogDay(final Date logDay) {
        String url = Constants.Net.LOG_PATH + urlLogDate.format(logDay);
        synchronized (url.intern()) {
            long time = System.currentTimeMillis();
            ParsingInfo info = new ParsingInfo(logDay, url);
            info = infoDao.saveAndFlush(info);
            final Parser parser = new Parser(info, logEventDao);
            addLog(Log.LOG_LEVEL.INFO, null, "Start parse. Url=" + url, info);
            List<DataCommand> result = logTemplate.execute(url, HttpMethod.GET, null, new ResponseExtractor<List<DataCommand>>() {
                @Override
                public List<DataCommand> extractData(ClientHttpResponse response) throws IOException {
                    return parser.parseInput(response.getBody());
                }
            });
            for (DataCommand dataCommand : result) {
                executeCommand(dataCommand, info);
            }
            long processTime = System.currentTimeMillis() - time;
            addLog(Log.LOG_LEVEL.INFO, null, "End parse. Url=" + url + " commands=" + result.size() + " time=" + String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(processTime),
                    TimeUnit.MILLISECONDS.toSeconds(processTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(processTime))
            ), info);
            info.setParsed(true);
            info.setWithoutExceptions(info.getLogEvents().size() == 0);
            infoDao.saveAndFlush(info);
        }
    }


    public void executeCommand(DataCommand dataCommand, ParsingInfo info) {
        try {
            if (dataCommand != null && TextUtils.notEmpty(dataCommand.link)) {
                String link = dataCommand.link;
                if(dataCommand.getTitle() != null && dataCommand.getTitle().length() > 250) {
                    dataCommand.setTitle(dataCommand.getTitle().substring(0,250) + "...");
                }
                if (link.endsWith("/about") || link.endsWith("/")) {
                    Author newAuthor = new Author(link.substring(0, link.lastIndexOf("/") + 1));
                    newAuthor.setFullName(dataCommand.authorName);
                    if (link.endsWith("/")) {
                        newAuthor.setAbout(dataCommand.title);
                    }
                    if (link.endsWith("/about")) {
                        newAuthor.setAnnotation(dataCommand.annotation);
                    }
                    authorDao.save(newAuthor);
                } else {
                    Work oldWork = workDao.findOne(link);
                    Work newWork = new Work(dataCommand.link);
                    newWork.getAuthor().setFullName(dataCommand.authorName);
                    Category category = new Category();
                    category.setType(dataCommand.type);
                    category.setId(new CategoryId(newWork.getAuthor().getLink(), category.getTitle()));
                    category.setAuthor(newWork.getAuthor());
                    newWork.setCategory(category);
                    newWork.addGenre(dataCommand.genre);
                    newWork.setType(dataCommand.type);
                    newWork.setTitle(dataCommand.title);
                    newWork.setChangedDate(dataCommand.commandDate);
                    newWork.setAnnotation(dataCommand.annotation);
                    newWork.setCreateDate(dataCommand.createDate);
                    newWork.setSize(dataCommand.size);
                    if (oldWork != null) {
                        newWork.setUpdateDate(oldWork.getUpdateDate());
                    } else {
                        newWork.setUpdateDate(dataCommand.createDate);
                    }
                    if (newWork.getSize() == null && oldWork != null) {
                        newWork.setSize(oldWork.getSize());
                    }
                    if (newWork.getCreateDate() == null && oldWork != null) {
                        newWork.setCreateDate(oldWork.getCreateDate());
                    }
                    newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                    switch (dataCommand.getCommand()) {
                        case EDT:
                        case RPL:
                        case REN:
                        case UNK:
                            if (oldWork != null) {
                                newWork.setActivityCounter(oldWork.getActivityCounter());
                            }
                            authorDao.save(newWork.getAuthor());
                            categoryDao.save(newWork.getCategory());
                            workDao.save(newWork);
                            break;
                        case NEW:
                        case TXT:
                            if (oldWork != null) {
                                newWork.setActivityCounter(oldWork.getActivityCounter() + 1);
                            }
                            newWork.setUpdateDate(dataCommand.commandDate);
                            newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                            authorDao.save(newWork.getAuthor());
                            categoryDao.save(newWork.getCategory());
                            workDao.save(newWork);
                            break;
                        case DEL:
                            if (oldWork != null) {
                                workDao.delete(oldWork);
                            }
                            break;
                    }
                }
            }
        } catch (Exception ex) {
            addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error by command - " + dataCommand, info);
        }
    }

    private void addLog(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData, ParsingInfo info) {
        Log.saveLogEvent(logLevel, ex, corruptedData, logEventDao, info);
    }
}
