package com.example.n00132610.mycalendarapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Grace on 17/01/2016.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public Calendar date;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /** Use the current date as the default date in the picker */
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        /** Creates a new instance of DatePickerDialog and return it */
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        /** stores instance selected from dialog and stores in 'date' */
        date = Calendar.getInstance();
        date.set(year, month, dayOfMonth);

        TextView editDate = (TextView) getActivity().findViewById(R.id.editDate);
        /** formats the date in a certain way so that it can store into the database */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(date.getTime());
        editDate.setText(dateStr);

    }
}
