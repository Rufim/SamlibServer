package ru.samlib.server;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.samlib.server.util.SystemUtils.readFile;

@RunWith(SpringRunner.class)
@RestClientTest(value =  CommandExecutorService.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureDataJpa
public class ParserTests {

    public static final String TAG = ParserTests.class.getSimpleName();
    private File logTestFile;

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
    public void setUp() throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        logTestFile = new File(classLoader.getResource("test-log.txt").getFile());
        this.server.expect(requestTo("/2015/06-30.log"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(readFile(logTestFile, "CP1251"), MediaType.TEXT_PLAIN));
    }

    @Test
    public void parseTestFile() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2015);
        ParsingInfo info = new ParsingInfo(calendar.getTime(), Constants.Net.LOG_PATH + "/2015/06-30.log");
        List<DataCommand> result;
        try (FileInputStream inputStream = new FileInputStream(logTestFile)) {
            Parser parser = new Parser(info, logEventDao);
            result = parser.parseInput(inputStream);
            info = parser.getInfo();
        }
        assertEquals(10, result.size());
        for (DataCommand dataCommand : result) {
            executorService.executeCommand(dataCommand, info);
        }
        assertTrue(info.getLogEvents().isEmpty());
        List<Author> authors = (List<Author>) authorDao.findAll();
        List<Category> categories = (List<Category>) categoryDao.findAll();
        List<Work> works = (List<Work>) workDao.findAll();
        assertEquals(5, authors.size());
        assertEquals(3, categories.size());
        assertEquals(2, works.size());
    }

    @Test
    public void testScheduler() {
        executorService.scheduledExecution();
    }
}
