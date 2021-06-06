package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by jt on 2/26/20.
 */
@Slf4j
@RequiredArgsConstructor
@Component
// Spring Component AllocationFailureAction is invoked when we have an ALLOCATION_FAILED event
// this coding pattern is modular + targeted - following the Single Responsibility Principle,
// AllocationFailureAction has 1 function - sending out an allocation failure message
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    // implements the AllocationFailureAction from the Spring State Machine
    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        // take in the Spring State Machine StateContext - then extract the beerOrderId from it
        String beerOrderId = (String) context.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        // send to the ALLOCATE_FAILURE_QUEUE, the message is a POJO>
        // AllocationFailureEvent that gets converted to a JMS message which contains the orderId property that failed allocation
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_FAILURE_QUEUE, AllocationFailureEvent.builder()
            .orderId(UUID.fromString(beerOrderId))
                    .build());

        log.debug("Sent Allocation Failure Message to queue for order id " + beerOrderId);
    }
}
