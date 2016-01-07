package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class scoresAdapter extends CursorAdapter
{

    public double detail_match_id = 0;
    public scoresAdapter(Context context,Cursor cursor,int flags)
    {
        super(context,cursor,flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);
        ViewHolder mHolder = new ViewHolder(mItem);
        mItem.setTag(mHolder);
        //Log.v(FetchScoreTask.LOG_TAG,"new View inflated");
        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor)
    {
        final ViewHolder mHolder = (ViewHolder) view.getTag();
        String homeName = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_COL));
        String awayName = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_COL));
        mHolder.home_name.setText(homeName);
        // home club logo
        Glide.with(context.getApplicationContext()).load(
                cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_LOGO_COL)))
                .error(R.drawable.no_icon)
                .into(mHolder.home_crest);
        mHolder.home_crest.setContentDescription(homeName);

        mHolder.away_name.setText(awayName);
        // away club logo
        Glide.with(context.getApplicationContext()).load(
                cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_LOGO_COL)))
                .error(R.drawable.no_icon)
                .into(mHolder.away_crest);
        mHolder.away_crest.setContentDescription(awayName);
        mHolder.date.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.TIME_COL)));
        String homeGoals =  cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_GOALS_COL));
        String awayGoals =  cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_GOALS_COL));
        mHolder.score.setText(Utilities.getScores(homeGoals, awayGoals));
        mHolder.match_id = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.scores_table.MATCH_ID));

        //Log.v(FetchScoreTask.LOG_TAG,mHolder.home_name.getText() + " Vs. " + mHolder.away_name.getText() +" id " + String.valueOf(mHolder.match_id));
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(detail_match_id));
        LayoutInflater vi = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.detail_fragment, null);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.details_fragment_container);
        if(mHolder.match_id == detail_match_id)
        {
            //Log.v(FetchScoreTask.LOG_TAG,"will insert extraView");

            container.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));
            TextView match_day = (TextView) v.findViewById(R.id.matchday_textview);
            int matchDay =  cursor.getInt(cursor.getColumnIndex(DatabaseContract.scores_table.MATCH_DAY));
            int leagueInt =  cursor.getInt(cursor.getColumnIndex(DatabaseContract.scores_table.LEAGUE_COL));
            match_day.setText(Utilities.getMatchDay(context, matchDay,
                    leagueInt));
            TextView league = (TextView) v.findViewById(R.id.league_textview);
            league.setText(Utilities.getLeague(context, leagueInt));
            Button share_button = (Button) v.findViewById(R.id.share_button);
            share_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    //add Share Action
                    context.startActivity(createShareForecastIntent(mHolder.home_name.getText()+" "
                    +mHolder.score.getText()+" "+mHolder.away_name.getText() + " "
                            + context.getString(R.string.share_hashtag) ));
                }
            });
        }
        else
        {
            container.removeAllViews();
        }

    }
    public Intent createShareForecastIntent(String ShareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ShareText);
        return shareIntent;
    }

}
