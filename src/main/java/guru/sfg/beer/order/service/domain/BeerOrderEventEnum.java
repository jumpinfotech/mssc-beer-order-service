package guru.sfg.beer.order.service.domain;

/**
 * Created by jt on 11/29/19.
 */
public enum BeerOrderEventEnum {
    // new event CANCEL_ORDER>will get passed into the Spring State Machine
    VALIDATE_ORDER, CANCEL_ORDER, VALIDATION_PASSED, VALIDATION_FAILED,
    ALLOCATE_ORDER, ALLOCATION_SUCCESS, ALLOCATION_NO_INVENTORY, ALLOCATION_FAILED,
    BEERORDER_PICKED_UP
}
