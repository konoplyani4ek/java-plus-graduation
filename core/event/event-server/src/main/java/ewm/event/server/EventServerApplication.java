package ewm.event.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {"ewm.event.server", "ewm.stat.client"})
@EnableFeignClients(basePackages = {
        "ewm.user.client",
        "ewm.request.client",
        "ewm.category.client",
        "ewm.place.client"
})
public class EventServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServerApplication.class, args);
    }
}