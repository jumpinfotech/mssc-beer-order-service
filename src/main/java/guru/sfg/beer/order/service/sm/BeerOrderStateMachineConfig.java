package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/**
 * Created by jt on 11/29/19.
 */
@RequiredArgsConstructor
@Configuration
@EnableStateMachineFactory
// new state machine created
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> { 

    // property name matches class name + @RequiredArgsConstructor is added>spring then autowires
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum>  validateOrderAction; 
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum>  allocateOrderAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(BeerOrderStatusEnum.NEW)
                .states(EnumSet.allOf(BeerOrderStatusEnum.class)) // range of valid states
                .end(BeerOrderStatusEnum.PICKED_UP) // we have several terminal(end) states
                .end(BeerOrderStatusEnum.DELIVERED)
                .end(BeerOrderStatusEnum.DELIVERY_EXCEPTION)
                .end(BeerOrderStatusEnum.VALIDATION_EXCEPTION)
                .end(BeerOrderStatusEnum.ALLOCATION_EXCEPTION);
    }

    // State Machine notes can be found here.
    // https://github.com/jumpinfotech/mssc-ssm/commit/a97dc989782fe78073779dc5676fe08c62c708f5
    // https://github.com/jumpinfotech/mssc-ssm/commit/736ee9a1757fea9eb2330995a1cc13d7346fe7f1
    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.VALIDATION_PENDING)
                .event(BeerOrderEventEnum.VALIDATE_ORDER)
                // when we transition with the event to VALIDATE_ORDER, we transition from NEW to VALIDATION_PENDING, 
                // the event triggers the validateOrderAction, which calls ValidateOrderAction.execute( ... )
                .action(validateOrderAction)
           .and().withExternal() 
                // for event VALIDATION_PASSED source NEW will transition to the target VALIDATED 
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.VALIDATED)
                .event(BeerOrderEventEnum.VALIDATION_PASSED)
           .and().withExternal()
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.VALIDATION_EXCEPTION)
                .event(BeerOrderEventEnum.VALIDATION_FAILED)
            .and().withExternal()
                // ALLOCATION_PENDING means we've placed the allocation request
                .source(BeerOrderStatusEnum.VALIDATED).target(BeerOrderStatusEnum.ALLOCATION_PENDING) 
                // ALLOCATE_ORDER triggers the allocateOrderAction>which sends a JMS message
                .event(BeerOrderEventEnum.ALLOCATE_ORDER)
                .action(allocateOrderAction)
            .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATED)
                .event(BeerOrderEventEnum.ALLOCATION_SUCCESS) // normal complete allocation
            .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATION_EXCEPTION)
                .event(BeerOrderEventEnum.ALLOCATION_FAILED) // failure event
            .and().withExternal()
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.PENDING_INVENTORY)
                .event(BeerOrderEventEnum.ALLOCATION_NO_INVENTORY); // waiting for inventory




    }
}
