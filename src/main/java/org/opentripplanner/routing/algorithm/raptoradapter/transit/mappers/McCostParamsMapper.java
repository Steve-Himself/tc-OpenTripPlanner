package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.McCostParams;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.McCostParamsBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class McCostParamsMapper {

  public static McCostParams map(
    RoutingRequest request,
    Function<FeedScopedId, Collection<FeedScopedId>> routesByAgency
  ) {
    McCostParamsBuilder builder = new McCostParamsBuilder();

    builder.transferCost(request.transferCost).waitReluctanceFactor(request.waitReluctance);

    if (request.modes.transferMode == StreetMode.BIKE) {
      builder.boardCost(request.bikeBoardCost);
    } else {
      builder.boardCost(request.walkBoardCost);
    }
    builder.transitReluctanceFactors(mapTransitReluctance(request.transitReluctanceForMode()));

    builder.wheelchairAccessibility(request.wheelchairAccessibility);

    var unpreferredRoutes = new HashSet<>(
      request
        .getUnpreferredAgencies()
        .stream()
        .map(routesByAgency)
        .flatMap(Collection::stream)
        .toList()
    );
    unpreferredRoutes.addAll(request.getUnpreferredRoutes());
    builder.unpreferredRoutes(unpreferredRoutes);

    builder.unpreferredCost(request.unpreferredRouteCost);

    return builder.build();
  }

  public static double[] mapTransitReluctance(Map<TransitMode, Double> map) {
    if (map.isEmpty()) {
      return null;
    }

    // The transit reluctance is arranged in an array with the {@link TransitMode} ordinal
    // as an index. This make the lookup very fast and the size of the array small.
    // We could get away with a smaller array if we kept an index from mode to index
    // and passed that into the transit layer and used it to set the
    // {@link TripScheduleWithOffset#transitReluctanceIndex}, but this is difficult with the
    // current transit model design.
    double[] transitReluctance = new double[TransitMode.values().length];
    Arrays.fill(transitReluctance, McCostParams.DEFAULT_TRANSIT_RELUCTANCE);
    for (TransitMode mode : map.keySet()) {
      transitReluctance[mode.ordinal()] = map.get(mode);
    }
    return transitReluctance;
  }
}
