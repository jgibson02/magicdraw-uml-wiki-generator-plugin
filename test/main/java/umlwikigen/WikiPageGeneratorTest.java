package main.java.umlwikigen;

import junit.framework.TestCase;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;

/**
 * Test WikiPageGeneratorTest class
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class WikiPageGeneratorTest extends TestCase {
    /**
     * Give wikiPageGen a test file path and test project and give badGen a bad fileLoc
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test to make sure the correct boolean is returned for a succesful
     * generation and a failed generation
     */
    public void testGenerateWikiString() {
        String wikiPageEqual = "<html><title>testProject</title><body><img " +
                "src=\"C:\\Users\\kkabdolh\\Documents\\UMLWikiGenerator\\test\\resources\\imgs\\img1.jpg/><br/><img src=\"C:\\Users\\kkabdolh\\Documents\\UMLWikiGenerator\\test\\resources\\imgs\\img2.jpg/><br/></body></html>";
        String goodOutput = WikiPageGenerator.generateWikiString("test\\resources\\imgs", new File
                ("test\\resources\\testProject.mzip"));;
        Exception thrown = null;
        try {
            WikiPageGenerator.generateWikiString("badfilepath", new File
                    ("test\\resources\\testProject.mzip"));
        } catch (FileSystemNotFoundException e){
            thrown = e;
        }
        assertNotNull(thrown);
        assertTrue(wikiPageEqual.equals(goodOutput));
    }
}