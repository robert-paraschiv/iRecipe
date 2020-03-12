package com.rokudoz.irecipe.Fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.ScheduleEvent;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment {
    private static final String TAG = "ScheduleFragment";

    private CompactCalendarView calendarView;
    private View view;
    private TextView currentDayNREvents, monthTextView;

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesRef = db.collection("Recipes");

    public ScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_schedule, container, false);

        calendarView = view.findViewById(R.id.scheduleFragment_calendarView);
        monthTextView = view.findViewById(R.id.scheduleFragment_month_textView);
        currentDayNREvents = view.findViewById(R.id.scheduleFragment_nrEventsTV);

        final List<ScheduleEvent> scheduleEventList = new ArrayList<>();
        final List<ScheduleEvent> todayScheduleList = new ArrayList<>();
        final Date currentDate = new Date();

        final DateFormat smallDateFormat = new SimpleDateFormat("MMMM, YYYY", Locale.getDefault());
        final DateFormat dateFormat = new SimpleDateFormat("dd, MMMM, YYYY", Locale.getDefault());
        final String currentDateString = dateFormat.format(currentDate);
        String currentMonthString = smallDateFormat.format(calendarView.getFirstDayOfCurrentMonth());
        monthTextView.setText(currentMonthString);

        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null || queryDocumentSnapshots == null) {
                    Log.e(TAG, "onEvent: ", e);
                    return;
                }
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    ScheduleEvent scheduleEvent = documentSnapshot.toObject(ScheduleEvent.class);
                    if (scheduleEvent != null) {
                        scheduleEvent.setDocumentID(documentSnapshot.getId());
                        if (!scheduleEventList.contains(scheduleEvent))
                            scheduleEventList.add(scheduleEvent);
                        if (!todayScheduleList.contains(scheduleEvent) && currentDateString.equals(dateFormat.format(scheduleEvent.getDate())))
                            todayScheduleList.add(scheduleEvent);
                    }

                }
                calendarView.removeAllEvents();
                for (ScheduleEvent event : scheduleEventList) {
                    switch (event.getMealType()) {
                        case "breakfast": {
                            Event eventToAdd = new Event(Color.GREEN, event.getDate().getTime(), event.getRecipeID());
                            calendarView.addEvent(eventToAdd);
                            break;
                        }
                        case "lunch": {
                            Event eventToAdd = new Event(Color.BLUE, event.getDate().getTime(), event.getRecipeID());
                            calendarView.addEvent(eventToAdd);
                            break;
                        }
                        case "dinner": {
                            Event eventToAdd = new Event(Color.RED, event.getDate().getTime(), event.getRecipeID());
                            calendarView.addEvent(eventToAdd);
                            break;
                        }
                    }
                }
                if (todayScheduleList.size() > 0) {
                    currentDayNREvents.setText("You've scheduled " + todayScheduleList.size() + " meals for today");
                } else {
                    currentDayNREvents.setText("No meals planned for today");
                }

            }
        });


        calendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                Navigation.findNavController(view).navigate(ScheduleFragmentDirections.actionScheduleFragmentToDayScheduleFragment(dateFormat.format(dateClicked)));
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                monthTextView.setText(smallDateFormat.format(firstDayOfNewMonth));
            }
        });

        //
        return view;
    }
}
