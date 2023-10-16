/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentripplanner.gtfs.graphbuilder;

import static org.onebusaway.gtfs_transformer.csv.CSVUtil.readCsv;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs_transformer.services.GtfsTransformStrategy;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeStopFromStopExtStrategy implements GtfsTransformStrategy {

  private final Logger _log = LoggerFactory.getLogger(MergeStopFromStopExtStrategy.class);

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  public static <T> List<T> readCsv(
    final Class<T> klass,
    InputStreamReader inputStream,
    CsvEntityReader reader
  ) {
    if (reader == null) {
      reader = new CsvEntityReader();
    }
    final List<T> ret = new ArrayList<>();
    reader.addEntityHandler(
      new EntityHandler() {
        @Override
        public void handleEntity(Object o) {
          ret.add(klass.cast(o));
        }
      }
    );
    try {
      reader.readEntities(klass, inputStream);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ret;
  }

  @Override
  public void run(TransformContext context, GtfsMutableRelationalDao dao) {
    try {
      CsvInputSource inputSource = context.getReader().getInputSource();

      if (inputSource.hasResource("stops_ext.txt")) {
        HashMap<String, Stop> referenceStops = new HashMap<>();
        CsvEntityReader reader = new CsvEntityReader();
        reader.setInputSource(inputSource);
        reader.getContext().put(GtfsReader.KEY_CONTEXT, context.getReader().getGtfsReaderContext());

        for (Stop stop : readCsv(
          Stop.class,
          new InputStreamReader(inputSource.getResource("stops_ext.txt")),
          reader
        )) {
          referenceStops.put(stop.getId().getId(), stop);
        }
        int stopCount = 0;
        for (Stop stop : dao.getAllStops()) {
          String identifier = stop.getId().getId();

          Stop refStop = referenceStops.get(identifier);
          if (refStop != null) {
            stopCount++;
            stop.setIsPublic(refStop.getIsPublic());
            stop.setDistrictId(refStop.getDistrictId());
            stop.setDistrictName(refStop.getDistrictName());
            stop.setMunicipality(refStop.getMunicipality());
          }
        }

        _log.info("found {} stop extensions, matched {} stops", referenceStops.size(), stopCount);
      }
    } catch (Exception e) {
      _log.error("An error occurred while processing stop extension");
    }
  }
}
