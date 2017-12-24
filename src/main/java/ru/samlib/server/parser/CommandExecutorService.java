package ru.samlib.server.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.*;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
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

    private final RestTemplate restTemplate;

    SimpleDateFormat urlLogDate = new SimpleDateFormat("/yyyy/MM-dd'.log'");


    public CommandExecutorService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.rootUri(Constants.Net.LOG_PATH)
                .setConnectTimeout(15000)
                .setReadTimeout(15000)
                .build();
    }

    public Constants getConstants() {
        return constants;
    }

    //@Scheduled(cron = "*/15 * * * * *")  //4 реквеста в минуту
    @Scheduled(fixedDelay = 15000)
    public void scheduledLogParseExecution() {
        synchronized (CommandExecutorService.class) {
            try {
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
            } catch (Exception ex) {
                addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error", null);
            }
        }
    }

    @Scheduled(fixedDelay = 6000)  // 10 в минуту
    public void scheduledAuthorParseExecution() {
        Author author = authorDao.findFirstByMonthUpdateFiredFalseOrderByLastUpdateDateDesc();
        if (author != null) {
            parseAReaderAuthorLink(author.getLink());
            if (constants.isParseStat()) {
                parseStat(author.getLink());
            }
        }
    }

    public void parseStat(String link) {
        String url = Constants.Net.getStatPage(link);
        synchronized (url.intern()) {
            try {
                long time = System.currentTimeMillis();
                ParsingInfo info = new ParsingInfo(new Date(), url);
                infoDao.saveAndFlush(info);
                addLog(Log.LOG_LEVEL.INFO, null, "Start stat parse. Url=" + url, info);
                Map<String, Integer> stat = restTemplate.execute(url, HttpMethod.GET, null, new ResponseExtractor<Map<String, Integer>>() {
                    @Override
                    public Map<String, Integer> extractData(ClientHttpResponse response) throws IOException {
                        return Parser.parseStat(response.getBody());
                    }
                });
                workDao.updateStat(stat, link);
                long processTime = System.currentTimeMillis() - time;
                addLog(Log.LOG_LEVEL.INFO, null, "End stat parse. Url=" + url + " stat size=" + stat.size() + " time=" + String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(processTime),
                        TimeUnit.MILLISECONDS.toSeconds(processTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(processTime))
                ), info);
                info.setParsed(true);
                info.setWithoutExceptions(info.getLogEvents().size() == 0);
                infoDao.saveAndFlush(info);
            } catch (Exception ex) {
                addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error", null);
            }
        }
    }

    public void parseAReaderAuthorLink(final String link) {
        String url = Constants.Net.A_READER_QUERY + link;
        ParsingInfo info = new ParsingInfo(new Date(), url);
        parseUrl(url, info, Parser.getAReaderDelegateInstance());
    }


    public void parseLogDay(final Date logDay) {
        String url = Constants.Net.LOG_PATH + urlLogDate.format(logDay);
        ParsingInfo info = new ParsingInfo(logDay, url);
        parseUrl(url, info, Parser.getLogDelegateInstance());
    }

    @Transactional
    public void parseUrl(final String url, final ParsingInfo info, final Parser.ParseDelegate parseDelegate) {
        synchronized (url.intern()) {
            try {
                long time = System.currentTimeMillis();
                infoDao.saveAndFlush(info);
                final Parser parser = new Parser(info, logEventDao);
                addLog(Log.LOG_LEVEL.INFO, null, "Start parse. Url=" + url, info);
                List<DataCommand> result = restTemplate.execute(url, HttpMethod.GET, null, new ResponseExtractor<List<DataCommand>>() {
                    @Override
                    public List<DataCommand> extractData(ClientHttpResponse response) throws IOException {
                        return parser.parseInput(response.getBody(), parseDelegate);
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
            } catch (Exception ex) {
                addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error", null);
            }
        }
    }


    public void executeCommand(DataCommand dataCommand, ParsingInfo info) {
        try {
            if (dataCommand != null && TextUtils.notEmpty(dataCommand.link)) {
                String link = dataCommand.link;
                if (dataCommand.getTitle() != null && dataCommand.getTitle().length() > 250) {
                    dataCommand.setTitle(dataCommand.getTitle().substring(0, 250) + "...");
                }
                if (link.endsWith("/about") || link.endsWith("/")) {
                    link = link.substring(0, link.lastIndexOf("/") + 1);
                    Author oldAuthor = authorDao.findOne(link);
                    Author newAuthor;
                    if (oldAuthor != null) {
                        newAuthor = oldAuthor;
                    } else {
                        newAuthor = new Author(link);
                    }
                    newAuthor.setFullName(dataCommand.authorName);
                    if (link.endsWith("/")) {
                        newAuthor.setAbout(dataCommand.title);
                    }
                    if (link.endsWith("/about")) {
                        newAuthor.setAnnotation(dataCommand.annotation);
                    }
                    if (newAuthor.getLastUpdateDate() == null) {
                        newAuthor.setLastUpdateDate(constants.firstLogDay());
                    }
                    authorDao.save(newAuthor);
                } else {
                    Work oldWork = workDao.findOne(link);
                    Work newWork;
                    if (oldWork != null) {
                        newWork = oldWork;
                    } else {
                        newWork = new Work(dataCommand.link);
                    }
                    newWork.getAuthor().setFullName(dataCommand.authorName);
                    Category newCategory = new Category();
                    newCategory.setType(dataCommand.type);
                    CategoryId id = new CategoryId(newWork.getAuthor().getLink(), newCategory.getTitle());
                    if (oldWork == null || oldWork.getCategory() == null || !oldWork.getCategory().getId().equals(id)) {
                        newCategory.setId(id);
                        newCategory.setAuthor(newWork.getAuthor());
                        newWork.setCategory(newCategory);
                    }
                    if (!dataCommand.command.equals(Command.ARD) || oldWork == null) {
                        ArrayList genres = new ArrayList(1);
                        genres.add(dataCommand.genre);
                        newWork.setGenres(genres);
                    }
                    newWork.setType(dataCommand.type);
                    newWork.setTitle(dataCommand.title);
                    if (!dataCommand.command.equals(Command.ARD)) {
                        newWork.setChangedDate(dataCommand.commandDate);
                    } else if (oldWork == null) {
                        newWork.setChangedDate(dataCommand.createDate);
                    }
                    newWork.setAnnotation(dataCommand.annotation);
                    newWork.setCreateDate(dataCommand.createDate);
                    newWork.setWorkAuthorName(dataCommand.authorName);
                    if (oldWork == null) {
                        newWork.setUpdateDate(dataCommand.createDate);
                        newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                    }
                    if (dataCommand.createDate != null) {
                        newWork.setCreateDate(dataCommand.createDate);
                    }
                    if (dataCommand.size != null) {
                        if (oldWork != null && !dataCommand.command.equals(Command.ARD) && dataCommand.size > oldWork.getSize()) {
                            int activityWeight = (dataCommand.size - oldWork.getSize()); // 20k
                            if (oldWork.getUpdateDate() == null) {
                                activityWeight = 1;
                            } else {
                                int days = (int) TimeUnit.MILLISECONDS.toDays(info.getLogDate().getTime() - oldWork.getUpdateDate().getTime());
                                if (days <= 0) {
                                    if (days < 0) activityWeight = 0;
                                    days = 1;
                                }
                                activityWeight = (int) (((double) activityWeight) / days);
                                if (activityWeight < 1) {
                                    activityWeight = 1;
                                }
                            }
                            newWork.setActivityIndex(oldWork.getActivityIndex() + activityWeight);
                            newWork.setUpdateDate(dataCommand.commandDate);
                        }
                        newWork.setSize(dataCommand.size);
                    }
                    switch (dataCommand.getCommand()) {
                        case ARD:
                            newWork.setRate(dataCommand.rate);
                            newWork.setVotes(dataCommand.votes);
                        case EDT:
                        case RPL:
                        case REN:
                        case UNK:
                            if (oldWork != null) {
                                newWork.setActivityIndex(oldWork.getActivityIndex());
                            }
                            updateDate(newWork);
                            workDao.saveWork(newWork, oldWork != null);
                            break;
                        case NEW:
                        case TXT:
                            newWork.setUpdateDate(dataCommand.commandDate);
                            updateDate(newWork);
                            workDao.saveWork(newWork, oldWork != null);
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

    private void updateDate(Work newWork) {
        if (newWork.getAuthor().getLastUpdateDate() == null || newWork.getAuthor().getLastUpdateDate().before(newWork.getUpdateDate())) {
            newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
        }
    }

    private void addLog(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData, ParsingInfo info) {
        Log.saveLogEvent(logLevel, ex, corruptedData, logEventDao, info);
    }
}
