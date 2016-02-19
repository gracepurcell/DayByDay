package com.example.n00132610.mycalendarapp;


import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Date;

import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_DATE;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_TEXT;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_TIME;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_LOCATION;

public class EditorActivity extends AppCompatActivity implements Serializable {

    /** Setting up the variables */
    public static final String KEY_ID = "id";
    public static final String KEY_TIME = "time" ;
    public static final String KEY_LOCAT = "location";
    private static final int MAP_REQUEST_CODE = 1;

    /** creating objects used throughout activity, TextViews, Buttons etc.*/
    private String action;
    private EditText editor;
    private TextView editorDate;
    private TextView editorTime;
    private TextView editorLocation;
    private ImageButton dateButton;
    private ImageButton timeButton;
    private ImageButton locationButton;
    private String noteFilter;
    private String oldText;
    private String oldDate;
    private String oldTime;
    private String oldLocation;
    private boolean mEditmode;
    String value = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        /** specifying which object corresponds with its view in xml doc. */
        editor = (EditText) findViewById(R.id.editText);
        editorDate = (TextView) findViewById(R.id.editDate);
        editorTime = (TextView) findViewById(R.id.editTime);
        editorLocation = (TextView) findViewById(R.id.editLocation);
        dateButton = (ImageButton) findViewById(R.id.imgButtonCal);
        timeButton = (ImageButton) findViewById(R.id.imgButtonClock);
        locationButton = (ImageButton) findViewById(R.id.imgButtonMap);

        /** pulling extra from MainActivity so a note can be edited in this activity  */
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        /** specifying whether the method was opened with data from previous activity */
        if (extras == null) {
            action = Intent.ACTION_INSERT;
            setTitle(getString(R.string.new_note));
        }
        else {
            long id = extras.getLong(KEY_ID);

            if (id == 0){
                action = Intent.ACTION_INSERT;
                setTitle(getString(R.string.new_note));

                /** extra that was created from date activity for making a note on that date chosen */
                long time = intent.getLongExtra(KEY_TIME, 0);
                if (time != 0) {
                    Date d = new Date(time);
                    String dateString= DateFormat.format("yyyy-MM-dd", d).toString();
                    editorDate.setText(dateString);
                }
            }
            /** code that is executed when editing a note */
            else {
                action = Intent.ACTION_EDIT;
                setTitle(getString(R.string.edit_note));

                /** calling the note that was chosen on the listview in mainactivity from the database */
                Uri uri = Uri.parse(NotesProvider.CONTENT_URI + "/" + id);
                noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();

                Cursor cursor;
                cursor = getContentResolver().query(uri,
                        DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);
                cursor.moveToFirst();
                /** storing the text that was originally saved in the database to the following local objects */
                oldText = cursor.getString(cursor.getColumnIndex(NOTE_TEXT));
                oldDate = cursor.getString(cursor.getColumnIndex(NOTE_DATE));
                oldTime = cursor.getString(cursor.getColumnIndex(NOTE_TIME));
                oldLocation = cursor.getString(cursor.getColumnIndex(NOTE_LOCATION));
                /** setting the text in the "editor" field to what is stored in the local object "oldtext" */
                editor.setText(oldText);
                /** Making the field and buttons un-clickable until the edit button is clicked */
                editor.setEnabled(false);
                editorDate.setText(oldDate);
                dateButton.setEnabled(false);
                editorTime.setText(oldTime);
                timeButton.setEnabled(false);
                editorLocation.setText(oldLocation);
                locationButton.setEnabled(false);
                editor.requestFocus();
            }
        }
    }

    /** Displaying which menu is shown on the toolbar depending on whether the activity is creating a new note or updating an existing one*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (action.equals(Intent.ACTION_EDIT)) {
            getMenuInflater().inflate(R.menu.menu_editor, menu);
            MenuItem edit = menu.findItem(R.id.action_edit);

            if (mEditmode) {
                edit.setIcon(R.drawable.ic_content_save_white_24dp);
                edit.setTitle(R.string.action_save);
            } else {
                edit.setIcon(R.drawable.ic_pencil_white_24dp);
                edit.setTitle(R.string.action_edit);
            }
            super.onPrepareOptionsMenu(menu);
        }
        else if(action.equals(Intent.ACTION_INSERT)){
            getMenuInflater().inflate(R.menu.menu_editor_create, menu);
        }
        return true;
    }

    /** determines what method is called with each menu button */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
            case R.id.action_edit:
                enableFields(!mEditmode);
                invalidateOptionsMenu();
                break;
            case R.id.action_save:
                finishEditing();
                break;
        }

        return true;
    }

    /** method to enable the buttons and fields within the activity */
    private void enableFields(boolean editMode){
        mEditmode = editMode;
        if(NotesProvider.CONTENT_URI != null) {
            editor.setEnabled(editMode);
            dateButton.setEnabled(editMode);
            timeButton.setEnabled(editMode);
            locationButton.setEnabled(editMode);
        }
    }

    /** method used to delete note that is being edited */
    private void deleteNote() {
        getContentResolver().delete(NotesProvider.CONTENT_URI,
                noteFilter,null);
        Toast.makeText(this, R.string.note_deleted,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    /** method used to save changes made to each field on note */
    private void finishEditing(){
        /** creating new local variables and storing an object in each */
        String newText = editor.getText().toString().trim();
        String newDate = editorDate.getText().toString().trim();
        String newTime = editorTime.getText().toString().trim();
        String newLocation = editorLocation.getText().toString().trim();

        switch (action) {
            case Intent.ACTION_INSERT:
                /** if each field is left blank, don't insert a new note */
                if (newText.length() == 0 && newDate.length() == 0 && newTime.length() == 0){
                    setResult(RESULT_CANCELED);
                }else{
                    insertNote(newText, newDate, newTime, newLocation);
                }
                break;
            case Intent.ACTION_EDIT:
                /** if each field is left blank (while editing) and saved, delete that note */
                if (newText.length() == 0 && newDate.length() == 0 && newTime.length() == 0 && newLocation.length() == 0){
                    deleteNote();
                }
                /** if the new text that was entered is the same as before, then don't do anything */
                else if (oldText.equals(newText) && oldDate.equals(newDate) && oldTime.equals(newTime) && oldLocation.equals(newLocation)){
                    setResult(RESULT_CANCELED);
                }
                /** if you exit the activity with the fields left enabled then don't save changes that were made */
                else if (mEditmode){
                    setResult(RESULT_CANCELED);
                }
                else {
                    updateNote(newText, newDate, newTime, newLocation);
                }
        }
        finish();
    }

    /** method used to save changes to each field in the note */
    private void updateNote(String noteText, String noteDate, String noteTime, String noteLocation) {
        ContentValues values = new ContentValues();
        values.put(NOTE_TEXT, noteText);
        values.put(NOTE_DATE, noteDate);
        values.put(NOTE_TIME, noteTime);
        values.put(NOTE_LOCATION, noteLocation);
        getContentResolver().update(NotesProvider.CONTENT_URI, values, noteFilter, null);
        Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
    }

    /** method used to create new note */
    private void insertNote(String noteText, String noteDate, String noteTime, String noteLocation) {
        ContentValues values = new ContentValues();
        values.put(NOTE_TEXT, noteText);
        values.put(NOTE_DATE, noteDate);
        values.put(NOTE_TIME, noteTime);
        values.put(NOTE_LOCATION, noteLocation);
        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        setResult(RESULT_OK);
    }

    /** method used to determine what happens when the back button is pressed */
    @Override
    public void onBackPressed() {
        if(action.equals(Intent.ACTION_INSERT)){
            finish();
        }else{
            finishEditing();
        }
    }

    /** method used to fire TimePickerFragment when 'time' button is clicked */
    public void onButtonClicked(View v){
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    /** method used to fire DatPickerFragment when the 'date' button is pressed */
    public void showDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    /** Starts up a new intent to hold the map fragment */
    public void openMapFragment(View v) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivityForResult(intent, MAP_REQUEST_CODE);
    }

    /** From the data being passed back for the result we get back*/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            String lat = data.getStringExtra(MapActivity.LATITUDE_EXTRA);
            String lng = data.getStringExtra(MapActivity.LONGITUDE_EXTRA);

            editorLocation.setText(lat + "," + lng);
        }
        else {
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        }
    }
}
