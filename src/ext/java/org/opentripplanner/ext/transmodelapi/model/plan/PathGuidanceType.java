package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.plan.WalkStep;

public class PathGuidanceType {

  public static GraphQLObjectType create(GraphQLObjectType elevationStepType) {
    return GraphQLObjectType
      .newObject()
      .name("PathGuidance")
      .description("A series of turn by turn instructions used for walking, biking and driving.")
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("distance")
          .description("The distance in meters that this step takes.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).getDistance())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("relativeDirection")
          .description("The relative direction of this step.")
          .type(EnumTypes.RELATIVE_DIRECTION)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).getRelativeDirection())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("streetName")
          .description("The name of the street.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            GraphQLUtils.getTranslation(
              ((WalkStep) environment.getSource()).getName(),
              environment
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("heading")
          .description("The absolute direction of this step.")
          .type(EnumTypes.ABSOLUTE_DIRECTION)
          .dataFetcher(environment ->
            ((WalkStep) environment.getSource()).getAbsoluteDirection().orElse(null)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("exit")
          .description("When exiting a highway or traffic circle, the exit name/number.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).isExit())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stayOn")
          .description("Indicates whether or not a street changes direction at an intersection.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).isStayOn())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("area")
          .description(
            "This step is on an open area, such as a plaza or train platform, and thus the directions should say something like \"cross\""
          )
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).getArea())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bogusName")
          .description(
            "The name of this street was generated by the system, so we should only display it once, and generally just display right/left directions"
          )
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment -> ((WalkStep) environment.getSource()).getBogusName())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("latitude")
          .description("The latitude of the step.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            ((WalkStep) environment.getSource()).getStartLocation().latitude()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("longitude")
          .description("The longitude of the step.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            ((WalkStep) environment.getSource()).getStartLocation().longitude()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("elevationProfile")
          .description(ElevationProfileStepType.makeDescription("step"))
          .type(new GraphQLNonNull(new GraphQLList(elevationStepType)))
          .dataFetcher(environment ->
            ElevationProfileStepType.mapElevationProfile(
              ((WalkStep) environment.getSource()).getElevationProfile()
            )
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("legStepText")
      //                        .description("Direction information as readable text.")
      //                        .type(Scalars.GraphQLString)
      //                        .argument(GraphQLArgument.newArgument()
      //                                .name("locale")
      //                                .type(localeEnum)
      //                                .defaultValue("no")
      //                                .build())
      //                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).getLegStepText(environment))
      //                        .build())
      .build();
  }
}
