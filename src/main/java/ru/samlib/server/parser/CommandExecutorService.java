package ru.samlib.server.parser;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CommandExecutorService {

    public final static int MAX_LOGS_PER_DAY = 30;
    private static final String TAG = CommandExecutorService.class.getSimpleName();

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
        this.logTemplate = restTemplateBuilder.rootUri(Constants.Net.BASE_DOMAIN + "/" + Constants.Net.LOG_PATH)
                .setConnectTimeout(15000)
                .setReadTimeout(15000)
                .build();
    }

    @Scheduled(cron = "0 3 * * *")
    public void scheduledExecution() {
        Calendar calendar = Calendar.getInstance();
        Date lastParsedDay;
        ParsingInfo info = infoDao.findFirstByOrderByLogDateDesc();
        if(info != null) {
            lastParsedDay = info.getLogDate();
        } else {
            try {
                lastParsedDay = new SimpleDateFormat("yyyy-MM-dd").parse(constants.getFirstLogDay());
            } catch (Exception e) {
                LogEvent event = new LogEvent();
                event.setCorruptedData("Cannot found last parse log day");
                event.setTime(new Date());
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
                || calendar.get(Calendar.DAY_OF_YEAR) > dayToParse.get(Calendar.DAY_OF_YEAR)) && daysParsed <= MAX_LOGS_PER_DAY) {
            parseLogDay(dayToParse.getTime());
            daysParsed++;
            dayToParse.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    public void parseLogDay(final Date logDay) {
        List<DataCommand> result = logTemplate.execute(urlLogDate.format(logDay), HttpMethod.GET, null, new ResponseExtractor<List<DataCommand>>() {
            @Override
            public List<DataCommand> extractData(ClientHttpResponse response) throws IOException {
                Parser parser = new Parser(logDay);
                List<DataCommand> result = parser.parseInput(response.getBody());
                infoDao.save(parser.getInfo());
                return result;
            }
        });
        for (DataCommand dataCommand : result) {
            executeCommand(dataCommand);
        }
    }

    public void executeCommand(DataCommand dataCommand) {
        String link = dataCommand.link;
        if(link.endsWith("about")) {
            Work newWork = new Work(dataCommand.link);
            Author newAuthor = newWork.getAuthor();
            newAuthor.setFullName(dataCommand.authorName);
            newAuthor.setAnnotation(dataCommand.annotation);
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
            newWork.setChangedDate(dataCommand.commandDate);
            newWork.setAnnotation(dataCommand.annotation);
            newWork.setCreateDate(dataCommand.createDate);
            newWork.setSize(dataCommand.size);
            newWork.setUpdateDate(dataCommand.createDate);
            switch (dataCommand.getCommand()) {
                case EDT:
                case RPL:
                case REN:
                case UNK:
                    if (oldWork != null) {
                        newWork.setChangedDate(oldWork.getChangedDate());
                    } else {
                        newWork.getAuthor().setLastUpdateDate(newWork.getUpdateDate());
                    }
                    authorDao.save(newWork.getAuthor());
                    categoryDao.save(newWork.getCategory());
                    workDao.save(newWork);
                    break;
                case NEW:
                case TXT:
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




}
