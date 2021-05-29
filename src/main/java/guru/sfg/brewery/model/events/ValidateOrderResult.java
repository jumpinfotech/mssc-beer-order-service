package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Created by jt on 12/2/19.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// copied + pasted from mssc-beer-service project 
public class ValidateOrderResult {
    private UUID orderId;
    private Boolean isValid;
}
