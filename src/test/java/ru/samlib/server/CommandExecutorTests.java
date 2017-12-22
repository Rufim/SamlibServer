package ru.samlib.server;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.yaml.snakeyaml.Yaml;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.*;
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
public class CommandExecutorTests {

    public static final String TAG = ParserTests.class.getSimpleName();
    private File logTestFile;

    @Autowired
    private CommandExecutorService executorService;
    @Autowired
    private MockRestServiceServer server;


    @Before
    public void setUp() throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        logTestFile = new File(classLoader.getResource("actual-log.txt").getFile());
        for (int i = 0; i < 100; i++) {
            this.server.expect(requestTo("/2017/06-07.log"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(readFile(logTestFile, "CP1251"), MediaType.TEXT_PLAIN));
        }
    }

    @Ignore
    @Test
    public void perfomanceTest() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.JUNE,7);
        for (int i = 0; i < 100; i++) {
            executorService.parseLogDay(calendar.getTime());
        }
    }
}
