/**
 * Copyright Vast 2018. All Rights Reserved.
 * <p/>
 * http://www.vast.com
 */
package com.vocumsineratio.dddsample.cargo.test;

import com.vocumsineratio.dddsample.cargo.shell.api.Cargo;
import org.junit.Before;
import org.junit.Test;
import se.citerus.dddsample.application.ApplicationEvents;
import se.citerus.dddsample.application.BookingService;
import se.citerus.dddsample.application.CargoInspectionService;
import se.citerus.dddsample.application.HandlingEventService;
import se.citerus.dddsample.application.impl.BookingServiceImpl;
import se.citerus.dddsample.application.impl.CargoInspectionServiceImpl;
import se.citerus.dddsample.application.impl.HandlingEventServiceImpl;
import se.citerus.dddsample.application.util.DateTestUtil;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.Delivery;
import se.citerus.dddsample.domain.model.cargo.HandlingActivity;
import se.citerus.dddsample.domain.model.cargo.Itinerary;
import se.citerus.dddsample.domain.model.cargo.Leg;
import se.citerus.dddsample.domain.model.cargo.RouteSpecification;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.CannotCreateHandlingEventException;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventFactory;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.model.voyage.Voyage;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;
import se.citerus.dddsample.domain.service.RoutingService;
import se.citerus.dddsample.infrastructure.messaging.stub.SynchronousApplicationEventsStub;
import se.citerus.dddsample.infrastructure.persistence.inmemory.CargoRepositoryInMem;
import se.citerus.dddsample.infrastructure.persistence.inmemory.HandlingEventRepositoryInMem;
import se.citerus.dddsample.infrastructure.persistence.inmemory.LocationRepositoryInMem;
import se.citerus.dddsample.infrastructure.persistence.inmemory.VoyageRepositoryInMem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static se.citerus.dddsample.application.util.DateTestUtil.toDate;
import static se.citerus.dddsample.domain.model.location.SampleLocations.CHICAGO;
import static se.citerus.dddsample.domain.model.location.SampleLocations.HAMBURG;
import static se.citerus.dddsample.domain.model.location.SampleLocations.HONGKONG;
import static se.citerus.dddsample.domain.model.location.SampleLocations.NEWYORK;
import static se.citerus.dddsample.domain.model.location.SampleLocations.STOCKHOLM;
import static se.citerus.dddsample.domain.model.location.SampleLocations.TOKYO;
import static se.citerus.dddsample.domain.model.voyage.SampleVoyages.v100;
import static se.citerus.dddsample.domain.model.voyage.SampleVoyages.v200;
import static se.citerus.dddsample.domain.model.voyage.SampleVoyages.v300;
import static se.citerus.dddsample.domain.model.voyage.SampleVoyages.v400;

/**
 * @author Danil Suits (danil@vast.com)
 */
public class CiterusBookingLifecycleTest {
    /**
     * Repository implementations are part of the infrastructure layer,
     * which in this test is stubbed out by in-memory replacements.
     */
    HandlingEventRepository handlingEventRepository;
    CargoRepository cargoRepository;
    LocationRepository locationRepository;
    VoyageRepository voyageRepository;

    /**
     * This interface is part of the application layer,
     * and defines a number of events that occur during
     * aplication execution. It is used for message-driving
     * and is implemented using JMS.
     * <p>
     * In this test it is stubbed with synchronous calls.
     */
    ApplicationEvents applicationEvents;

    /**
     * These three components all belong to the application layer,
     * and map against use cases of the application. The "real"
     * implementations are used in this lifecycle test,
     * but wired with stubbed infrastructure.
     */
    BookingService bookingService;
    HandlingEventService handlingEventService;
    CargoInspectionService cargoInspectionService;

    /**
     * This factory is part of the handling aggregate and belongs to
     * the domain layer. Similar to the application layer components,
     * the "real" implementation is used here too,
     * wired with stubbed infrastructure.
     */
    HandlingEventFactory handlingEventFactory;

    /**
     * This is a domain service interface, whose implementation
     * is part of the infrastructure layer (remote call to external system).
     * <p>
     * It is stubbed in this test.
     */
    RoutingService routingService;

    @Before
    public void setUp() {
        routingService = new RoutingService() {
            public List<Itinerary> fetchRoutesForSpecification(RouteSpecification routeSpecification) {
                // TODO: the dates here need to be made internally consistent with the narrative.
                // As they were a copy/paste from CargoLifecycleScenarioTest, that should be updated
                // as well.
                if (routeSpecification.origin().equals(HONGKONG)) {
                    // Hongkong - NYC - Chicago - Stockholm, initial routing
                    return Arrays.asList(
                            new Itinerary(Arrays.asList(
                                    // TODO: these legs should be in order;
                                    // TODO: and it would be more consistent for v100 to include a stop in JNTKO
                                    new Leg(v100, HONGKONG, NEWYORK, toDate("2009-03-03"), toDate("2009-03-09")),
                                    new Leg(v200, NEWYORK, CHICAGO, toDate("2009-03-10"), toDate("2009-03-14")),
                                    new Leg(v200, CHICAGO, STOCKHOLM, toDate("2009-03-07"), toDate("2009-03-11"))
                            ))
                    );
                } else {
                    // Tokyo - Hamburg - Stockholm, rerouting misdirected cargo from Tokyo
                    return Arrays.asList(
                            new Itinerary(Arrays.asList(
                                    // TODO: These should be later to arrive than the original itinerary
                                    // although still on time
                                    new Leg(v300, TOKYO, HAMBURG, toDate("2009-03-08"), toDate("2009-03-12")),
                                    new Leg(v400, HAMBURG, STOCKHOLM, toDate("2009-03-14"), toDate("2009-03-15"))
                            ))
                    );
                }
            }
        };


        applicationEvents = new SynchronousApplicationEventsStub();

        // In-memory implementations of the repositories
        handlingEventRepository = new HandlingEventRepositoryInMem();
        cargoRepository = new CargoRepositoryInMem();
        locationRepository = new LocationRepositoryInMem();
        voyageRepository = new VoyageRepositoryInMem();

        // Actual factories and application services, wired with stubbed or in-memory infrastructure
        handlingEventFactory = new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository);

        cargoInspectionService = new CargoInspectionServiceImpl(applicationEvents, cargoRepository, handlingEventRepository);
        handlingEventService = new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, handlingEventFactory);
        bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService);

        // Circular dependency when doing synchrounous calls
        ((SynchronousApplicationEventsStub) applicationEvents).setCargoInspectionService(cargoInspectionService);
    }

    @Test
    public void testBookingLifeCycle () {
        BookingLifecycle lifecycle = new BookingLifecycle();

        Cargo sut = new Cargo() {
            @Override
            public String book(String origin, String destination, String arrivalDeadline) {
                TrackingId trackingId = bookingService.bookNewCargo(
                        new UnLocode(origin),
                        new UnLocode(destination),
                        DateTestUtil.toDate(arrivalDeadline)
                );

                return trackingId.idString();
            }

            @Override
            public Map delivery(String trackingId) {
                se.citerus.dddsample.domain.model.cargo.Cargo legacyCargo = cargo(trackingId);

                Delivery delivery = legacyCargo.delivery();

                Map result = new HashMap();
                result.put("transportStatus", delivery.transportStatus().name());
                result.put("routingStatus", delivery.routingStatus().name());

                Date eta = delivery.estimatedTimeOfArrival();
                if (null != eta) {
                    result.put("eta", dateString(eta));
                }

                HandlingActivity handlingActivity = delivery.nextExpectedActivity();
                if (null != handlingActivity) {
                    result.put("nextExpectedActivity", asMap(handlingActivity));
                }

                Location location = delivery.lastKnownLocation();
                if (null != location) {
                    result.put("lastKnownLocation", location.unLocode().idString());
                }

                Voyage voyage = delivery.currentVoyage();
                if (null != voyage) {
                    result.put("currentVoyage", voyage.voyageNumber().idString());
                }

                result.put("isMisdirected", delivery.isMisdirected());

                return result;
            }

            private Map asMap(HandlingActivity handlingActivity) {
                Map result = new HashMap();

                result.put("event", handlingActivity.type().name());
                Voyage voyage = handlingActivity.voyage();
                if (null != voyage) {
                    result.put("voyageNumber", voyage.voyageNumber().idString());
                }
                result.put("location", handlingActivity.location().unLocode().idString());

                return result;
            }

            private se.citerus.dddsample.domain.model.cargo.Cargo cargo(String trackingId) {
                return cargoRepository.find(
                                    new TrackingId(trackingId)
                            );
            }

            @Override
            public List routes(String trackingId) {
                List<Itinerary> itineraries = bookingService.requestPossibleRoutesForCargo(
                        new TrackingId(trackingId)
                );

                List result = new ArrayList();
                for(Itinerary itinerary : itineraries) {
                    result.add(asMap(itinerary));
                }
                return result;
            }

            Map asMap(Itinerary itinerary) {
                Map route = new HashMap();
                List legs = new ArrayList();
                route.put("legs", legs);
                for(Leg leg : itinerary.legs()) {

                    legs.add(asMap(leg));
                }

                return route;
            }

            Map asMap(Leg leg) {
                Map dto = new HashMap();

                dto.put("voyageNumber", leg.voyage().voyageNumber().idString());
                dto.put("from", leg.loadLocation().unLocode().idString());
                dto.put("to", leg.unloadLocation().unLocode().idString());
                dto.put("loadTime", dateString(leg.loadTime()));
                dto.put("unloadTime", dateString(leg.unloadTime()));

                return dto;
            }

            String dateString(Date date) {
                SimpleDateFormat sdf = new SimpleDateFormat("YYYY-mm-dd");
                return sdf.format(date);
            }

            @Override
            public void assignToRoute(String trackingId, Map dto) {
                Itinerary itinerary = new Itinerary(toLegs(dto));

                se.citerus.dddsample.domain.model.cargo.Cargo legacyCargo = cargo(trackingId);
                legacyCargo.assignToRoute(itinerary);
            }

            private List<Leg> toLegs(Map dto) {
                List<Leg> legs = new ArrayList<>();

                for (Object crnt : (List)dto.get("legs")) {
                    Map leg = (Map)crnt;

                    Voyage voyage = voyageRepository.find(
                            new VoyageNumber(
                                    (String)leg.get("voyageNumber")
                            )
                    );

                    Location loadLocation = locationRepository.find(
                            new UnLocode(
                                    (String)leg.get("from")
                            )
                    );

                    Location unloadLocation = locationRepository.find(
                            new UnLocode(
                                    (String)leg.get("to")
                            )
                    );

                    Date loadTime = DateTestUtil.toDate(
                            (String)leg.get("loadTime")
                    );

                    Date unloadTime = DateTestUtil.toDate(
                            (String)leg.get("unloadTime")
                    );

                    legs.add(
                            new Leg (
                                    voyage,
                                    loadLocation,
                                    unloadLocation,
                                    loadTime,
                                    unloadTime
                            )
                    );
                }

                return legs;
            }


            @Override
            public void registerEvent(String trackingId, String eventDate, String location, String eventName) {
                try {
                    handlingEventService.registerHandlingEvent(
                            toDate(eventDate),
                            new TrackingId(trackingId),
                            null,
                            new UnLocode(location),
                            HandlingEvent.Type.valueOf(eventName)
                    );
                } catch (CannotCreateHandlingEventException e) {
                    // For this API, there's no side effect.
                }

            }

            @Override
            public void registerEvent(String trackingId, String eventDate, String location, String eventName, String voyageNumber) {
                try {
                    handlingEventService.registerHandlingEvent(
                            toDate(eventDate),
                            new TrackingId(trackingId),
                            new VoyageNumber(voyageNumber),
                            new UnLocode(location),
                            HandlingEvent.Type.valueOf(eventName)
                    );
                } catch (CannotCreateHandlingEventException e) {
                    // For this API, there's no side effect.
                }            }

            @Override
            public void specifyNewRoute(String trackingId, String location, String destination, String arrivalDeadline) {
                se.citerus.dddsample.domain.model.cargo.Cargo legacyCargo = cargo(trackingId);

                RouteSpecification route = new RouteSpecification(
                    locationRepository.find(new UnLocode(location)),
                    locationRepository.find(new UnLocode(destination)),
                    DateTestUtil.toDate(arrivalDeadline)
                );

                legacyCargo.specifyNewRoute(route);
            }
        };

        lifecycle.fromHongKongToStockholm(sut);
    }
}
