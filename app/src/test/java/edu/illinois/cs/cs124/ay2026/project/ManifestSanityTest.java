package edu.illinois.cs.cs124.ay2026.project;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Regression for the preloaded_fonts crash:
 *   android.content.res.Resources$NotFoundException: Array resource ID
 *
 * Android's preloadFonts() calls obtainTypedArray(), which requires an @array/ resource.
 * If a <meta-data android:name="preloaded_fonts"> block references @xml/preloaded_fonts
 * instead of a typed array, the app crashes at startup before any Activity runs.
 *
 * This test ensures that meta-data element is absent from the manifest.
 */
public class ManifestSanityTest {

    @Test
    public void manifest_doesNotContainPreloadedFontsMetaData() throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(manifest);

        NodeList metaDataNodes = doc.getElementsByTagName("meta-data");
        for (int i = 0; i < metaDataNodes.getLength(); i++) {
            String name = metaDataNodes.item(i).getAttributes()
                    .getNamedItem("android:name").getNodeValue();
            assertEquals(
                    "Found <meta-data android:name=\"preloaded_fonts\"> in AndroidManifest.xml. "
                    + "This causes a startup crash because Android's preloadFonts() requires an "
                    + "@array/ resource, not @xml/. Remove the meta-data block - embedded fonts "
                    + "don't need preloading hints.",
                    false,
                    "preloaded_fonts".equals(name)
            );
        }
    }
}