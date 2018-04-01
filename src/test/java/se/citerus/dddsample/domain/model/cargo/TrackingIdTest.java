package se.citerus.dddsample.domain.model.cargo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;

public class TrackingIdTest {

    @Test
    public void testConstructor() {
        try {
            new TrackingId(null);
            fail("Should not accept null constructor arguments");
        } catch (IllegalArgumentException expected) {
        }
    }

}
