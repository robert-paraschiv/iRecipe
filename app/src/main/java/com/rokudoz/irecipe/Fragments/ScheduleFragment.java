package com.rokudoz.irecipe.Fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment {
    private static final String TAG = "ScheduleFragment";

    private CompactCalendarView calendarView;
    private View view;

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

//        Date date = Calendar.getInstance().getTime();
//        Event event1 = new Event(Color.BLUE, date.getTime(), "EV1");
//        Event event2 = new Event(Color.GREEN, date.getTime(), "EV2");
//        calendarView.addEvent(event1);
//        calendarView.addEvent(event2);
        final List<ScheduleEvent> scheduleEventList = new ArrayList<>();

        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "onEvent: ", e);
                    return;
                }
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    ScheduleEvent scheduleEvent = documentSnapshot.toObject(ScheduleEvent.class);
                    if (scheduleEvent != null) {
                        scheduleEvent.setDocumentID(documentSnapshot.getId());
                        if (!scheduleEventList.contains(scheduleEvent))
                            scheduleEventList.add(scheduleEvent);
                    }

                }
                calendarView.removeAllEvents();
                for (ScheduleEvent event : scheduleEventList) {
                    Event eventToAdd = new Event(Color.BLUE, event.getDate().getTime(), event.getRecipeID());
                    calendarView.addEvent(eventToAdd);
                }
            }
        });

        calendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                ScheduleEvent newEvent = new ScheduleEvent("SomeID BOSS", dateClicked, "breakfast");
                usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").add(newEvent).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "onSuccess: ADDED");
                    }
                });
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {

            }
        });
        //
        return view;
    }
}
