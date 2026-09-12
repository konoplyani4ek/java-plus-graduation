package ewm.request.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"ewm.user.client", "ewm.event.client"})
public class RequestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestServerApplication.class, args);
    }
}