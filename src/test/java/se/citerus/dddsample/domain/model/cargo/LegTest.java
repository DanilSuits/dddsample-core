package se.citerus.dddsample.domain.model.cargo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;

public class LegTest {

    @Test
    public void testConstructor() {
        try {
            new Leg(null, null, null, null, null);
            fail("Should not accept null constructor arguments");
        } catch (IllegalArgumentException expected) {
        }
    }
}
