package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by jt on 12/2/19.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// same as ValidateOrderRequest, it's bad practise to use same object though, hence this new 'duplicating' class
public class AllocateOrderRequest {
    private BeerOrderDto beerOrderDto;
}
