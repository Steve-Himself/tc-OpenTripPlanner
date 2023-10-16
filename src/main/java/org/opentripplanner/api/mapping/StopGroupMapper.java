package org.opentripplanner.api.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiStopGroup;
import org.opentripplanner.api.model.ApiStopGroupShort;
import org.opentripplanner.transit.model.site.StopLocationsGroup;

public class StopGroupMapper {

  public static List<ApiStopGroup> mapToApi(Collection<StopLocationsGroup> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(StopGroupMapper::mapToApi).collect(Collectors.toList());
  }

  public static ApiStopGroup mapToApi(StopLocationsGroup domain) {
    return mapToApi(domain, true);
  }

  public static ApiStopGroup mapToApi(StopLocationsGroup domain, boolean extended) {
    if (domain == null) {
      return null;
    }

    ApiStopGroup api = new ApiStopGroup();
    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.name = I18NStringMapper.mapToApi(domain.getName(), null);
    api.lat = domain.getLat();
    api.lon = domain.getLon();
    api.name = domain.getName().toString();
    api.locationType = 1;
    return api;
  }

  public static ApiStopGroupShort mapToApiShort(StopLocationsGroup domain) {
    if (domain == null) {
      return null;
    }

    ApiStopGroupShort api = new ApiStopGroupShort();
    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.name = I18NStringMapper.mapToApi(domain.getName(), null);
    api.lat = domain.getLat();
    api.lon = domain.getLon();

    return api;
  }

  /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
  public static ApiStopGroupShort mapToApiShort(StopLocationsGroup domain, double distance) {
    if (domain == null) {
      return null;
    }

    ApiStopGroupShort api = mapToApiShort(domain);
    api.dist = (int) distance;

    return api;
  }

  public static List<ApiStopGroupShort> mapToApiShort(Collection<StopLocationsGroup> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(StopGroupMapper::mapToApiShort).toList();
  }
}
