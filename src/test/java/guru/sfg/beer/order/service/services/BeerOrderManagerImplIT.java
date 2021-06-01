package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.brewery.model.BeerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.jgroups.util.Util.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by jt on 2/14/20.
 */
// bring in the WireMock extension for JUnit 5
@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    // Hibernate is on the class path - creating a H2 in memory database 
    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    // Jackson ObjectMapper is defined in the Spring Context - this is configured with the nuances that Spring Boot has setup.
    @Autowired
    ObjectMapper objectMapper;

    //WireMockServer is autowired in 
    @Autowired 
    WireMockServer wireMockServer;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();


    @TestConfiguration // tells Spring Boot this is my test configuration.
    static class RestTemplateBuilderProvider {
        // destroyMethod is recommended by the WireMock documentation. 
        // When the spring context stops this bean will be shut down.
        @Bean(destroyMethod = "stop") 
        // This configuration class sets up the WireMockServer configuration> the server is available for our tests.
        public WireMockServer wireMockServer(){
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }
    // execute before every test method
    @BeforeEach 
    void setUp() {
    // select the builder method e.g. type 'C' for Customer from the suggestions select Customer.builder().build()
    // >after ...er.builder() add a dot to start setting properties
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    // Happy path test>we take the order from NEW>goes through the validation + allocation process>we assert that the order is updated properly 
    @Test
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        // make sure the Microservice isn't running otherwise you'll get Failed to bind to /0.0.0.0:8080
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        // had project issues>resolution>Project Structure>Project SDK 11>Project language level: 11>apply>OK
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
        // okJson returns back a BeerDto as a JSON body
        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))); 

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        // await() polls>better than using Thread.sleep(500)
        // default polling timeout is 10 seconds> ConditionTimeoutException
        await().untilAsserted(() -> { 
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            // test listener is implemented:-
            // mssc-beer-order-service\src\test\java\guru\sfg\beer\order\service\services\testcomponets\BeerOrderAllocationListener.java
            // now we expect ALLOCATED
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            // verify QuantityAllocated is updated
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        // get updated order
        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(savedBeerOrder2);
        // must be ALLOCATED ... restTemplate was initially causing problems for the test
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());
        savedBeerOrder2.getBeerOrderLines().forEach(line -> {
            // in our happy path, we will have 100% allocation on this order>verify QuantityAllocated gets updated
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });
    }

    // has duplicated code, when we have a few tests written look to remove duplication
    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        // creates beerOrderPickedUp method
        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            // poll until status=PICKED_UP
            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

        // only testing, but bad practise: .get(), should be working with the Optional object???
        // these 2 lines aren't needed as a duplication of the steps above?
        BeerOrder pickedUpOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedUpOrder.getOrderStatus());
    }

    // helper method>usually he keeps them at the bottom of the class.
    // Creating a valid BeerOrder - not persisting it.
    public BeerOrder createBeerOrder(){
        BeerOrder beerOrder = BeerOrder.builder()
                // added testCustomer
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                // needed so the request is /api/v1/beerUpc/12345 
                // + not /api/v1/beerUpc/null 
                .upc("12345") 
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        // we only have 1 BeerOrderLine
        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
