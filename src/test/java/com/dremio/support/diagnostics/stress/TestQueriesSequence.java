import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dremio.support.diagnostics.stress.QueriesSequence;
import org.junit.jupiter.api.Test;

public class TestQueriesSequence {

  @Test
  public void testRandomToString() {
    assertEquals("RANDOM", QueriesSequence.RANDOM.toString());
  }

  @Test
  public void testSequentialToString() {
    assertEquals("SEQUENTIAL", QueriesSequence.SEQUENTIAL.toString());
  }
}
