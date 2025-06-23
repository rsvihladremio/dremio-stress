import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dremio.support.diagnostics.stress.JobStatusResponse;
import org.junit.jupiter.api.Test;

public class JobStatusResponseTest {

  @Test
  public void testGetSetMessage() {
    JobStatusResponse response = new JobStatusResponse();
    String testMessage = "Test message";
    response.setMessage(testMessage);
    assertEquals(testMessage, response.getMessage());
  }

  @Test
  public void testGetSetStatus() {
    JobStatusResponse response = new JobStatusResponse();
    String testStatus = "RUNNING";
    response.setStatus(testStatus);
    assertEquals(testStatus, response.getStatus());
  }
}
