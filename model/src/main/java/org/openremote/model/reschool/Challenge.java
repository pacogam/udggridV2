package org.openremote.model.reschool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Challenge {

    protected Date startDate;
    protected Date endDate;
    protected Date joinedAt;
    protected Boolean inProgress;
    protected Integer points;
    protected Integer pointsCurrentMax;
    protected Integer pointsTotalMax;

    public Challenge() {

    }

    public Challenge setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Challenge setStartDate(String stringDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.startDate = sdf.parse(stringDate);
        return this;
    }

    public Challenge setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public Challenge setEndDate(String stringDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.endDate = sdf.parse(stringDate);
        return this;
    }

    public Challenge setJoinDate(Date joinedAt) {
        this.joinedAt = joinedAt;
        return this;
    }

    public Challenge setJoinDate(String stringDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.joinedAt = sdf.parse(stringDate);
        return this;
    }

    public Challenge setInProgress(Boolean progress) {
        this.inProgress = progress;
        return this;
    }

    public Challenge setPoints(Integer points) {
        this.points = points;
        return this;
    }

    public Challenge setPointsCurrentMax(Integer pointsCurrentMax) {
        this.pointsCurrentMax = pointsCurrentMax;
        return this;
    }

    public Challenge setPointsTotalMax(Integer pointsTotalMax) {
        this.pointsTotalMax = pointsTotalMax;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public Boolean getInProgress() {
        return inProgress;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getPointsCurrentMax() {
        return pointsCurrentMax;
    }

    public Integer getPointsTotalMax() {
        return pointsTotalMax;
    }
}
