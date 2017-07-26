package wikigeneratorplugin;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 7/20/2017
 *
 * Diagram status, whether it has been created or updated, used for the email
 * update
 */
public enum Status {
    CREATED("created"), UPDATED("updated");

    private String s;

    Status(String s) {
        this.s = s;
    }

    /**
     * Returns string version of enum
     * @return string name
     */
    public String getString() {
        return s;
    }
}
