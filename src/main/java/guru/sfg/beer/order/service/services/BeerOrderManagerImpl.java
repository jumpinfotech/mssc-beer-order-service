package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Created by jt on 11/29/19.
 */
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    // we want a constant, defining it in multiple places would be bad to do
    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        // id is set to null just in case someone sends in an existing BeerOrder - defensive programming 
        beerOrder.setId(null);
        // our definition of what a newBeerOrder is
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW); 

        // persisted to DB, hibernate sets the id value
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder); 
        
        // we do not need to rehydrate the state machine as it's a new BeerOrder,
        // we send the state machine a message, with the VALIDATE_ORDER event
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        // get BeerOrder
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId); 

        if(isValid){
            // send VALIDATION_PASSED event to state machine
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED); 

            // when we do a sendBeerOrderEvent on the previous line, the interceptor saves it,
            // BeerOrder beerOrder (at the start of this method) then becomes a stale object>
            // so hibernate won't be happy, we therefore get a current fresh version, expensive?  
            // Maybe not as it may come from hibernate's cache (maybe we could see this caching in the SQL log??):-
            BeerOrder validatedOrder = beerOrderRepository.findOneById(beerOrderId); 

            // send event - telling the state machine to allocate that order, 
            // which ultimately triggers the action to send a message to the allocation queue
            sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        // get BeerOrder from repository
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId()); 
        // send BeerOrder with ALLOCATION_SUCCESS message, update the state machine which evolves the status
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQty(beerOrderDto, beerOrder);
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        // update the state machine which evolves the status
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);

        updateAllocatedQty(beerOrderDto, beerOrder);
    }

    // used for if allocation was a success, or it is pending inventory,
    // common method updates the quantity allocated on each line
    private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDto.getId());

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(beerOrderLine.getId() .equals(beerOrderLineDto.getId())){
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(beerOrder);
    }

    // send a failure event if allocation failed 
    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum){
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
        // we build a Spring Message with the event
        Message msg = MessageBuilder.withPayload(eventEnum)
                // set the order id, even though it's a UUID we put that on as a String
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString()) 
                .build();

        // send the state machine a message which will have the order id, 
        // this will go through the configuration logic of the state machine
        sm.sendEvent(msg);
    }

    // build up a new state machine, getting the status from the database
    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder){
        // returns back a state machine for that beerOrder id, Spring will do some caching of that if we're already working with it, else it builds a new one
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();
        // we won't take what the stateMachineFactory says> we'll take with the database says 
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    // when we build our state machine we add the Interceptor 
                    sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                    // set it to the status of the beerOrder
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });

        sm.start();

        // we effectively pulled the state machine out of the database
        return sm;
    }
}
