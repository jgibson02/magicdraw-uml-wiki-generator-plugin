package main.java.umlwikigen;

import junit.framework.TestCase;

import java.io.File;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class GenerateWikiPageTest extends TestCase {

    private GenerateWikiPage wikiPageGen;

    public void setUp() {
        wikiPageGen = new GenerateWikiPage("" +
                "..\\..\\..\\resources\\imgs", new File
                ("..\\..\\..\\resources\\testProject.mzip")); // test\resources\imgs
    }

    public void testGenerate() {
        assertTrue(wikiPageGen.generate());
        
    }
}