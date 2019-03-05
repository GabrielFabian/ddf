/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import org.junit.Before;
import org.junit.Test;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RunWith(MockitoJUnitRunner.class)
public class KmzTransformerImplTest {

    private KmzTransformerImpl kmzTransformer;

    @Mock
    private KmlTransformerImpl kmlTransformer;

    static final String KML_EXTENSION = ".kml";

    private static MimeType KMZ_MIMETYPE;

    @Before
    public void setup() throws IOException, MimeTypeParseException {
        kmzTransformer = new KmzTransformerImpl(kmlTransformer);
        KMZ_MIMETYPE = new MimeType("application/vnd.google-earth.kmz");
    }

    @Test
    public void testKmlToKmzTransform() throws IOException {
        final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
        BinaryContent inputKmlFile = new BinaryContentImpl(resourceAsStream);
        BinaryContent kmz = kmzTransformer.kmlToKmzTransform(inputKmlFile);
        assertThat(kmz.getMimeType().match(KMZ_MIMETYPE), is(true));

        String outputKml = getOutputFromBinaryContent(kmz);
        assertThat(outputKml, is(resourceToString("/kmlPoint.kml")));
    }

    @Test
    public void testQueryResponseTransformation() throws IOException {
        //TODO
        assert true;
    }

    @Test
    public void testMetacardTransformation() throws IOException {
        //TODO
        assert true;
    }

    private InputStream getResourceAsStream(String resourcePath) {
        return this.getClass().getResourceAsStream(resourcePath);
    }

    private String getOutputFromBinaryContent(BinaryContent binaryContent) throws IOException {
        // BC is a kmz zip file containing a single kml file called doc.kml.
        // Optionally, relative file links will exist in folder called files
        String outputKml;
        try (ZipInputStream zipInputStream = new ZipInputStream(binaryContent.getInputStream())) {

            ZipEntry entry;
            outputKml = "";
            while ((entry = zipInputStream.getNextEntry()) != null) {

                // According to Google, a .kmz should only contain a single .kml file
                // so we stop at the first one we find.
                final String fileName = entry.getName();
                if (fileName.endsWith(KML_EXTENSION)) {
                    assertThat(fileName, is("doc.kml"));
                    outputKml = readContentsFromZipInputStream(zipInputStream);
                    break;
                }
            }
        }
        return outputKml;
    }

    private String resourceToString(String resourceName) throws IOException {
        try (final InputStream inputStream = getResourceAsStream(resourceName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        }
    }

    private String readContentsFromZipInputStream(ZipInputStream zipInputStream) throws IOException {
        String s = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8.name());
        IOUtils.closeQuietly(zipInputStream);
        return s;
    }

}
