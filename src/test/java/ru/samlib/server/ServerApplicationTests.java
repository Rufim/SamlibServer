package ru.samlib.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.samlib.server.domain.dao.AuthorDao;
import ru.samlib.server.domain.dao.ParsingInfoDao;
import ru.samlib.server.domain.entity.Author;
import ru.samlib.server.domain.entity.ParsingInfo;
import ru.samlib.server.parser.CommandExecutorService;

import java.util.Calendar;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ServerApplicationTests {

	@Autowired
	private ParsingInfoDao parsingInfoDao;
	@Autowired
	private CommandExecutorService executorService;
	@Autowired
	private AuthorDao authorDao;


	@Test
	public void contextLoads() {
	}


	@Test
	public void liveTestScheduler() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, -4);
		ParsingInfo info = new ParsingInfo();
		info.setParsed(true);
		info.setParseDate(calendar.getTime());
		info.setLogDate(calendar.getTime());
	    info.setParsed(true);
	    info.setWithoutExceptions(false);
		parsingInfoDao.save(info);
		executorService.scheduledLogParseExecution();
	}


	@Test
	public void liveTestStat() {
		Author sedrik = new Author(new Author("/s/sedrik/"));
		sedrik.setLastUpdateDate(new Date());
		authorDao.save(sedrik);
		executorService.scheduledAuthorUpdate();
	}

}
