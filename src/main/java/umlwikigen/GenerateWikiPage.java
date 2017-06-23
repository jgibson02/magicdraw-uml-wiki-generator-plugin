package main.java.umlwikigen;

import java.io.File;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class GenerateWikiPage {
    private String wikiPage;
    private String imgDest;

    public GenerateWikiPage(String imgDest) {
        this.wikiPage = "";
        this.imgDest = imgDest;
        generate();
    }

    private void generate() {
        File dir = new File(imgDest);
        File[] dirList = dir.listFiles();

        //wikiPage += "<img src=\"" + img.getAbsoluteFile() + "/><br/>";
    }
}
