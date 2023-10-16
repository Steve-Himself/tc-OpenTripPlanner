package org.opentripplanner.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiStopGroupShort {

  public String id;
  public String code;
  public String name;
  public double lat;
  public double lon;
  public String url;

  /** Distance to the stop when it is returned from a location-based query. */
  @JsonInclude(Include.NON_NULL)
  public Integer dist;
}
