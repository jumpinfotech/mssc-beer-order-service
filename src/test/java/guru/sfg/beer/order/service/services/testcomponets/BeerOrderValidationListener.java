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
// Added the capability to say it is not a valid order>previously we always returned isValid=true
// Can't we use Mockito? We are bringing up the JMS broker, we have a full JMS environment>this is a JMS listener test component. 
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void list(Message msg){
        // new variable
        boolean isValid = true; 
        // new send response variable
        boolean sendResponse = true;

        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();
        // null check added as some tests were failing
        if (request.getBeerOrder().getCustomerRef() != null) {
            // ValidateOrderRequest is now examined for some keys
            // ideally fail-validation string would be externalised
            if (request.getBeerOrder().getCustomerRef().equals("fail-validation")){
                //condition to fail validation
                isValid = false;
            // a dont-validate CustomerRef means the response should not be sent    
            } else if (request.getBeerOrder().getCustomerRef().equals("dont-validate")){
                sendResponse = false;
            }
        }

        if (sendResponse) {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                    ValidateOrderResult.builder()
                            .isValid(isValid)
                            .orderId(request.getBeerOrder().getId())
                            .build());
        }
    }
}
