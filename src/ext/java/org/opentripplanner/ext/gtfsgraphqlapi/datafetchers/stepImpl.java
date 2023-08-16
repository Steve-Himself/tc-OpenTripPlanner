package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLAbsoluteDirection;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLRelativeDirection;
import org.opentripplanner.ext.gtfsgraphqlapi.mapping.DirectionMapper;
import org.opentripplanner.ext.gtfsgraphqlapi.mapping.StreetNoteMapper;
import org.opentripplanner.model.plan.ElevationProfile.Step;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class stepImpl implements GraphQLDataFetchers.GraphQLStep {

  @Override
  public DataFetcher<GraphQLAbsoluteDirection> absoluteDirection() {
    return environment ->
      getSource(environment).getAbsoluteDirection().map(DirectionMapper::map).orElse(null);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment ->
      getSource(environment)
        .getStreetNotes()
        .stream()
        .map(StreetNoteMapper::mapStreetNoteToAlert)
        .toList();
  }

  @Override
  public DataFetcher<Boolean> area() {
    return environment -> getSource(environment).getArea();
  }

  @Override
  public DataFetcher<Boolean> bogusName() {
    return environment -> getSource(environment).getBogusName();
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).getDistance();
  }

  @Override
  public DataFetcher<Iterable<Step>> elevationProfile() {
    return environment -> getSource(environment).getElevationProfile().steps();
  }

  @Override
  public DataFetcher<String> exit() {
    return environment -> getSource(environment).isExit();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getStartLocation().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getStartLocation().longitude();
  }

  @Override
  public DataFetcher<GraphQLRelativeDirection> relativeDirection() {
    return environment -> DirectionMapper.map(getSource(environment).getRelativeDirection());
  }

  @Override
  public DataFetcher<Boolean> stayOn() {
    return environment -> getSource(environment).isStayOn();
  }

  @Override
  public DataFetcher<String> streetName() {
    return environment -> getSource(environment).getName().toString(environment.getLocale());
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).isWalkingBike();
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
