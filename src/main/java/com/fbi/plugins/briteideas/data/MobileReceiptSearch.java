package com.fbi.plugins.briteideas.data;

import com.fbi.fbdata.FBData;

import java.util.Date;

public class MobileReceiptSearch implements FBData<MobileReceiptSearch> {

    private int id;
    private String description;
    private Date timeStarted;
    private Date timeFinished;
    private Date timeUploaded;

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
    public int getId() {
        return id;
    }

    @Override
    public void setId(int i) {
        this.id = i;
    }

    @Override
    public int compareTo(MobileReceiptSearch mobileReceiptSearch) {
        return 0;
    }
}
