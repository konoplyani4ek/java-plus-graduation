package ewm.additional.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"ewm.event.client"})
public class AdditionalServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdditionalServerApplication.class, args);
    }
}