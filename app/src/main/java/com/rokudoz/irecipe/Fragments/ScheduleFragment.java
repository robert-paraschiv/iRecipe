package com.rokudoz.irecipe.Fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.ScheduledMeal;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ScheduledMealAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleFragment extends Fragment implements ScheduledMealAdapter.OnItemClickListener {
    private static final String TAG = "ScheduleFragment";

    private CompactCalendarView calendarView;
    private View view;
    private TextView currentDayNREvents, monthTextView;

    private final List<ScheduledMeal> scheduleEventList = new ArrayList<>();
    private final List<ScheduledMeal> todayScheduleList = new ArrayList<>();

    //FireBase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private User mUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesRef = db.collection("Recipes");

    //RecyclerView
    private RecyclerView recyclerView;
    private ScheduledMealAdapter scheduledMealAdapter;

    public ScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_schedule, container, false);

        calendarView = view.findViewById(R.id.scheduleFragment_calendarView);
        monthTextView = view.findViewById(R.id.scheduleFragment_month_textView);
        currentDayNREvents = view.findViewById(R.id.scheduleFragment_nrEventsTV);
        recyclerView = view.findViewById(R.id.scheduleFragment_recyclerView);


        final Date currentDate = new Date();

        final DateFormat smallDateFormat = new SimpleDateFormat("MMMM, YYYY", Locale.getDefault());
        final DateFormat dateFormat = new SimpleDateFormat("dd, MMMM, YYYY", Locale.getDefault());
        final String currentDateString = dateFormat.format(currentDate);
        String currentMonthString = smallDateFormat.format(calendarView.getFirstDayOfCurrentMonth());
        monthTextView.setText(currentMonthString);

        buildRecyclerView();

        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null || queryDocumentSnapshots == null) {
                    Log.e(TAG, "onEvent: ", e);
                    return;
                }
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    final ScheduledMeal scheduleEvent = documentSnapshot.toObject(ScheduledMeal.class);
                    if (scheduleEvent != null) {
                        scheduleEvent.setDocumentID(documentSnapshot.getId());
                        if (!scheduleEventList.contains(scheduleEvent))
                            scheduleEventList.add(scheduleEvent);
                        if (currentDateString.equals(scheduleEvent.getDateString())) {
                            recipesRef.document(scheduleEvent.getRecipeID()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if (e == null && documentSnapshot != null) {
                                        Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                        if (recipe != null) {
                                            scheduleEvent.setMealPicture(recipe.getImageUrls_list().get(0));
                                            scheduleEvent.setMealTitle(recipe.getTitle());
                                            if (todayScheduleList.contains(scheduleEvent)) {
                                                Log.d(TAG, "onEvent: SETTING");
                                                todayScheduleList.set(todayScheduleList.indexOf(scheduleEvent), scheduleEvent);
                                                scheduledMealAdapter.notifyItemChanged(todayScheduleList.indexOf(scheduleEvent));
                                            } else {
                                                Log.d(TAG, "onEvent: ADDING");
                                                todayScheduleList.add(scheduleEvent);
                                                scheduledMealAdapter.notifyItemInserted(todayScheduleList.size()-1);
                                            }
                                            if (todayScheduleList.size() > 0) {
                                                if (todayScheduleList.size() == 1) {
                                                    currentDayNREvents.setText("You've scheduled 1 meal for today");
                                                } else {
                                                    currentDayNREvents.setText("You've scheduled " + todayScheduleList.size() + " meals for today");
                                                }

                                            } else {
                                                currentDayNREvents.setText("No meals planned for today");
                                            }
                                        } else
                                            Log.d(TAG, "onEvent: RECIPE NULL");
                                    } else
                                        Log.d(TAG, "onEvent: FIAL");
                                }
                            });
                        }

                    }

                }
                calendarView.removeAllEvents();
                for (ScheduledMeal event : scheduleEventList) {
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

    private void buildRecyclerView() {
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        scheduledMealAdapter = new ScheduledMealAdapter(todayScheduleList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(scheduledMealAdapter);
        scheduledMealAdapter.setOnItemClickListener(ScheduleFragment.this);
    }

    @Override
    public void onItemClick(int position) {
        Navigation.findNavController(view).navigate(ScheduleFragmentDirections.actionScheduleFragmentToRecipeDetailedFragment(todayScheduleList.get(position).getRecipeID()));
    }
}
