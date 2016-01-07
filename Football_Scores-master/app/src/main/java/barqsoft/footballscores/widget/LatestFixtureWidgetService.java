package barqsoft.footballscores.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;

public class LatestFixtureWidgetService extends IntentService {

    // tag logging with classname
    private static final String LOG_TAG = LatestFixtureWidgetService.class.getSimpleName();

    /**
     * Constructor
     */
    public LatestFixtureWidgetService() {
        super("LatestFixtureWidgetService");
    }

    /**
     * Handle the given intent when starting the service
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // find the active instances of our widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, LatestFixtureWidgetProvider.class));

        // create the date and time of now
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


        // get most recent fixture from the contentprovider
        Uri recentUri = DatabaseContract.scores_table.buildScoreMostRecent();
        Cursor cursor = getContentResolver().query(
                recentUri,
                null,
                null,
                new String[] { simpleDateFormat.format(date)},
                DatabaseContract.scores_table.DATE_COL +" DESC, "+ DatabaseContract.scores_table.TIME_COL +" DESC");

        if (cursor == null) {
            return;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        // loop through all our active widget instances
        for (int appWidgetId : appWidgetIds) {

            // get our layout
            final RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_latest_fixtures);

            // home team logo
            setImageViewBitmapFromUrl(
                    views,
                    R.id.home_crest,
                    cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_LOGO_COL)));

            // home team name
            String homeTeamName = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_COL));
            views.setTextViewText(R.id.home_name, homeTeamName);

            // score
            String score  =Utilities.getScores(
                    cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_GOALS_COL)),
                    cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_GOALS_COL)));
            views.setTextViewText(R.id.score_textview, score);

            // match time
            views.setTextViewText(R.id.data_textview, cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.TIME_COL)));

            // away team logo
            setImageViewBitmapFromUrl(
                    views,
                    R.id.away_crest,
                    cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_LOGO_COL)));

            // away team name
            String awayTeamName = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_COL));
            views.setTextViewText(R.id.away_name, awayTeamName);


            // launch the app onclick on the widget
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_latest_fixtures, pendingIntent);

            // update the widget with the set views
            appWidgetManager.updateAppWidget(appWidgetId, views);

            Log.v(LOG_TAG, homeTeamName);Log.v(LOG_TAG, score);Log.v(LOG_TAG, awayTeamName);
        }
    }

    /**
     * Load an image from a url using glide int a bitmap and set it as the image of a remote imageview
     * @param views RemoteViews
     * @param viewId int
     * @param imageUrl String
     */
    private void setImageViewBitmapFromUrl(RemoteViews views, int viewId, String imageUrl) {

        Bitmap bitmap = null;

        // try to load the image into a bitmap from given url
        try {
            bitmap = Glide.with(LatestFixtureWidgetService.this)
                    .load(imageUrl)
                    .asBitmap()
                    .error(R.drawable.no_icon)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG, "Error retrieving image from url: "+ imageUrl, e);
        }

        // if bitmap loaded update the given imageview
        if (bitmap != null) {
            // scale the bitmap down because of the binder limit
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                bitmap = Utilities.scaleBitmap(getApplicationContext(), bitmap, 150);
            }
            views.setImageViewBitmap(viewId, bitmap);
        }
    }
}