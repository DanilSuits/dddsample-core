package com.vocumsineratio.dddsample.cargo.shell.api;

import java.util.List;
import java.util.Map;

/**
 * @author Danil Suits (danil@vast.com)
 */
public interface Cargo {
    String book(String origin, String destination, String arrivalDeadline);

    Map delivery(String trackingId);

    List routes(String trackingId);

    void assignToRoute(String trackingId, Map itinerary);

    void registerEvent(String trackingId, String eventDate, String location, String eventName);

    void registerEvent(String trackingId, String eventDate, String location, String eventName, String voyageNumber);

    void specifyNewRoute(String trackingId, String location, String destination, String arrivalDeadline);
}
