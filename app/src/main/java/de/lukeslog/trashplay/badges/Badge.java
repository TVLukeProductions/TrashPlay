package de.lukeslog.trashplay.badges;

import io.realm.RealmObject;
import io.realm.annotations.RealmClass;

@RealmClass
public class Badge  extends RealmObject {

    private String title="";
    private String description="";
    private String type="";
    private String awardsMessage="";
    private boolean activated=false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAwardsMessage() {
        return awardsMessage;
    }

    public void setAwardsMessage(String awardsMessage) {
        this.awardsMessage = awardsMessage;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}
