package ru.samlib.server.parser;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.*;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

import javax.transaction.NotSupportedException;
import java.io.IOException;
import java.net.*;
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
        this.restTemplate = restTemplateBuilder.rootUri(Constants.Net.BASE_DOMAIN)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build()))
                .interceptors(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        HttpHeaders headers = request.getHeaders();
                        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        headers.add("User-Agent", "Mozilla/5.0 ");
                        headers.add("Accept-Encoding", "gzip, deflate");
                        return execution.execute(request, body);
                    }
                })
                .setConnectTimeout(15000)
                .setReadTimeout(15000)
                .build();
    }

    public Constants getConstants() {
        return constants;
    }

    @Scheduled(cron = "${settings.server.parse-log-cron}")  //4 реквеста в минуту
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

    @Scheduled(cron = "${settings.server.update-authors-cron}")  // 10 в минуту
    public void scheduledAuthorUpdate() {
        synchronized (CommandExecutorService.class) {
            Author author = authorDao.findFirstByMonthUpdateFiredFalseAndDeletedFalseOrderByLastUpdateDateDesc();
            if (author != null && parseAReaderAuthorLink(author.getLink())) {
                if (constants.isParseStat()) {
                    if(!parseStat(author.getLink())) {
                        author = authorDao.findOne(author.getLink());
                        author.setMonthUpdateFired(true);
                        authorDao.save(author);
                        authorDao.flush();
                    }
                }
            }
        }
    }

    public boolean parseStat(String link) {
        String url = Constants.Net.getStatPage(link);
        synchronized (url.intern()) {
            try {
                long time = System.currentTimeMillis();
                ParsingInfo info = new ParsingInfo(new Date(), url);
                infoDao.saveAndFlush(info);
                addLog(Log.LOG_LEVEL.INFO, null, "Start stat parse. Url=" + url, info);
                Map<String, String> stat = restTemplate.execute(url, HttpMethod.GET, null, new ResponseExtractor<Map<String, String>>() {
                    @Override
                    public Map<String, String> extractData(ClientHttpResponse response) throws IOException {
                        return Parser.parseStat(response.getBody());
                    }
                });
                if(workDao.updateStat(stat, link) < 0) return false;
                long processTime = System.currentTimeMillis() - time;
                addLog(Log.LOG_LEVEL.INFO, null, "End stat parse. Url=" + url + " stat size=" + stat.size() + " time=" + String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(processTime),
                        TimeUnit.MILLISECONDS.toSeconds(processTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(processTime))
                ), info);
                info.setParsed(true);
                info.setWithoutExceptions(info.getLogEvents().size() == 0);
                infoDao.saveAndFlush(info);
                return true;
            } catch (Exception ex) {
                if(ex instanceof HttpClientErrorException) {
                    if (((HttpClientErrorException) ex).getStatusCode() == HttpStatus.NOT_FOUND) {
                        if (pingHost(Constants.Net.BASE_HOST, 80, 6000)) {
                            Author author = authorDao.findOne(link);
                            author.setDeleted(true);
                            authorDao.save(author);
                            authorDao.flush();
                            addLog(Log.LOG_LEVEL.WARN, null, "Author not exist by link " + link, null);
                            return false;
                        }
                    }
                }
                addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error by link " + link, null);
            }
        }
        return false;
    }

    public static boolean pingHost(String host, int port, int timeout) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ex) {
                    //ignore;
                }
            }
        }
    }

    public boolean parseAReaderAuthorLink(final String link) {
        String url = Constants.Net.A_READER_QUERY + link;
        ParsingInfo info = new ParsingInfo(new Date(), url);
        return parseUrl(url, info, Parser.getAReaderDelegateInstance());
    }


    public boolean parseLogDay(final Date logDay) {
        String url = Constants.Net.LOG_PATH + urlLogDate.format(logDay);
        ParsingInfo info = new ParsingInfo(logDay, url);
        return parseUrl(url, info, Parser.getLogDelegateInstance());
    }

    @Transactional
    public boolean parseUrl(final String url, final ParsingInfo info, final Parser.ParseDelegate parseDelegate) {
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
                if(parseDelegate.getClass() == Parser.AReaderDelegate.class) {
                    List<String> persist = Stream.of(result).map(new Function<DataCommand, String>() {
                        @Override
                        public String apply(DataCommand value) {
                            return value.link;
                        }
                    }).collect(Collectors.toList());
                    workDao.deleteNotIn(persist, url.substring(url.lastIndexOf("=") + 1));
                }
                long processTime = System.currentTimeMillis() - time;
                addLog(Log.LOG_LEVEL.INFO, null, "End parse. Url=" + url + " commands=" + result.size() + " time=" + String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(processTime),
                        TimeUnit.MILLISECONDS.toSeconds(processTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(processTime))
                ), info);
                info.setParsed(true);
                info.setWithoutExceptions(info.getLogEvents().size() == 0);
                infoDao.saveAndFlush(info);
                return true;
            } catch (Exception ex) {
                addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error by utl - " + url, null);
            }
        }
        return false;
    }

    //TODO: HANDLE COMMAND !!! /f/fedotow_wdadimir_semenowich/|REN(f/fedotow_wladimir_semenowich)|2017-12-13 19:51:26|Ещё горит надежды свет|Федотов Вдадимир Семёнович|Нет|||05/12/2017|||
    public void executeCommand(DataCommand dataCommand, ParsingInfo info) {
        try {
            if (dataCommand != null && TextUtils.notEmpty(dataCommand.link)) {
                String link = dataCommand.link;
                if (dataCommand.getTitle() != null && dataCommand.getTitle().length() > 200) {
                    dataCommand.setTitle(dataCommand.getTitle().substring(0, 200) + "...");
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
                    if (dataCommand.command.equals(Command.DEL) && link.endsWith("/") && oldAuthor != null) {
                        authorDao.delete(oldAuthor);
                        return;
                    }
                    if (dataCommand.command.equals(Command.REN) && link.endsWith("/") && oldAuthor != null) {
                        authorDao.changeAuthorLink(oldAuthor, dataCommand.title);
                        return;
                    }
                    newAuthor.setFullName(dataCommand.authorName);
                    if (link.endsWith("/about")) {
                        newAuthor.setAnnotation(dataCommand.annotation);
                    } else if (link.endsWith("/")) {
                        newAuthor.setAbout(dataCommand.title);
                    }
                    if (newAuthor.getLastUpdateDate() == null) {
                        newAuthor.setLastUpdateDate(constants.firstLogDay());
                    }
                    authorDao.save(newAuthor);
                } else {
                    Work oldWork = workDao.findOne(link);
                    if (Command.DEL.equals(dataCommand.command)) {
                        if (oldWork != null) {
                            workDao.delete(oldWork);
                        }
                        return;
                    }
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
                        if(oldWork != null && oldWork.getSize() != null && !oldWork.getSize().equals(dataCommand.size)) newWork.setSizeDiff(dataCommand.size - oldWork.getSize());
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
                    }
                }
            }
        } catch (Exception ex) {
            addLog(Log.LOG_LEVEL.ERROR, ex, "Unexpected error by command - " + dataCommand, info);
        }
    }

    private void updateDate(Work newWork) {
        if (newWork.getUpdateDate() != null && (newWork.getAuthor().getLastUpdateDate() == null ||  newWork.getAuthor().getLastUpdateDate().before(newWork.getUpdateDate()))) {
            newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
        }
    }

    private void addLog(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData, ParsingInfo info) {
        Log.saveLogEvent(logLevel, ex, corruptedData, logEventDao, info);
    }
}
