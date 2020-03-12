package com.rokudoz.irecipe.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.ScheduleEvent;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class DayScheduleFragment extends Fragment {
    private static final String TAG = "DayScheduleFragment";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");

    private List<ScheduleEvent> scheduleEventList = new ArrayList<>();
    private String dateString = "";
    private View view;
    private TextView textView;

    public DayScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_day_schedule, container, false);

        textView = view.findViewById(R.id.dayScheduleFragment_textview);

        if (getArguments() != null) {
            DayScheduleFragmentArgs dayScheduleFragmentArgs = DayScheduleFragmentArgs.fromBundle(getArguments());
            dateString = dayScheduleFragmentArgs.getDateString();
        }
        usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").whereEqualTo("dateString", dateString).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                ScheduleEvent scheduleEvent = documentSnapshot.toObject(ScheduleEvent.class);
                                scheduleEvent.setDocumentID(documentSnapshot.getId());
                                if (scheduleEvent != null && !scheduleEventList.contains(scheduleEvent)) {
                                    scheduleEventList.add(scheduleEvent);
                                }
                            }
                        }
                        Log.d(TAG, "onSuccess: " + queryDocumentSnapshots.size());
                        StringBuilder eventsText = new StringBuilder();
                        for (ScheduleEvent event : scheduleEventList) {
                            eventsText.append(event.toString()).append("\n\n");
                        }
                        textView.setText(eventsText);
                    }
                });

        ///
        return view;
    }
}
