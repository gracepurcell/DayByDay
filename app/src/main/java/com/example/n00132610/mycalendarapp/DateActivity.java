package com.example.n00132610.mycalendarapp;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EDITOR_REQUEST_CODE = 1001;
    public static final String TIME_KEY = "time";
    private SimpleCursorAdapter cursorAdapter;
    private Bundle cursorArgs;
    // private FloatingActionButton fab;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date);
        Intent i = this.getIntent();
        final long time = i.getLongExtra(TIME_KEY, -1);

        if (time != -1) {
            Date dt = new Date(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            final String dateString = sdf.format(dt);

            setTitle(dateString);

            Toast.makeText(this, "Date selected = " + dateString, Toast.LENGTH_LONG).show();

            String[] from = {DBOpenHelper.NOTE_TEXT};
            int[] to = {R.id.tvNote};

            cursorAdapter = new SimpleCursorAdapter(this,
                    R.layout.note_list_item, null, from, to, 0);

            ListView list = (ListView) findViewById(android.R.id.list);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabDate);
            list.setAdapter(cursorAdapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(DateActivity.this, EditorActivity.class);
                    intent.putExtra(EditorActivity.KEY_ID, id);
                    startActivityForResult(intent, EDITOR_REQUEST_CODE);
                }
            });

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DateActivity.this, EditorActivity.class);
                    intent.putExtra(EditorActivity.KEY_TIME, time);
                    startActivityForResult(intent, EDITOR_REQUEST_CODE);
                }
            });

            cursorArgs = new Bundle();
            cursorArgs.putString(TIME_KEY, dateString);

            getLoaderManager().initLoader(0, cursorArgs, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_date, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDITOR_REQUEST_CODE && resultCode == RESULT_OK) {
            restartLoader();
        }
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(0, cursorArgs, this);
    }

    public void openEditorForNewNote(View view) {
        Intent intent = new Intent(this, EditorActivity.class);
        //String str = dateString;
        //intent.putExtra(str);
        startActivityForResult(intent, EDITOR_REQUEST_CODE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String dateString = args.getString(TIME_KEY);
        String selection = DBOpenHelper.NOTE_DATE + " = '" + dateString + "'";
        return new CursorLoader(this, NotesProvider.CONTENT_URI, null, selection, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);

    }
}
