package com.example.n00132610.mycalendarapp;



import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by Grace on 16/01/2016.
 */
public class  TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        /**Use the current time as the default values for the time picker*/
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        /**Create and return a new instance of TimePickerDialog */
        return new TimePickerDialog(getActivity(),this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        TextView editTime = (TextView) getActivity().findViewById(R.id.editTime);
        /** sets the field to the following in text*/
        editTime.setText("");
        editTime.setText(editTime.getText() + String.valueOf(hourOfDay)
                + ":" + String.valueOf(minute));
    }
}
