package ewm.stat.client.config;

import ewm.stat.client.StatClient;
import ewm.stat.client.StatClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatClientConfig {

    @Bean
    public StatClient statClient(@Value("${spring.application.name}") String app,
                                 @Value("${stat-server.service-id:stat-server}") String serviceId,
                                 @Value("${stat-client.timeout-ms:0}") int timeoutMs,
                                 DiscoveryClient discoveryClient) {

        return new StatClientImpl(app, serviceId, discoveryClient, timeoutMs);
    }

}