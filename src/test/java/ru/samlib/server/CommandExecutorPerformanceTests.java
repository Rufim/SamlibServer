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
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.FileSystemUtils;
import org.yaml.snakeyaml.Yaml;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.domain.dao.*;
import ru.samlib.server.domain.entity.*;
import ru.samlib.server.parser.CommandExecutorService;
import ru.samlib.server.parser.DataCommand;
import ru.samlib.server.parser.Parser;
import ru.samlib.server.util.SystemUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class CommandExecutorPerformanceTests {

    public static final String TAG = ParserTests.class.getSimpleName();
    private File logTestFile;

    @Autowired
    private CommandExecutorService executorService;
    @Autowired
    private MockRestServiceServer server;


    @Before
    public void setUp() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        logTestFile = new File(classLoader.getResource("actual-log.txt").getFile());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SystemUtils.readStream(new FileInputStream(logTestFile), os, new byte[4000]);
        this.server.expect(ExpectedCount.manyTimes(), requestTo("/2017/06-07.log"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(os.toByteArray(), MediaType.TEXT_PLAIN));
    }


    @Ignore
    @Test
    public void performanceTest() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.JUNE,7);
        for (int i = 0; i < 100; i++) {
            executorService.parseLogDay(calendar.getTime());
        }
    }
}
