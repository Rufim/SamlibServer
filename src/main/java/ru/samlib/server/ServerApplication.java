package ru.samlib.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.samlib.server.domain.Constants;
import ru.samlib.server.util.Log;
import ru.samlib.server.util.TextUtils;

@SpringBootApplication
public class ServerApplication  {

	@Autowired
	Environment environment;

	public static void main(String[] args)  {
		SpringApplication.run(ServerApplication.class, args);
	}

	// on app startup
	@EventListener(ContextRefreshedEvent.class)
	public void contextRefreshedEvent() {
		String logLevel = environment.getProperty("logging.level.ru.samlib.server");
		if(TextUtils.notEmpty(logLevel)) {
			try {
				Log.setLogLevel(Log.LOG_LEVEL.valueOf(logLevel.toUpperCase()));
			} catch (IllegalArgumentException ignore) {
			}
		}
	}

	@Bean
	public Constants newConstants() {
		return new Constants();
	}


	@ConditionalOnProperty(value = "settings.server.scheduling", havingValue = "true", matchIfMissing = true)
	@Configuration
	@EnableScheduling
	public static class SchedulingConfiguration {

	}

}

