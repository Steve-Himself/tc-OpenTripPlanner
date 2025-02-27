package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class DefaultAccessEgressTest {

  private static final int STOP = 5;
  private static final State LAST_STATE = TestStateBuilder.ofWalking().streetEdge().build();
  public static final Duration TIME_PENALTY = Duration.ofSeconds(1);
  public static final Cost COST_PENALTY = Cost.costOfSeconds(11);
  public static final TimeAndCost PENALTY = new TimeAndCost(TIME_PENALTY, COST_PENALTY);

  private final DefaultAccessEgress subject = new DefaultAccessEgress(STOP, LAST_STATE);
  private final DefaultAccessEgress subjectWithPenalty = subject.withPenalty(PENALTY);

  @Test
  void canNotAddPenaltyTwice() {
    assertThrows(IllegalStateException.class, () -> subjectWithPenalty.withPenalty(PENALTY));
  }

  @Test
  void durationInSeconds() {
    // TODO - The value is ?
    int expected = 118215;
    assertEquals(expected, subject.durationInSeconds());
    assertEquals(expected + TIME_PENALTY.toSeconds(), subjectWithPenalty.durationInSeconds());
  }

  @Test
  void stop() {
    assertEquals(STOP, subject.stop());
  }

  @Test
  void generalizedCost() {
    // TODO - The value is ?
    int expected = 23642959;
    assertEquals(expected, subject.generalizedCost());
    assertEquals(expected + COST_PENALTY.toCentiSeconds(), subjectWithPenalty.generalizedCost());
  }

  @Test
  void hasOpeningHours() {
    assertFalse(subject.hasOpeningHours());
  }

  @Test
  void getLastState() {
    assertEquals(LAST_STATE, subject.getLastState());
  }

  /**
   * @deprecated TODO - This test dos not test a single line in DefaultAccessEgress. If the
   *                    test have value move it to where it belong (StateTest ?).
   */
  @Deprecated
  @Test
  void containsDriving() {
    var state = TestStateBuilder.ofDriving().streetEdge().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, state);
    assertTrue(access.getLastState().containsModeCar());
  }

  /**
   * @deprecated TODO - This test dos not test a single line in DefaultAccessEgress. If the
   *                    test have value move it to where it belong (StateTest ?).
   */
  @Deprecated
  @Test
  void walking() {
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, state);
    assertFalse(access.getLastState().containsModeCar());
  }

  @Test
  void containsModeWalkOnly() {
    var stateWalk = TestStateBuilder.ofWalking().build();
    var subject = new DefaultAccessEgress(0, stateWalk);
    assertTrue(subject.isWalkOnly());

    var carRentalState = TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build();
    subject = new DefaultAccessEgress(0, carRentalState);
    assertFalse(subject.isWalkOnly());
  }

  @Test
  void hasPenalty() {
    assertFalse(subject.hasPenalty());
    assertFalse(subject.withPenalty(TimeAndCost.ZERO).hasPenalty());
    assertTrue(subjectWithPenalty.hasPenalty());
  }

  @Test
  void penalty() {
    assertEquals(TimeAndCost.ZERO, subject.penalty());
    assertEquals(PENALTY, subjectWithPenalty.penalty());
  }

  @Test
  void earliestDepartureTime() {
    assertEquals(89, subject.earliestDepartureTime(89));
  }

  @Test
  void latestArrivalTime() {
    assertEquals(89, subject.latestArrivalTime(89));
  }

  @Test
  void timeShiftDepartureTimeToActualTime() {
    assertEquals(89, subject.timeShiftDepartureTimeToActualTime(89));
    assertEquals(
      89 + PENALTY.timeInSeconds(),
      subjectWithPenalty.timeShiftDepartureTimeToActualTime(89)
    );
  }

  @Test
  void testToString() {
    assertEquals("Walk 1d8h50m15s $236429 ~ 5", subject.toString());
    assertEquals("Walk 1d8h50m16s $236440 w/penalty(1s $11) ~ 5", subjectWithPenalty.toString());
  }

  @Test
  void calculateEarliestDepartureTimeWithOpeningHours_NoPenalty() {
    final int requestedTime = 100;
    final int opensAtTime = 120;
    assertEquals(
      opensAtTime,
      subject.calculateEarliestDepartureTimeWithOpeningHours(
        requestedTime,
        v -> {
          assertEquals(requestedTime, v);
          return opensAtTime;
        }
      )
    );
  }

  @Test
  void calculateEarliestDepartureTimeWithOpeningHours_OpensAt() {
    final int requestedTime = 100;
    final int opensAtTime = 120;

    assertEquals(
      opensAtTime - PENALTY.timeInSeconds(),
      subjectWithPenalty.calculateEarliestDepartureTimeWithOpeningHours(
        requestedTime,
        v -> {
          assertEquals(requestedTime + PENALTY.timeInSeconds(), v);
          return opensAtTime;
        }
      )
    );
  }

  @Test
  void calculateEarliestDepartureTimeWithOpeningHours_Closed() {
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      subjectWithPenalty.calculateEarliestDepartureTimeWithOpeningHours(
        879789,
        v -> RaptorConstants.TIME_NOT_SET
      )
    );
  }

  @Test
  void calculateLatestArrivalTimeWithOpeningHours_NoPenalty() {
    final int requestedTime = 100;
    final int closesAtTime = 80;
    assertEquals(
      closesAtTime,
      subject.calculateLatestArrivalTimeWithOpeningHours(
        requestedTime,
        v -> {
          assertEquals(requestedTime, v);
          return closesAtTime;
        }
      )
    );
  }

  @Test
  void calculateLatestArrivalTimeWithOpeningHours_ClosesAt() {
    final int requestedTime = 100;
    final int closesAtTime = 80;

    assertEquals(
      closesAtTime + PENALTY.timeInSeconds(),
      subjectWithPenalty.calculateLatestArrivalTimeWithOpeningHours(
        requestedTime,
        v -> {
          assertEquals(requestedTime - PENALTY.timeInSeconds(), v);
          return closesAtTime;
        }
      )
    );
  }

  @Test
  void calculateLatestArrivalTimeWithOpeningHours_Closed() {
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      subjectWithPenalty.calculateLatestArrivalTimeWithOpeningHours(
        879789,
        v -> RaptorConstants.TIME_NOT_SET
      )
    );
  }
}
