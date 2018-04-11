/**
 * Copyright Vast 2018. All Rights Reserved.
 * <p/>
 * http://www.vast.com
 */
package com.vocumsineratio.dddsample.cargo.test;

import com.vocumsineratio.dddsample.cargo.shell.api.Cargo;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Danil Suits (danil@vast.com)
 */
class BookingLifecycle {
    void fromHongKongToStockholm(Cargo sut) {
        // TODO: We still need to get the available routes included in this scenario
        // which is to say, we need to pass these voyages through the api.
        // Hongkong - NYC - Chicago - Stockholm, initial routing
        // Tokyo - Hamburg - Stockholm, rerouting misdirected cargo from Tokyo

        // The basic idea here is to reproduce the legacy
        // life cycle scenario test, changing the API; that
        // will allow the evaluation of multiple implementations
        // that aren't so tightly coupled to the citerus design.

        // In this case, I'm deliberately aiming for a bare
        // bones API, suitable for working across a boundary.
        // So the constraint is "lowest common denominator";
        // ideally this would be all primitives, but that's a
        // bit more challenge than I like, and more than I
        // think is necessary in Java.  String, List<> and
        // especially Map<> should be in play.

        // Map<> is the important one, as I am thinking in terms
        // of messages, with weak extensible schema.

        String trackingId = sut.book("CNHKG", "SESTO", "2009-03-18");
        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("NOT_RECEIVED", transportStatus(delivery));
            Assert.assertEquals("NOT_ROUTED", routingStatus(delivery));
            Assert.assertEquals(false, isMisdirected(delivery));
            Assert.assertNull(eta(delivery));
            Assert.assertNull(nextExpectedActivity(delivery));
        }

        {
            // TODO: there's a code smell here, the legacy test is
            // wishy washy about the language `route` vs `itinerary`
            // Those spellings are replicated here, but the confusion
            // in the ubiquitous language should be resolved.  Maybe
            // the Evans book has information that would make things
            // more clear.
            List itineraries = sut.routes(trackingId);

            // TODO: we might should be testing that the itineraries
            // that are returned satisfy all of the the expected properties
            // each leg happens after the previous leg
            // each leg arrival happens after the leg departure
            // each leg departs from the previous legs arrival

            // we might also want to check that the itinerary satisfies
            // the constraints that we provided when we last specified the route.
        }

        {
            // But for our purposes, we don't actually care whether or not
            // the specified itinerary is one that was proposed by the model.
            // We can here in the test describe the itinerary that we should
            // be using in this test, without worrying about the routing
            // mechanic.
            Map itinerary = new HashMap();
            List legs = new ArrayList();
            itinerary.put("legs", legs);
            legs.addAll(
                    Arrays.asList(
                            asLeg("V100", "CNHKG", "USNYC", "2009-03-03", "2009-03-09"),
                            asLeg("V200", "USNYC", "USCHI", "2009-03-10", "2009-03-11"),
                            asLeg("V200", "USCHI", "SESTO", "2009-03-12", "2009-03-15")
                    )
            );

            // TODO:

            sut.assignToRoute(trackingId, itinerary);
        }

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("NOT_RECEIVED", transportStatus(delivery));
            Assert.assertEquals("ROUTED", routingStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertNotNull(eta(delivery));
            Assert.assertEquals("RECEIVE", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("CNHKG", nextLocation(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-01", "CNHKG", "RECEIVE");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("IN_PORT", transportStatus(delivery));
            Assert.assertEquals("CNHKG", lastKnownLocation(delivery));
        }

        sut.registerEvent(trackingId, "2009-03-03", "CNHKG", "LOAD", "V100");

        {
            // TODO: note that these assertions are starting to overfit a
            // particular itinerary being returned.  In this case, the
            // assumption is that the first leg of the assigned itinerary
            // is on this specific voyage, with is specifically going
            // to New York City.


            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("V100", currentVoyage(delivery));
            Assert.assertEquals("CNHKG", lastKnownLocation(delivery));
            Assert.assertEquals("ONBOARD_CARRIER", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertEquals("UNLOAD", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("USNYC", nextLocation(nextExpectedActivity(delivery)));
            Assert.assertEquals("V100", nextEventVoyage(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-05", "ZZZZZ", "LOAD", "XX000");

        sut.registerEvent(trackingId, "2009-03-07", "JNTKO", "UNLOAD", "V100");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("", currentVoyage(delivery));
            Assert.assertEquals("JNTKO", lastKnownLocation(delivery));
            Assert.assertEquals("IN_PORT", transportStatus(delivery));
            Assert.assertTrue(isMisdirected(delivery));
            Assert.assertNull(nextExpectedActivity(delivery));
        }

        sut.specifyNewRoute(trackingId, "JNTKO", "SESTO", "2009-03-18");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("MISROUTED", routingStatus(delivery));
            Assert.assertNull(nextExpectedActivity(delivery));
        }

        {
            List itineraries = sut.routes(trackingId);

            // But for our purposes, we don't actually care whether or not
            // the specified itinerary is one that was proposed by the model.
            // We can here in the test describe the itinerary that we should
            // be using in this test, without worrying about the routing
            // mechanic.
            Map itinerary = new HashMap();
            List legs = new ArrayList();
            itinerary.put("legs", legs);
            legs.addAll(
                    Arrays.asList(
                            asLeg("V300", "JNTKO", "DEHAM", "2009-03-08", "2009-03-12"),
                            asLeg("V400", "DEHAM", "SESTO", "2009-03-14", "2009-03-17")
                    )
            );

            sut.assignToRoute(trackingId, itinerary);
        }

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("ROUTED", routingStatus(delivery));
        }

        sut.registerEvent(trackingId, "2009-03-08", "JNTKO", "LOAD", "V300");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("V300", currentVoyage(delivery));
            Assert.assertEquals("JNTKO", lastKnownLocation(delivery));
            Assert.assertEquals("ONBOARD_CARRIER", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertEquals("UNLOAD", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("DEHAM", nextLocation(nextExpectedActivity(delivery)));
            Assert.assertEquals("V300", nextEventVoyage(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-12", "DEHAM", "UNLOAD", "V300");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("", currentVoyage(delivery));
            Assert.assertEquals("DEHAM", lastKnownLocation(delivery));
            Assert.assertEquals("IN_PORT", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertEquals("LOAD", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("DEHAM", nextLocation(nextExpectedActivity(delivery)));
            Assert.assertEquals("V400", nextEventVoyage(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-14", "DEHAM", "LOAD", "V400");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("V400", currentVoyage(delivery));
            Assert.assertEquals("DEHAM", lastKnownLocation(delivery));
            Assert.assertEquals("ONBOARD_CARRIER", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertEquals("UNLOAD", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("SESTO", nextLocation(nextExpectedActivity(delivery)));
            Assert.assertEquals("V400", nextEventVoyage(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-15", "SESTO", "UNLOAD", "V400");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("", currentVoyage(delivery));
            Assert.assertEquals("SESTO", lastKnownLocation(delivery));
            Assert.assertEquals("IN_PORT", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertEquals("CLAIM", nextEvent(nextExpectedActivity(delivery)));
            Assert.assertEquals("SESTO", nextLocation(nextExpectedActivity(delivery)));
        }

        sut.registerEvent(trackingId, "2009-03-16", "SESTO", "CLAIM");

        {
            Map delivery = sut.delivery(trackingId);
            Assert.assertEquals("", currentVoyage(delivery));
            Assert.assertEquals("SESTO", lastKnownLocation(delivery));
            Assert.assertEquals("CLAIMED", transportStatus(delivery));
            Assert.assertFalse(isMisdirected(delivery));
            Assert.assertNull(nextExpectedActivity(delivery));
        }
    }

    private Map asLeg(String voyage, String from, String to, String loadTime, String unloadTime) {
        Map dto = new HashMap();

        dto.put("voyageNumber", voyage);
        dto.put("from", from);
        dto.put("to", to);
        dto.put("loadTime", loadTime);
        dto.put("unloadTime", unloadTime);

        return dto;
    }

    private String currentVoyage(Map delivery) {
        return (String)delivery.get("currentVoyage");
    }

    private String lastKnownLocation(Map delivery) {
        return (String) delivery.get("lastKnownLocation");
    }

    private Map nextExpectedActivity(Map delivery) {
        return (Map)delivery.get("nextExpectedActivity");
    }

    private String eta(Map delivery) {
        return (String)delivery.get("eta");
    }

    private boolean isMisdirected(Map delivery) {
        return (Boolean)delivery.get("isMisdirected");
    }

    private String routingStatus(Map delivery) {
        return (String)delivery.get("routingStatus");
    }

    private String transportStatus(Map delivery) {
        return (String)delivery.get("transportStatus");
    }

    private String nextEventVoyage(Map expectedActivity) {
        return (String)expectedActivity.get("voyageNumber");
    }

    private String nextLocation(Map expectedActivity) {
        return (String)expectedActivity.get("location");
    }

    private String nextEvent(Map expectedActivity) {
        return (String)expectedActivity.get("event");
    }

}
