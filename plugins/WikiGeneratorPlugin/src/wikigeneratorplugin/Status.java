package wikigeneratorplugin;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 7/20/2017
 */
public enum Status {
    CREATED("created"), UPDATED("updated");

    private String s;

    Status(String s) {
        this.s = s;
    }

    public String getString() {
        return s;
    }
}
