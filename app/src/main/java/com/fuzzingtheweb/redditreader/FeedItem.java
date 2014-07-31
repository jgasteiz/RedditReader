package com.fuzzingtheweb.redditreader;

public class FeedItem {
    private String mTitle;
    private String mUrl;
    private String mSubReddit;
    private String mDomain;
    private String mId;
    private String mAuthor;
    private String mScore;
    private String mThumbnail;
    private String mPermalink;
    private String mCreated;
    private String mNumComments;

    public FeedItem(String title, String url, String subReddit, String id,
                    String domain,String author, String score, String thumbnail,
                    String permalink, String created, String numComments) {
        mTitle = title;
        mUrl = url;
        mSubReddit = subReddit;
        mId = id;
        mDomain = domain;
        mAuthor = author;
        mScore = score;
        mThumbnail = thumbnail;
        mPermalink = permalink;
        mCreated = created;
        mNumComments = numComments;
    }

    public String getTitle() {
        return mTitle;
    }
    public String getUrl() {
        return mUrl;
    }
    public String getSubReddit() {
        return mSubReddit;
    }
    public String getDomain() {
        return mDomain;
    }
    public String getId() {
        return mId;
    }
    public String getAuthor() {
        return mAuthor;
    }
    public String getScore() {
        return mScore;
    }
    public String getThumbnail() {
        return mThumbnail;
    }
    public String getPermalink() {
        return mPermalink;
    }
    public String getCreated() {
        return mCreated;
    }
    public String getNumComments() {
        return mNumComments;
    }
}
