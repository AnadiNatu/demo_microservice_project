package com.microservice_demo.auth_service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
@Slf4j
public class LoadBalancerConfig {

    @Bean
    public ServiceInstanceListSupplier staticInstanceListSupplier(){
        log.info("[LoadBalancer] Registering static service instances for 'multi-user-security-demo'");

        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "multi-user-security-demo";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(
                        new DefaultServiceInstance("instance-1" , getServiceId() , "localhost" , 8080 , false),
                        new DefaultServiceInstance("instance-2" , getServiceId() , "localhost" , 8081 , false)
                ));
            }
        };
    }
}
