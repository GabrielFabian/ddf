package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmzTransformerImpl implements KmzTransformer {

  private static final MimeType KMZ_MIMETYPE;

  private static final String DOC_KML = "doc.kml";

  private static final Logger LOGGER = LoggerFactory.getLogger(KmzTransformerImpl.class);

  static {
    try {
      KMZ_MIMETYPE = new MimeType("application/vnd.google-earth.kmz");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private Map<String, BinaryContent> supportingDocuments;

  private KmlTransformer kmlTransformer;

  public KmzTransformerImpl(KmlTransformer kmlTransformer) {
    this.kmlTransformer = kmlTransformer;
  }

  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    BinaryContent unzippedKml = kmlTransformer.transform(upstreamResponse, arguments);
    BinaryContent kmz = kmlToKmzTransform(unzippedKml);
    if(kmz == null){
      throw new CatalogTransformerException(
              String.format("Unable to transform KML to KMZ."));
    }
    return kmz;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    BinaryContent unzippedKml = kmlTransformer.transform(metacard, arguments);
    BinaryContent kmz = kmlToKmzTransform(unzippedKml);

    if(kmz == null) {
      throw new CatalogTransformerException(
              String.format("Unable to transform to KMZ for metacard ID: %s", metacard.getId()));
    }
    return kmz;
  }

  private BinaryContent kmlToKmzTransform(BinaryContent unzippedKml) {
    InputStream inputStream = unzippedKml.getInputStream();
    TemporaryFileBackedOutputStream temporaryFileBackedOutputStream =
        new TemporaryFileBackedOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(temporaryFileBackedOutputStream);

    try {
      final ZipEntry e = new ZipEntry(DOC_KML);
      zipOutputStream.putNextEntry(e);
      IOUtils.copy(inputStream, zipOutputStream);
      zipOutputStream.closeEntry();
      zipOutputStream.finish();
      final InputStream zipFile = temporaryFileBackedOutputStream.asByteSource().openStream();
      final BinaryContentImpl binaryContent = new BinaryContentImpl(zipFile, KMZ_MIMETYPE);
      return binaryContent;
    } catch (IOException e) {
      LOGGER.debug("Failed to create KMZ file from KML BinaryContent.", e);
    }
    return null;
  }
}
