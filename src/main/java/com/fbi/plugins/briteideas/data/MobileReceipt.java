package com.fbi.plugins.briteideas.data;

import com.fbi.fbdata.FBData;

import java.util.Date;

public class MobileReceipt implements FBData{

    private int id;
    private int mr_id;
    private String description;
    private Date timeStarted;
    private Date timeFinished;
    private Date timeUploaded;

    public MobileReceipt(int id, int mr_id, String description, Date timeStarted, Date timeFinished, Date timeUploaded){
        this.setId(id);
        this.setMr_id(mr_id);
        this.setDescription(description);
        this.setTimeStarted(timeStarted);
        this.setTimeFinished(timeFinished);
        this.setTimeUploaded(timeUploaded);

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMr_id() {
        return mr_id;
    }

    public void setMr_id(int mr_id) {
        this.mr_id = mr_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(Date timeStarted) {
        this.timeStarted = timeStarted;
    }

    public Date getTimeFinished() {
        return timeFinished;
    }

    public void setTimeFinished(Date timeFinished) {
        this.timeFinished = timeFinished;
    }

    public Date getTimeUploaded() {
        return timeUploaded;
    }

    public void setTimeUploaded(Date timeUploaded) {
        this.timeUploaded = timeUploaded;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
