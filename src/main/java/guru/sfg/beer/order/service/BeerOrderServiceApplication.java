package guru.sfg.beer.order.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeerOrderServiceApplication {

    // We verify all services - running against local MySQL + using ActiveMQ running inside a Docker Container, cross microservice test.
    // mssc-beer-order-service, mssc-beer-inventory-service + mssc-beer-service - active profile localmysql > start all services.
    // mssc-beer-order-service - our TastingRoomService is sending out orders periodically.
    // mssc-beer-inventory-service - has log chatter from allocation activities.
    public static void main(String[] args) {
        SpringApplication.run(BeerOrderServiceApplication.class, args);
    }
}
