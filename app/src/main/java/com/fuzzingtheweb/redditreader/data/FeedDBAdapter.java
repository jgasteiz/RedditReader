package com.fuzzingtheweb.redditreader.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.fuzzingtheweb.redditreader.FeedItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class FeedDBAdapter {

    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;

    public static final String POSTS_TABLE_NAME = "posts";
    public static final String _ID = "_id";

    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_DOMAIN = "domain";
    public static final String COLUMN_REDDIT_ID = "reddit_id";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_THUMBNAIL = "thumbnail";
    public static final String COLUMN_PERMALINK = "permalink";
    public static final String COLUMN_CREATED = "created";
    public static final String COLUMN_NUM_COMMENTS = "num_comments";

    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String LOG_TAG = FeedDBAdapter.class.getSimpleName();
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_CREATE = "CREATE TABLE " + POSTS_TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_TITLE + " TEXT NOT NULL, " +
            COLUMN_URL + " TEXT NOT NULL, " +
            COLUMN_SUBREDDIT + " TEXT NOT NULL, " +
            COLUMN_DOMAIN + " TEXT NOT NULL, " +
            COLUMN_REDDIT_ID + " TEXT NOT NULL, " +
            COLUMN_AUTHOR + " TEXT NOT NULL, " +
            COLUMN_SCORE + " TEXT NOT NULL, " +
            COLUMN_THUMBNAIL + " TEXT NOT NULL, " +
            COLUMN_PERMALINK + " TEXT NOT NULL, " +
            COLUMN_CREATED + " TEXT NOT NULL, " +
            COLUMN_NUM_COMMENTS + " TEXT NOT NULL, " +
            COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP);";


    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS posts");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public FeedDBAdapter(Context ctx) {
        mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws android.database.SQLException if the database could be neither opened or created
     */
    public FeedDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    /**
     * Create a new post using the given parameters.
     */
    public long createPost(FeedItem post) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(COLUMN_TITLE, post.getTitle());
        initialValues.put(COLUMN_URL, post.getUrl());
        initialValues.put(COLUMN_SUBREDDIT, post.getSubReddit());
        initialValues.put(COLUMN_DOMAIN, post.getDomain());
        initialValues.put(COLUMN_REDDIT_ID, post.getId());
        initialValues.put(COLUMN_AUTHOR, post.getAuthor());
        initialValues.put(COLUMN_SCORE, post.getScore());
        initialValues.put(COLUMN_THUMBNAIL, post.getThumbnail());
        initialValues.put(COLUMN_PERMALINK, post.getPermalink());
        initialValues.put(COLUMN_CREATED, post.getCreated());
        initialValues.put(COLUMN_NUM_COMMENTS, post.getNumComments());

        return mDb.insert(POSTS_TABLE_NAME, null, initialValues);
    }

    public void createPosts(ArrayList<FeedItem> posts) {
        for (FeedItem post : posts) {
            createPost(post);
        }
    }

    /**
     * Delete the post with the given rowId
     */
    public boolean deletePost(long rowId) {
        return mDb.delete(POSTS_TABLE_NAME, _ID + " = " + rowId, null) > 0;
    }

    /**
     * Delete all posts
     *
     * @return true if deleted, false otherwise
     */
    public boolean deleteAllPosts() {
        return mDb.delete(POSTS_TABLE_NAME, null, null) > 0;
    }

    public boolean deleteAllSubredditPosts(String subReddit) {
        return mDb.delete(POSTS_TABLE_NAME, COLUMN_SUBREDDIT + " = '" + subReddit + "';", null) > 0;
    }

    /**
     * Return a Cursor over the list of all posts in the database
     *
     * @return Cursor over all posts
     */
    public Cursor fetchAllPosts() {
        return mDb.query(POSTS_TABLE_NAME, new String[] {
                        _ID, COLUMN_TITLE, COLUMN_URL, COLUMN_SUBREDDIT},
                null, null, null, null, null, null);
    }

    public Cursor fetchSubRedditPosts(String subReddit) {
        return mDb.query(POSTS_TABLE_NAME, new String[] {
                        _ID, COLUMN_TITLE, COLUMN_URL, COLUMN_SUBREDDIT,
                        COLUMN_DOMAIN, COLUMN_REDDIT_ID, COLUMN_AUTHOR,
                        COLUMN_SCORE, COLUMN_THUMBNAIL, COLUMN_PERMALINK,
                        COLUMN_CREATED, COLUMN_NUM_COMMENTS},
                COLUMN_SUBREDDIT + " = '" + subReddit + "';",
                null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     *
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchPost(long rowId) throws SQLException {

        Cursor mCursor =
                mDb.query(true, POSTS_TABLE_NAME, new String[] {
                                _ID, COLUMN_TITLE, COLUMN_URL},
                        _ID + "=" + rowId,
                        null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

}
