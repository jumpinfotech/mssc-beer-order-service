package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Created by jt on 2/26/20.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// this is the POJO that we send out to the JMS Queue
public class AllocationFailureEvent {
    private UUID orderId;
}
