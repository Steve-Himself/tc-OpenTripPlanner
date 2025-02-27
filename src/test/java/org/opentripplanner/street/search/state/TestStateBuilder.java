package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model.site.PathwayMode.WALKWAY;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.ElevatorOffboardVertex;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Builds up a state chain for use in tests.
 */
public class TestStateBuilder {

  private static final Instant DEFAULT_START_TIME = OffsetDateTime
    .parse("2023-04-18T12:00:00+02:00")
    .toInstant();
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    currentState =
      new State(
        StreetModelForTest.intersectionVertex(count, count),
        StreetSearchRequest.of().withMode(mode).withStartTime(DEFAULT_START_TIME).build()
      );
  }

  /**
   * Create an initial state that starts walking.
   */
  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  /**
   * Create an initial state that start in a car.
   */
  public static TestStateBuilder ofDriving() {
    return new TestStateBuilder(StreetMode.CAR);
  }

  public static TestStateBuilder ofCarRental() {
    return new TestStateBuilder(StreetMode.CAR_RENTAL);
  }

  public static TestStateBuilder ofScooterRental() {
    return new TestStateBuilder(StreetMode.SCOOTER_RENTAL);
  }

  public static TestStateBuilder ofBikeRental() {
    return new TestStateBuilder(StreetMode.BIKE_RENTAL);
  }

  public static TestStateBuilder ofCycling() {
    return new TestStateBuilder(StreetMode.BIKE);
  }

  public static TestStateBuilder ofBikeAndRide() {
    return new TestStateBuilder(StreetMode.BIKE_TO_PARK);
  }

  public static TestStateBuilder parkAndRide() {
    return new TestStateBuilder(StreetMode.CAR_TO_PARK);
  }

  /**
   * Traverse a very plain street edge with no special characteristics.
   */
  public TestStateBuilder streetEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);

    var edge = StreetModelForTest.streetEdge(from, to);
    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  /**
   * Traverse a street edge and switch to Car mode
   */
  public TestStateBuilder pickUpCarFromStation() {
    return pickUpRentalVehicle(
      RentalFormFactor.CAR,
      TestVehicleRentalStationBuilder.of().withVehicleTypeCar().build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingCar() {
    return pickUpRentalVehicle(
      RentalFormFactor.CAR,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleCar().build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingScooter() {
    return pickUpRentalVehicle(
      RentalFormFactor.SCOOTER,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleScooter().build()
    );
  }

  public TestStateBuilder pickUpBikeFromStation() {
    return pickUpRentalVehicle(
      RentalFormFactor.BICYCLE,
      TestVehicleRentalStationBuilder.of().withVehicleTypeBicycle().build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingBike() {
    return pickUpRentalVehicle(
      RentalFormFactor.BICYCLE,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleBicycle().build()
    );
  }

  /**
   * Traverse an elevator (onboard, hop and offboard edges).
   */
  public TestStateBuilder elevator() {
    count++;

    var onboard1 = elevatorOnBoard(count, "1");
    var onboard2 = elevatorOnBoard(count, "2");
    var offboard1 = elevatorOffBoard(count, "1");
    var offboard2 = elevatorOffBoard(count, "2");

    var from = (StreetVertex) currentState.vertex;
    var link = StreetModelForTest.streetEdge(from, offboard1);

    var boardEdge = ElevatorBoardEdge.createElevatorBoardEdge(offboard1, onboard1);

    var hopEdge = ElevatorHopEdge.createElevatorHopEdge(
      onboard1,
      onboard2,
      StreetTraversalPermission.PEDESTRIAN,
      Accessibility.POSSIBLE
    );

    var alightEdge = ElevatorAlightEdge.createElevatorAlightEdge(
      onboard2,
      offboard2,
      new NonLocalizedString("1")
    );

    currentState =
      EdgeTraverser
        .traverseEdges(currentState, List.of(link, boardEdge, hopEdge, alightEdge))
        .orElseThrow();
    return this;
  }

  public TestStateBuilder stop(RegularStop stop) {
    return arriveAtStop(stop);
  }

  /**
   * Add a state that arrives at a transit stop.
   */
  public TestStateBuilder stop() {
    count++;
    return arriveAtStop(TransitModelForTest.stopForTest("stop", count, count));
  }

  public TestStateBuilder enterStation(String id) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    final var entranceVertex = StreetModelForTest.transitEntranceVertex(id, count, count);
    var edge = StreetTransitEntranceLink.createStreetTransitEntranceLink(from, entranceVertex);
    var states = edge.traverse(currentState);
    currentState = states[0];
    return this;
  }

  public TestStateBuilder exitStation(String id) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var entranceVertex = StreetModelForTest.transitEntranceVertex(id, count, count);
    var edge = PathwayEdge.createLowCostPathwayEdge(from, entranceVertex, WALKWAY);
    var state = edge.traverse(currentState)[0];

    count++;
    var to = StreetModelForTest.intersectionVertex(count, count);
    var link = StreetTransitEntranceLink.createStreetTransitEntranceLink(entranceVertex, to);
    var states = link.traverse(state);
    currentState = states[0];
    return this;
  }

  public TestStateBuilder pathway(String s) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var tov = StreetModelForTest.intersectionVertex(count, count);
    var edge = PathwayEdge.createPathwayEdge(
      from,
      tov,
      I18NString.of(s),
      60,
      100,
      0,
      0,
      true,
      WALKWAY
    );
    currentState = edge.traverse(currentState)[0];
    return this;
  }

  @Nonnull
  private TestStateBuilder arriveAtStop(RegularStop stop) {
    var from = (StreetVertex) currentState.vertex;
    var to = new TransitStopVertexBuilder().withStop(stop).build();

    var edge = StreetTransitStopLink.createStreetTransitStopLink(from, to);
    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  @Nonnull
  private static ElevatorOffboardVertex elevatorOffBoard(int count, String suffix) {
    return new ElevatorOffboardVertex(
      StreetModelForTest.intersectionVertex(count, count),
      suffix,
      suffix
    );
  }

  @Nonnull
  private static ElevatorOnboardVertex elevatorOnBoard(int count, String suffix) {
    return new ElevatorOnboardVertex(
      StreetModelForTest.intersectionVertex(count, count),
      suffix,
      suffix
    );
  }

  private TestStateBuilder pickUpRentalVehicle(
    RentalFormFactor rentalFormFactor,
    VehicleRentalPlace place
  ) {
    count++;
    VehicleRentalPlaceVertex vertex = new VehicleRentalPlaceVertex(place);
    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(
      (StreetVertex) currentState.vertex,
      vertex
    );
    currentState = link.traverse(currentState)[0];

    var edge = VehicleRentalEdge.createVehicleRentalEdge(vertex, rentalFormFactor);

    State[] traverse = edge.traverse(currentState);
    currentState =
      Arrays.stream(traverse).filter(it -> it.currentMode() != TraverseMode.WALK).findFirst().get();

    assertTrue(currentState.isRentingVehicle());

    var linkBack = StreetVehicleRentalLink.createStreetVehicleRentalLink(
      (VehicleRentalPlaceVertex) currentState.vertex,
      StreetModelForTest.intersectionVertex(count, count)
    );
    currentState = linkBack.traverse(currentState)[0];

    return this;
  }

  public State build() {
    return currentState;
  }
}
