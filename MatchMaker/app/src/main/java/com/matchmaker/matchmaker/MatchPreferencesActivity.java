package com.matchmaker.matchmaker;
/**************************************************************************************************
 Match Preferences Activity
 Authors: Pamela Kelly, Andrew Cameron
 Course: COMP 41690 Android Programming
 Usage: An Activitiy for enabling the user to set their preferences for searching for events.
 **************************************************************************************************/

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Map;

public class MatchPreferencesActivity extends AppCompatActivity {
    // variables for use in the time and date pickers
    private int mYear, mMonth, mDay, mHour, mMinute;

    // variables for representing the user's preference choices
    private String userTimeChoice;
    private String userDateChoice;
    private String userActivityChoice;
    StringBuilder matchResultsTemp = new StringBuilder();

    // Array to store results from query
    String[] matchResults = new String[5];
    int count = 0; // used later to index array

    // Other variables needed
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_preferences);
        // Get the Intent that start this activity and extract the string
        // Capture the layout's TextView and set the string as its text
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(R.string.select_preferences_message);


        //######################################SPINNER########################################
        // Setting up the Spinner for sport choice
        Spinner spinner = (Spinner) findViewById(R.id.sports_spinner);

        //Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(this, R.array.sports_list_values,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                userActivityChoice = parent.getItemAtPosition(pos).toString();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    //##################################### Time Picker Set Up #################################
    // reference: https://www.journaldev.com/9976/android-date-time-picker-dialog
    public void showTimePickerDialog(View v) {
        // function for displaying the time picker
        final EditText editTextTime = (EditText) findViewById(R.id.time);
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editTextTime.setText(hourOfDay + ":" + minute);
                        userTimeChoice = hourOfDay + ":" + minute;
                    }
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    //##################################### Date Picker Set Up ##################################
    // reference: https://www.journaldev.com/9976/android-date-time-picker-dialog
    public void showDatePickerDialog(View v) {
        final EditText editTextDate = (EditText) findViewById(R.id.date);
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                editTextDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                userDateChoice = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;
            }
        }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    public void sendPreferences(View view) {
        // Ideally there would be an option here for users to search either by sport
        // or by date/time - at the moment the Sport one is the only one enabled.
        getMatchesBySport();
    }

    public void getMatchesByDate() {
        //this is called by inputs to search by date ONLY

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //filter by date range: these variables added to showDatePickerDialog as query range
        final Calendar c = Calendar.getInstance();
        int mDate = c.get(Calendar.DATE);
        int mTime = mHour; // could be more precise with more time to fully implement
        final int startDate = mDate - 2;
        final int finishDate = mDate + 2;
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events").child(userActivityChoice.toLowerCase());

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent intent = new Intent(getBaseContext(), SearchResults.class);
                intent.putExtra("Activity", userActivityChoice);
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Event event = singleSnapshot.getValue(Event.class);
                    int numDate = Integer.parseInt(event.date.split("-")[0]);
                    if (numDate >= startDate && numDate <= finishDate) {
                        String eventString = event.toString();
                        String eventName = singleSnapshot.getKey();
                        intent.putExtra("Event" + Integer.toString(count), eventString);
                        count += 1;
                        // Doesn't always get to 5!
                        if (count >= 5) {
                            break;
                        }
                    }
                    }
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post Failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
            }
        });;
    };

    public void getMatchesByTime() {
        //this is called by inputs to search by time ONLY
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //filter by time range: these variables added to showTimePickerDialog as query range
        final int startHour = mHour - 5;
        final int finishHour = mHour + 5;
        DatabaseReference eventTimeRef = database.getReference("events").child(userActivityChoice.toLowerCase());

        eventTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent intent = new Intent(getBaseContext(), SearchResults.class);
                intent.putExtra("Activity", userActivityChoice);
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Event event = singleSnapshot.getValue(Event.class);
                    int eventHour = Integer.parseInt(event.time.split(":")[0]);
                    if (eventHour >= startHour && eventHour <= finishHour) {
                        String eventString = event.toString();
                        String eventName = singleSnapshot.getKey();
                        intent.putExtra("Event" + Integer.toString(count), eventString);
                        count += 1;
                        // Doesn't always get to 5!
                        if (count >= 5) {
                            break;
                        }
                    }
                }
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post Failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
            }


        });
    };

    public void getMatchesBySport() {
        //################### Get Data from Firebase ############################

        // Get a firebase instance
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Get a reference for the "table" we want to access using key "events"
        DatabaseReference eventQuery = database.getReference("events").child(userActivityChoice.toLowerCase());
        // Query eventQuery = myRef.child(userActivityChoice.toLowerCase());
        eventQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent intent = new Intent(getBaseContext(), SearchResults.class);
                intent.putExtra("Activity", userActivityChoice);
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Event event = singleSnapshot.getValue(Event.class);
                    String eventString = event.toString();
                    String eventName = singleSnapshot.getKey();
                    intent.putExtra("Event" + Integer.toString(count), eventString);
                    count += 1;
                    // Doesn't always get to 5!
                    if (count >= 5) {
                        break;
                    }
                }
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post Failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
            }


        });

    }
}



