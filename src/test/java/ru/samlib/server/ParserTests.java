package ru.samlib.server;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.AuthorDao;
import ru.samlib.server.domain.dao.CategoryDao;
import ru.samlib.server.domain.dao.LogEventDao;
import ru.samlib.server.domain.dao.WorkDao;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.parser.CommandExecutorService;
import ru.samlib.server.parser.DataCommand;
import ru.samlib.server.parser.Parser;
import ru.samlib.server.util.SystemUtils;

import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(value =  CommandExecutorService.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureDataJpa
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ParserTests {

    public static final String TAG = ParserTests.class.getSimpleName();
    private File logTestFile;
    private File aReaderTestFile;
    private File statFile;
    @Autowired
    private CommandExecutorService executorService;
    @Autowired
    private AuthorDao authorDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private WorkDao workDao;

    @Autowired
    private MockRestServiceServer server;
    @Autowired
    private LogEventDao logEventDao;


    @Before
    public void setUp() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        logTestFile = new File(classLoader.getResource("test-log.txt").getFile());
        /*try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(logTestFile)) {
            SystemUtils.readStream(fis, os, new byte[4000]);
            this.server.expect(requestTo("/2015/06-30.log"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(os.toByteArray(), MediaType.TEXT_PLAIN));
        }*/
        aReaderTestFile = new File(classLoader.getResource("areader-sedrik.txt").getFile());
        statFile = new File(classLoader.getResource("stat-page.txt").getFile());
    }

    @Test
    public void parseTestFile() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2015, Calendar.JUNE, 30,0,0,0);
        ParsingInfo info = new ParsingInfo(calendar.getTime(), Constants.Net.LOG_PATH + "/2015/06-30.log");
        List<DataCommand> result;
        try (FileInputStream inputStream = new FileInputStream(logTestFile)) {
            Parser parser = new Parser(info, logEventDao);
            result = parser.parseInput(inputStream, Parser.getLogDelegateInstance());
            info = parser.getInfo();
        }
        assertEquals(13, result.size());
        for (DataCommand dataCommand : result) {
            executorService.executeCommand(dataCommand, info);
        }
        assertTrue(info.getLogEvents().isEmpty());
        List<Author> authors = authorDao.findAll();
        List<Category> categories =  categoryDao.findAll();
        List<Work> works =  workDao.findAll();
        assertEquals(5, authors.size());
        assertEquals(3, categories.size());
        assertEquals(2, works.size());
        List<Work> workList = workDao.searchWorksByActivity("максимОва", Type.NOVEL, Genre.FICTION, null, null);
        assertEquals(1, workList.size());
        assertEquals(13, workList.get(0).getActivityIndex().intValue());
        workList = workDao.searchWorksByActivity("дьявол-Хранитель", Type.ARTICLE, Genre.PROSE, null, null);
        assertEquals(1, workList.size());
    }
    
    @Test
    public void parseAReaderResults() throws IOException {
        String link = "/s/sedrik/";
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(aReaderTestFile)) {
            SystemUtils.readStream(fis, os, new byte[4000]);
            this.server.expect(requestTo(Constants.Net.A_READER_QUERY + link))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(os.toByteArray(), MediaType.TEXT_PLAIN));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(statFile)) {
            SystemUtils.readStream(fis, os, new byte[4000]);
            this.server.expect(requestTo(Constants.Net.getStatPage(link)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(os.toByteArray(), MediaType.TEXT_PLAIN));
        }
        executorService.parseAReaderAuthorLink(link);
        executorService.parseStat(link);
        List<Author> authors = authorDao.findAll();
        List<Category> categories = categoryDao.findAll();
        List<Work> works = workDao.findAll();
        assertEquals(1, authors.size());
        assertEquals(3, categories.size());
        assertEquals(58, works.size());
        works = workDao.findFirst3ByOrderByViewsDesc();
        assertEquals(842222, works.get(0).getViews().intValue());
        assertEquals(521703, works.get(1).getViews().intValue());
        assertEquals(458748, works.get(2).getViews().intValue());
        assertEquals(7387861, authors.get(0).getViews().intValue());
        works = workDao.searchWorksByActivityNative("дрик", Type.ARTICLE, Genre.EMPTY, null, null);
        assertEquals(33, works.size());
        works = workDao.searchWorksByActivityNative("дрик", Type.HEAD, Genre.EMPTY, null, null);
        assertEquals(1, works.size());
        assertEquals(0, works.get(0).getActivityIndex().intValue());
        works = workDao.searchWorksByActivityNative("дрик", Type.STORY, Genre.EMPTY, null, null);
        assertEquals(1, works.size());
        assertEquals(0, works.get(0).getActivityIndex().intValue());
        works = workDao.searchWorksByActivityNative("Ракот", null, null, null, null);
        assertEquals(2, works.size());
        works = workDao.searchWorksByActivityNative("Rakot", null, null, null, null);
        assertEquals(8, works.size());
        works = workDao.searchWorksByActivityNative("Keelminir", null, null, null, null);
        assertEquals(2, works.size());
        works = workDao.searchWorksByActivityNative("омак", Type.HEAD, Genre.EMPTY, null, null);
        assertEquals(3, works.size());
    }

}
