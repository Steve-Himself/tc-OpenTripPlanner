package org.opentripplanner.gtfs.graphbuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs_transformer.IGtfsTransformer;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.onebusaway.gtfs_transformer.services.GtfsEntityTransformStrategy;
import org.onebusaway.gtfs_transformer.services.GtfsTransformStrategy;
import org.onebusaway.gtfs_transformer.services.TransformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsTransformer implements IGtfsTransformer {

  private static Logger _log = LoggerFactory.getLogger(GtfsTransformer.class);

  private File _gtfsReferenceDirectory;

  private List<GtfsTransformStrategy> _transformStrategies = new ArrayList<GtfsTransformStrategy>();

  private TransformContext _context = new TransformContext();

  private GtfsReader _reader = new GtfsReader();

  private GtfsReader _referenceReader = new GtfsReader();

  private GtfsMutableRelationalDao _dao = new GtfsRelationalDaoImpl();

  private String _agencyId;

  private Map<String, Object> _parameters = new HashMap<>();

  private TransformFactory _transformFactory = new TransformFactory(this);

  public GtfsTransformer(GtfsReader reader, GtfsRelationalDaoImpl dao) {
    this._reader = reader;
    this._dao = dao;
  }

  public void setGtfsReferenceDirectory(File referenceDirectory) {
    _gtfsReferenceDirectory = referenceDirectory;
  }

  public void addTransform(GtfsTransformStrategy strategy) {
    _transformStrategies.add(strategy);
  }

  public List<GtfsTransformStrategy> getTransforms() {
    return _transformStrategies;
  }

  public GtfsTransformStrategy getLastTransform() {
    if (_transformStrategies.isEmpty()) return null;
    return _transformStrategies.get(_transformStrategies.size() - 1);
  }

  public void addEntityTransform(GtfsEntityTransformStrategy entityTransform) {}

  public void addParameter(String key, Object value) {
    _parameters.put(key, value);
  }

  public void setAgencyId(String agencyId) {
    _agencyId = agencyId;
  }

  @Override
  public GtfsReader getReader() {
    return _reader;
  }

  public GtfsReader getReferenceReader() {
    return _referenceReader;
  }

  @Override
  public GtfsRelationalDao getDao() {
    return _dao;
  }

  public TransformFactory getTransformFactory() {
    return _transformFactory;
  }

  public void run() throws Exception {
    // copy over parameters
    for (String key : _parameters.keySet()) {
      _context.putParameter(key, _parameters.get(key));
    }

    readGtfs();
    if (_gtfsReferenceDirectory != null && _gtfsReferenceDirectory.exists()) {
      readReferenceGtfs();
    } else {
      _log.trace("reference GTFS not found, continuing");
    }

    _context.setDefaultAgencyId(_reader.getDefaultAgencyId());
    _context.setReader(_reader);

    updateGtfs();
  }

  /****
   * Protected Methods
   ****/

  /****
   * Private Methods
   ****/

  private void readGtfs() throws IOException {
    DefaultEntitySchemaFactory schemaFactory = new DefaultEntitySchemaFactory();
    schemaFactory.addFactory(GtfsEntitySchemaFactory.createEntitySchemaFactory());
    _transformStrategies.forEach(s -> s.updateReadSchema(schemaFactory));
    _reader.setEntitySchemaFactory(schemaFactory);
  }

  private void readReferenceGtfs() throws IOException {
    _log.info("reading reference GTFS at " + _gtfsReferenceDirectory);
    GenericMutableDao dao = new GtfsRelationalDaoImpl();
    _referenceReader.setEntityStore(dao);

    if (_agencyId != null) _referenceReader.setDefaultAgencyId(_agencyId);

    _referenceReader.setInputLocation(_gtfsReferenceDirectory);
    _referenceReader.run();
    _context.setReferenceReader(_referenceReader);
  }

  private void updateGtfs() {
    for (GtfsTransformStrategy strategy : _transformStrategies) {
      String strategyName = strategy.toString();
      try {
        strategyName = strategy.getName();
      } catch (AbstractMethodError ame) {
        _log.info("(AbstractMethodError) strategy " + strategy + " does not support getName");
      }
      _log.info("Running strategy {} ....", strategyName);
      try {
        strategy.run(_context, _dao);
      } catch (Throwable t) {
        _log.error("Exception in strategy (v1) " + strategyName, t);
        throw new RuntimeException(t);
      }
      _log.info("Strategy {} complete.", strategyName);
    }
  }
}
