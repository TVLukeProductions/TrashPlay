package de.lukeslog.trashplay.badges;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Badge")
public class Badge  {

    @Column(name = "title")
    private String title="";

    @Column(name = "description") 
    private String description="";

    @Column(name = "type")
    private String type="";

    @Column(name = "awardsMessage")
    private String awardsMessage="";

    @Column(name = "activated")
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
