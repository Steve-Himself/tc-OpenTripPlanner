package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParametersBuilder;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class map GTFS build configuration from JSON to Java objects.
 */
public class GtfsConfig {

  public static GtfsFeedParameters mapGtfsDefaultParameters(
    NodeAdapter root,
    String parameterName
  ) {
    var node = root
      .of(parameterName)
      .since(V2_3)
      .summary("The gtfsDefaults section allows you to specify default properties for GTFS files.")
      .asObject();

    return mapGenericParameters(node);
  }

  public static GtfsFeedParameters mapGtfsFeed(NodeAdapter node, GtfsFeedParameters defaults) {
    return defaults
      .copyOf()
      .withFeedId(
        node
          .of("feedId")
          .since(V2_2)
          .summary(
            "The unique ID for this feed. This overrides any feed ID defined within the feed itself."
          )
          .asString(null)
      )
      .withSource(
        node.of("source").since(V2_2).summary("The unique URI pointing to the data file.").asUri()
      )
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary("Should consecutive identical stops be merged into one stop time entry.")
          .asBoolean(defaults.removeRepeatedStops())
      )
      .withStationTransferPreference(
        node
          .of("stationTransferPreference")
          .since(V2_3)
          .summary(
            "Should there be some preference or aversion for transfers at stops that are part of a station."
          )
          .description(
            """
            This parameter sets the generic level of preference. What is the actual cost can be changed
            with the `stopTransferCost` parameter in the router configuration.
            """
          )
          .asEnum(defaults.stationTransferPreference())
      )
      .withDiscardMinTransferTimes(
        node
          .of("discardMinTransferTimes")
          .since(V2_3)
          .summary("Should minimum transfer times in GTFS files be discarded.")
          .description(
            """
            This is useful eg. when the minimum transfer time is only set for ticketing purposes,
            but we want to calculate the transfers always from OSM data.
            """
          )
          .asBoolean(defaults.discardMinTransferTimes())
      )
      .withBlockBasedInterlining(
        node
          .of("blockBasedInterlining")
          .since(V2_3)
          .summary(
            "Whether to create stay-seated transfers in between two trips with the same block id."
          )
          .asBoolean(defaults.blockBasedInterlining())
      )
      .withMaxInterlineDistance(
        node
          .of("maxInterlineDistance")
          .since(V2_3)
          .summary(
            "Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle."
          )
          .asInt(defaults.maxInterlineDistance())
      )
      .build();
  }

  private static GtfsFeedParameters mapGenericParameters(NodeAdapter node) {
    return new GtfsFeedParametersBuilder()
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary("Should consecutive identical stops be merged into one stop time entry")
          .asBoolean(GtfsFeedParameters.DEFAULT_REMOVE_REPEATED_STOPS)
      )
      .withStationTransferPreference(
        node
          .of("stationTransferPreference")
          .since(V2_3)
          .summary(
            "Should there be some preference or aversion for transfers at stops that are part of a station."
          )
          .description(
            """
            This parameter sets the generic level of preference. What is the actual cost can be changed
            with the `stopTransferCost` parameter in the router configuration.
            """
          )
          .asEnum(GtfsFeedParameters.DEFAULT_STATION_TRANSFER_PREFERENCE)
      )
      .withDiscardMinTransferTimes(
        node
          .of("discardMinTransferTimes")
          .since(V2_3)
          .summary("Should minimum transfer times in GTFS files be discarded.")
          .description(
            """
            This is useful eg. when the minimum transfer time is only set for ticketing purposes,
            but we want to calculate the transfers always from OSM data.
            """
          )
          .asBoolean(GtfsFeedParameters.DEFAULT_DISCARD_MIN_TRANSFER_TIMES)
      )
      .withBlockBasedInterlining(
        node
          .of("blockBasedInterlining")
          .since(V2_3)
          .summary(
            "Whether to create stay-seated transfers in between two trips with the same block id."
          )
          .asBoolean(GtfsFeedParameters.DEFAULT_BLOCK_BASED_INTERLINING)
      )
      .withMaxInterlineDistance(
        node
          .of("maxInterlineDistance")
          .since(V2_3)
          .summary(
            "Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle."
          )
          .asInt(GtfsFeedParameters.DEFAULT_MAX_INTERLINE_DISTANCE)
      )
      .build();
  }
}
