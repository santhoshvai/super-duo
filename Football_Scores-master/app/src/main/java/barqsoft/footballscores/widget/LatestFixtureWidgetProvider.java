package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import barqsoft.footballscores.ScoresProvider;
import barqsoft.footballscores.Utilities;
import barqsoft.footballscores.service.myFetchService;

/**
 * Created by santhoshvai on 06/01/16.
 */
public class LatestFixtureWidgetProvider extends AppWidgetProvider {

    /**
     * Based on the updatePeriodMillis config we will request updated data on set intervals
     * @param context Context
     * @param appWidgetManager AppWidgetManager
     * @param appWidgetIds int[]
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // start the football data service to request the updated fixtures from the api (this will
        //  cause the scores provider to send a broadcast when it finished updating the database)
        // check if we have a network connection
        if (Utilities.isNetworkAvailable(context)) {
            Intent service_start = new Intent(context, myFetchService.class);
            context.startService(service_start);
        }
        // just to be sure lets also load the data from the database into the widget
        context.startService(new Intent(context, LatestFixtureWidgetService.class));
    }


    /**
     * When the ScoresProvider has updated that database with fixtures it will send a broadcast that
     *  this widget will receive and handle here to update the widget
     * @param context Context
     * @param intent Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // refresh the widget by starting the widgetservice that will reload the data from the database
        if (ScoresProvider.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, LatestFixtureWidgetService.class));
        }
    }
}
