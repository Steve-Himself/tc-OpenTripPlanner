/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.api.model;

import java.io.Serializable;
import java.util.Objects;

public class ApiStopGroup implements Serializable {

  public String id;
  public String name;
  public Double lat;
  public Double lon;
  public String desc;
  public String zoneId;
  public String url;
  public Integer locationType;

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiStopGroup apiStop = (ApiStopGroup) o;
    return id.equals(apiStop.id);
  }

  @Override
  public String toString() {
    return "<Stop " + this.id + ">";
  }
}
