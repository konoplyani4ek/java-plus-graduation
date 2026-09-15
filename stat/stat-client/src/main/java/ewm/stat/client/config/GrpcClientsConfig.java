package ewm.stat.client.config;

import ewm.stat.client.grpc.AnalyzerClient;
import ewm.stat.client.grpc.AnalyzerClientImpl;
import ewm.stat.client.grpc.CollectorClient;
import ewm.stat.client.grpc.CollectorClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientsConfig {

    @Bean
    public CollectorClient collectorClient() {
        return new CollectorClientImpl();
    }

    @Bean
    public AnalyzerClient analyzerClient() {
        return new AnalyzerClientImpl();
    }
}