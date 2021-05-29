package guru.sfg.beer.order.service.domain;

/**
 * Created by jt on 11/29/19.
 */
// new enum for events
public enum BeerOrderEventEnum { 
    // We could have an ALLOCATION event, however we use VALIDATION_PASSED event to do the allocation. There's no right way.
    VALIDATE_ORDER, VALIDATION_PASSED, VALIDATION_FAILED,
    ALLOCATE_ORDER, ALLOCATION_SUCCESS, ALLOCATION_NO_INVENTORY, ALLOCATION_FAILED,
    BEERORDER_PICKED_UP
}
