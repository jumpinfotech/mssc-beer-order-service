package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by jt on 11/30/19.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// simple POJO created will be used in ValidateOrderAction> will be sent into JMS queue as JSON payload
public class ValidateOrderRequest { 

    private BeerOrderDto beerOrder;
}
