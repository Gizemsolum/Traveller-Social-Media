package com.gizemsolum.travellerapplication;

public class ModelVideo {

    private String VideoId,Videoname, Videourl,vLikes, vComments, vTime, uName, uEmail, uid, uDp,search;

    public ModelVideo() {
    }

    public ModelVideo(String videoId, String videoname, String videourl, String vLikes, String vComments, String vTime, String uName, String uEmail, String uid, String uDp, String search) {
        VideoId = videoId;
        Videoname = videoname;
        Videourl = videourl;
        this.vLikes = vLikes;
        this.vComments = vComments;
        this.vTime = vTime;
        this.uName = uName;
        this.uEmail = uEmail;
        this.uid = uid;
        this.uDp = uDp;
        this.search = search;
    }

    public String getVideoId() {
        return VideoId;
    }

    public void setVideoId(String videoId) {
        VideoId = videoId;
    }

    public String getVideoname() {
        return Videoname;
    }

    public void setVideoname(String videoname) {
        Videoname = videoname;
    }

    public String getVideourl() {
        return Videourl;
    }

    public void setVideourl(String videourl) {
        Videourl = videourl;
    }

    public String getvLikes() {
        return vLikes;
    }

    public void setvLikes(String vLikes) {
        this.vLikes = vLikes;
    }

    public String getvComments() {
        return vComments;
    }

    public void setvComments(String vComments) {
        this.vComments = vComments;
    }

    public String getvTime() {
        return vTime;
    }

    public void setvTime(String vTime) {
        this.vTime = vTime;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getuEmail() {
        return uEmail;
    }

    public void setuEmail(String uEmail) {
        this.uEmail = uEmail;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getuDp() {
        return uDp;
    }

    public void setuDp(String uDp) {
        this.uDp = uDp;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}
