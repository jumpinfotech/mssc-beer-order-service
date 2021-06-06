package guru.sfg.beer.order.service.services.testcomponets;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Created by jt on 2/16/20.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg){
        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
        // added for testPartialAllocation
        boolean pendingInventory = false;
        // added for testAllocationFailure
        boolean allocationError = false;
        // new variable send response variable
        boolean sendResponse = true;

        if (request.getBeerOrderDto().getCustomerRef() != null) { // avoid NullPointerException - fixes a defect
            // if test value equals fail-allocation, I return back an allocationError
            if (request.getBeerOrderDto().getCustomerRef().equals("fail-allocation")){
                allocationError = true;
            // if test value equals partial-allocation, I return back an pendingInventory
            }  else if (request.getBeerOrderDto().getCustomerRef().equals("partial-allocation")) {
                pendingInventory = true;
                // a dont-allocate CustomerRef means the response should not be sent   
            } else if (request.getBeerOrderDto().getCustomerRef().equals("dont-allocate")){
                sendResponse = false;
            }
        }

        // effectively a final variable
        boolean finalPendingInventory = pendingInventory;

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            // it's inside a forEach so it needs to be effectively final, variables cannot change state - compiler complains without it
            if (finalPendingInventory) {
                // if finalPendingInventory=true we aren't allocating the full order quantity>we decrease it by 1
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                // set QuantityAllocated to OrderQuantity
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });

        if (sendResponse) {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .beerOrderDto(request.getBeerOrderDto())
                            // allocationError + pendingInventory changed from false to variables
                            .pendingInventory(pendingInventory)
                            .allocationError(allocationError)
                            .build());
        }
    }
}
