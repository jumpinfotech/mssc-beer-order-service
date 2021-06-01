package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by jt on 12/2/19.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderResult result){
        final UUID beerOrderId = result.getOrderId();

        // this doesn't appear in console unless the test has a wait / polling wait added,
        // (application.properties correctly has logging.level.guru=debug)
        // message is put on the queue, but the test is terminated before we can read the message off the queue
        log.debug("Validation Result for Order Id: " + beerOrderId);

        beerOrderManager.processValidationResult(beerOrderId, result.getIsValid());
    }
}
