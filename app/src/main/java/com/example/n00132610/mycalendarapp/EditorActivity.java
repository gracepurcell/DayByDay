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
import android.widget.Toast;

import java.io.Serializable;
import java.util.Date;

import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_DATE;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_TEXT;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_TIME;
import static com.example.n00132610.mycalendarapp.DBOpenHelper.NOTE_LOCATION;

public class EditorActivity extends AppCompatActivity implements Serializable {

    public static final String KEY_ID = "id";
    public static final String KEY_TIME = "time" ;
    public static final String KEY_LOCAT = "location";
    private static final int MAP_REQUEST_CODE = 1;


    private String action;
    private EditText editor;
    private EditText editorDate;
    private EditText editorTime;
    private EditText editorLocation;
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

        editor = (EditText) findViewById(R.id.editText);
        editorDate = (EditText) findViewById(R.id.editDate);
        editorTime = (EditText) findViewById(R.id.editTime);
        editorLocation = (EditText) findViewById(R.id.editLocation);
        dateButton = (ImageButton) findViewById(R.id.imgButtonCal);
        timeButton = (ImageButton) findViewById(R.id.imgButtonClock);
        locationButton = (ImageButton) findViewById(R.id.imgButtonMap);

        //enableEdit = (FloatingActionButton) findViewById(R.id.fabEdit);


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            action = Intent.ACTION_INSERT;
            setTitle(getString(R.string.new_note));
        }
        else {
            long id = extras.getLong(KEY_ID);

            if (id == 0){
                action = Intent.ACTION_INSERT;
                setTitle(getString(R.string.new_note));

                long time = intent.getLongExtra(KEY_TIME, 0);
                if (time != 0) {
                    Date d = new Date(time);
                    String dateString= DateFormat.format("yyyy-MM-dd", d).toString();
                    editorDate.setText(dateString);
                }
            }

            else {
                action = Intent.ACTION_EDIT;
                setTitle(getString(R.string.edit_note));

                Uri uri = Uri.parse(NotesProvider.CONTENT_URI + "/" + id);
                noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();

                Cursor cursor;
                cursor = getContentResolver().query(uri,
                        DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);
                cursor.moveToFirst();
                oldText = cursor.getString(cursor.getColumnIndex(NOTE_TEXT));
                oldDate = cursor.getString(cursor.getColumnIndex(NOTE_DATE));
                oldTime = cursor.getString(cursor.getColumnIndex(NOTE_TIME));
                oldLocation = cursor.getString(cursor.getColumnIndex(NOTE_LOCATION));
                editor.setText(oldText);
                editor.setEnabled(false);
                editorDate.setText(oldDate);
                editorDate.setEnabled(false);
                dateButton.setEnabled(false);
                editorTime.setText(oldTime);
                editorTime.setEnabled(false);
                timeButton.setEnabled(false);
                editorLocation.setText(oldLocation);
                editorLocation.setEnabled(false);
                locationButton.setEnabled(false);
                editor.requestFocus();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (action.equals(Intent.ACTION_EDIT)){
            getMenuInflater().inflate(R.menu.menu_editor, menu);
        }

        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem edit = menu.findItem(R.id.action_edit);
//
//        if (mEditmode) {
//            edit.setIcon(R.drawable.ic_content_save_white_24dp);
//            edit.setTitle(R.string.action_save);
//        }
//        else {
//            edit.setIcon(R.drawable.ic_pencil_white_24dp);
//            edit.setTitle(R.string.action_edit);
//        }
//        super.onPrepareOptionsMenu(menu);
//
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {
            case android.R.id.home:

                finishEditing();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
            case R.id.action_edit:
                enableFields(!mEditmode);
                invalidateOptionsMenu();
                break;
        }

        return true;
    }
    private void enableFields(boolean editMode){
        mEditmode = editMode;
        if(NotesProvider.CONTENT_URI != null) {
            editor.setEnabled(/*true*/editMode);
            editorDate.setEnabled(/*true*/editMode);
            dateButton.setEnabled(/*true*/editMode);
            editorTime.setEnabled(/*true*/editMode);
            timeButton.setEnabled(/*true*/editMode);
            editorLocation.setEnabled(/*true*/editMode);
            locationButton.setEnabled(/*true*/editMode);

            //saveButton.setEnabled(true);
            //saveButton = (FloatingActionButton) findViewById(R.id.fabSave);
        }
    }
    private void deleteNote() {
        getContentResolver().delete(NotesProvider.CONTENT_URI,
                noteFilter,null);
        Toast.makeText(this, R.string.note_deleted,
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void finishEditing(){
        String newText = editor.getText().toString().trim();
        String newDate = editorDate.getText().toString().trim();
        String newTime = editorTime.getText().toString().trim();
        String newLocation = editorLocation.getText().toString().trim();

        switch (action) {
            case Intent.ACTION_INSERT:
                if (newText.length() == 0 && newDate.length() == 0 && newTime.length() == 0){
                    setResult(RESULT_CANCELED);
                } else{
                    insertNote(newText, newDate, newTime, newLocation);
                }
                break;
            case Intent.ACTION_EDIT:
                if (newText.length() == 0 && newDate.length() == 0 && newTime.length() == 0 && newLocation.length() == 0){
                    deleteNote();
                }else if (oldText.equals(newText) && oldDate.equals(newDate) && oldTime.equals(newTime) && oldLocation.equals(newLocation)){
                    setResult(RESULT_CANCELED);
                }else {
                    updateNote(newText, newDate, newTime, newLocation);
                }
        }
        finish();
    }

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

    private void insertNote(String noteText, String noteDate, String noteTime, String noteLocation) {
        ContentValues values = new ContentValues();
        values.put(NOTE_TEXT, noteText);
        values.put(NOTE_DATE, noteDate);
        values.put(NOTE_TIME, noteTime);
        values.put(NOTE_LOCATION, noteLocation);
        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        setResult(RESULT_OK);
    }

    @Override
    public void onBackPressed() {
        finishEditing();
    }

    public void onSaveNote(View view) { finishEditing();}

    public void onButtonClicked(View v){
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void openMapFragment(View v) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivityForResult(intent, MAP_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            String lat = data.getStringExtra(MapActivity.LATITUDE_EXTRA);
            String lng = data.getStringExtra(MapActivity.LONGITUDE_EXTRA);

            editorLocation.setText(lat + ", " + lng);
        }
        else {
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        }
    }
}
