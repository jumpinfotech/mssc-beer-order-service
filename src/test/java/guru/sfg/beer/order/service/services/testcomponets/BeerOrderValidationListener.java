package guru.sfg.beer.order.service.services.testcomponets;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Created by jt on 2/15/20.
 */
@Slf4j
@RequiredArgsConstructor
@Component
// Expected: ALLOCATED Actual: VALIDATION_PENDING
// We send a JMS message + the status is now VALIDATION_PENDING
// We need to process that message, we could use a mock bean, but we have an embedded ActiveMQ server - we provide a processor for it.
// This listener stands in as a double for the real listener, listen to the JMS queue + process the message.
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;


    // I'm going to add in a bean that's going to listen for the validation messages.
    // Remember we're sending out to VALIDATE_ORDER_QUEUE queue, this component listens on that queue. 
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void list(Message msg){

        // Spring parses the Message payload into a ValidateOrderRequest using Jackson
        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();

        // output added to prove this is running > it does appear in the console
        System.out.println("########### I RAN ########");

        // We send back to the VALIDATE_ORDER_RESPONSE_QUEUE, providing a ValidateOrderResult.
        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderResult.builder()
                .isValid(true)
                // the BeerOrder Id is gotten from the Message
                .orderId(request.getBeerOrder().getId())
                // don't forget build() at the end
                .build());

    }
}
