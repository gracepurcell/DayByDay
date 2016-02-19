package com.example.n00132610.mycalendarapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CalendarView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int EDITOR_REQUEST_CODE = 1001;
    private CursorAdapter cursorAdapter;
    private String noteFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /** Telling the application to run your version of your code as well as default for onCreate */
        super.onCreate(savedInstanceState);
        /** specifying which xml layout to use with this particular activity */
        setContentView(R.layout.activity_main);
        setTitle("Day by Day");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        String[] from = {DBOpenHelper.NOTE_TEXT, DBOpenHelper.NOTE_DATE};
        int[] to = {R.id.tvNote};

        /** creating new cursor adapter to use*/
        cursorAdapter = new SimpleCursorAdapter(this,
                R.layout.note_list_item, null, from, to, 0);

        CalendarView cal = (CalendarView) findViewById(R.id.calendar);

        /** creating on click listener for viewing a certain date on the calendar > DateActivity */
        cal.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                Intent intent = new Intent(MainActivity.this, DateActivity.class);
                Calendar date = new GregorianCalendar();
                date.set(year, month, dayOfMonth);
                Date dt = date.getTime();
                long time = dt.getTime();
                intent.putExtra(DateActivity.TIME_KEY, time);

                startActivityForResult(intent, EDITOR_REQUEST_CODE);
            }
        });

        /** creating new view on screen to display what is stored in database */
        ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(cursorAdapter);

        /** creating new on click listener to bring you to view a certain note you have saved in the database. > EditorActivity */
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                intent.putExtra(EditorActivity.KEY_ID, id);
                startActivityForResult(intent, EDITOR_REQUEST_CODE);
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    /** method used to create a new note into the database  */
    private void insertNote(String noteText, String noteDate, String noteTime, String noteLocation) {
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.NOTE_DATE, noteDate);
        values.put(DBOpenHelper.NOTE_TIME, noteTime);
        values.put(DBOpenHelper.NOTE_LOCATION, noteLocation);
        Uri noteUri = getContentResolver().insert(NotesProvider.CONTENT_URI, values);
    }

    /** specifying what menu to use in this particular activity */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** stating what each option in the menu does when clicked */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_create_sample:
                insertSampleData();
                break;
            case R.id.action_delete_all:
                deleteAllNotes();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /** the method to delete everything in the database */
    private void deleteAllNotes() {
        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                        if (button == DialogInterface.BUTTON_POSITIVE) {
                            //Insert Data management code here
                            getContentResolver().delete(
                                    NotesProvider.CONTENT_URI, null, null
                            );
                            restartLoader();

                            Toast.makeText(MainActivity.this,
                                    getString(R.string.all_deleted),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                .show();
    }

    /** this method is purely designed for demonstration purposes
     * in order to show how it works when everything is used correctly.
     * Creates sample data so that you can easily see how it works etc.*/
    private void insertSampleData() {
        insertNote("Walk the dog", "2016-02-12", "15:45", "53.366889980492545,-6.2345464645444");
        insertNote("Go Shopping", "2016-02-12", "12:00", "53.3668766812,-6.23445784");
        insertNote("See Friend", "2016-02-12", "17:00", "53.375729463537284,-6.4456874364646378");
        insertNote("Hairdresser", "2016-02-15", "12:40", "53.3451254,-6.345411551");
        insertNote("Swimming lesson", "2016-02-08", "20:00", "53.37464783648367,-6.4367863433373");
        restartLoader();
    }

    /** restarting loader created*/
    private void restartLoader() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /** creating new loader for cursor  */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, NotesProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }

    /** method to start EditorActivity*/
    public void openEditorForNewNote(View view) {
        Intent intent = new Intent(this, EditorActivity.class);
        startActivityForResult(intent, EDITOR_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDITOR_REQUEST_CODE && resultCode == RESULT_OK) {
            restartLoader();
        }
    }
}
