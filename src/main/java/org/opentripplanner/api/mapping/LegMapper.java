package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.model.plan.Leg;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LegMapper {

    public static List<ApiLeg> mapLegs(Collection<Leg> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(LegMapper::mapLeg).collect(Collectors.toList());
    }

    public static ApiLeg mapLeg(Leg domain) {
        if(domain == null) { return null; }
        ApiLeg api = new ApiLeg();
        api.startTime = domain.startTime;
        api.endTime = domain.endTime;
        api.departureDelay = domain.departureDelay;
        api.arrivalDelay = domain.arrivalDelay;
        api.realTime = domain.realTime;
        api.isNonExactFrequency = domain.isNonExactFrequency;
        api.headway = domain.headway;
        api.distance = domain.distance;
        api.pathway = domain.pathway;
        api.mode = domain.mode;
        api.route = domain.route;
        api.agencyName = domain.agencyName;
        api.agencyUrl = domain.agencyUrl;
        api.agencyBrandingUrl = domain.agencyBrandingUrl;
        api.agencyTimeZoneOffset = domain.agencyTimeZoneOffset;
        api.routeColor = domain.routeColor;
        api.routeType = domain.routeType;
        api.routeId = domain.routeId;
        api.routeTextColor = domain.routeTextColor;
        api.interlineWithPreviousLeg = domain.interlineWithPreviousLeg;
        api.tripShortName = domain.tripShortName;
        api.tripBlockId = domain.tripBlockId;
        api.headsign = domain.headsign;
        api.agencyId = domain.agencyId;
        api.tripId = domain.tripId;
        api.serviceDate = domain.serviceDate;
        api.routeBrandingUrl = domain.routeBrandingUrl;
        api.from = PlaceMapper.mapPlace(domain.from);
        api.to = PlaceMapper.mapPlace(domain.to);
        api.intermediateStops = PlaceMapper.mapPlaces(domain.intermediateStops);
        api.legGeometry = domain.legGeometry;
        api.walkSteps = domain.walkSteps;
        api.alerts = domain.alerts;
        api.alertPatches = domain.alertPatches;
        api.routeShortName = domain.routeShortName;
        api.routeLongName = domain.routeLongName;
        api.boardRule = domain.boardRule;
        api.alightRule = domain.alightRule;
        api.rentedBike = domain.rentedBike;
        return api;
    }

}
