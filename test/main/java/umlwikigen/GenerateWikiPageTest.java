package main.java.umlwikigen;

import junit.framework.TestCase;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;

/**
 * Test GenerateWikiPageTest class
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class GenerateWikiPageTest extends TestCase {

    private GenerateWikiPage wikiPageGen; // Ideal senario
    private GenerateWikiPage badGen; // Inccorect file path

    /**
     * Give wikiPageGen a test file path and test project and give badGen a
     * bad fileLoc
     */
    public void setUp() throws Exception {
        super.setUp();
        wikiPageGen = new GenerateWikiPage("" +
                "test\\resources\\imgs", new File
                ("test\\resources\\testProject.mzip"));
        badGen = new GenerateWikiPage("badfilepath", new File
                ("test\\resources\\testProject.mzip"));
    }

    /**
     * Test to make sure the correct boolean is returned for a succesful
     * generation and a failed generation
     */
    public void testGenerateWikiString() {
        String wikiPageEqual = "<html><title>testProject</title><body><img " +
                "src=\"C:\\Users\\kkabdolh\\Documents\\UMLWikiGenerator\\test\\resources\\imgs\\img1.jpg/><br/><img src=\"C:\\Users\\kkabdolh\\Documents\\UMLWikiGenerator\\test\\resources\\imgs\\img2.jpg/><br/></body></html>";
        Exception thrown = null;
        try {
            badGen.generateWikiString();
        } catch (FileSystemNotFoundException e){
            thrown = e;
        }
        assertNotNull(thrown);
        assertTrue(wikiPageEqual.equals(wikiPageGen.generateWikiString()));
    }
}