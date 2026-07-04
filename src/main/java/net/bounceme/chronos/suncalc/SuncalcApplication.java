package net.bounceme.chronos.suncalc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ImportResource;

import net.bounceme.chronos.notifications.services.NotificationService;

@SpringBootApplication(exclude={BatchAutoConfiguration.class})
@ImportResource({
    "classpath:applicationContext.xml",
	"classpath:importTimes.xml",
    "classpath:flow-importTimes.xml",
    "classpath:flow-recalculateDifferences.xml"
})
@EnableFeignClients
public class SuncalcApplication implements CommandLineRunner {

	@Autowired
	private NotificationService notificationService;

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(SuncalcApplication.class);
		builder.headless(false);
		builder.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		notificationService.sendNotification("suncalc-batch", "Proceso automatizado iniciado correctamente", "OK");
	}
}
