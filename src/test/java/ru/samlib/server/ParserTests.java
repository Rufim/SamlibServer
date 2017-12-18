package ru.samlib.server;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.AuthorDao;
import ru.samlib.server.domain.dao.CategoryDao;
import ru.samlib.server.domain.dao.ParsingInfoDao;
import ru.samlib.server.domain.dao.WorkDao;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.parser.CommandExecutorService;
import ru.samlib.server.parser.DataCommand;
import ru.samlib.server.parser.Parser;
import ru.samlib.server.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ParserTests {

    public static final String TAG = ParserTests.class.getSimpleName();
    private File logTestFile;

    @Autowired
    private CommandExecutorService executorService;
    @Autowired
    private ParsingInfoDao parsingInfoDao;
    @Autowired
    private AuthorDao authorDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private WorkDao workDao;


    @Before
    public void loadTestFile() {
        ClassLoader classLoader = getClass().getClassLoader();
        logTestFile = new File(classLoader.getResource("test-log.txt").getFile());
    }

    @Test
    public void parseTestFile() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2015);
        ParsingInfo info;
        List<DataCommand> result;
        try (FileInputStream inputStream = new FileInputStream(logTestFile)) {
            Parser parser = new Parser(calendar.getTime());
            result = parser.parseInput(inputStream);
            info = parser.getInfo();
        }
        assertTrue(info != null && info.isWithoutExceptions());
        assertEquals(10, result.size());
        for (DataCommand dataCommand : result) {
            executorService.executeCommand(dataCommand);
        }
        List<Author> authors = (List<Author>) authorDao.findAll();
        List<Category> categories = (List<Category>) categoryDao.findAll();
        List<Work> works = (List<Work>) workDao.findAll();
        assertEquals(5, authors.size());
        assertEquals(3, categories.size());
        assertEquals(2, works.size());
    }





}
