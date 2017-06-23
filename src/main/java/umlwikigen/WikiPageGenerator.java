package main.java.umlwikigen;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class WikiPageGenerator {
    private final int FILE_EXTENSION = 5;

    String generateWikiString(String imgDest, File project) {
        String projectName = project.getName().substring(0, project.getName()
                .length() - FILE_EXTENSION);
        String wikiPage = "<html><title>"+projectName+"</title><body>";
        File dir = new File(imgDest);
        File[] dirList = dir.listFiles();
        if (dirList != null) {
            for (File img : dirList) {
                wikiPage += "<img src=\"" + img.getAbsoluteFile() + "/><br/>";
            }
            wikiPage += "</body></html>";
        }
        else {
            throw new FileSystemNotFoundException();
        }
        return wikiPage;
    }
}
