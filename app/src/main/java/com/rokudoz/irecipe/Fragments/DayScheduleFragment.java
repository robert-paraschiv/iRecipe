package com.rokudoz.irecipe.Fragments;

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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.ScheduledMeal;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ScheduledMealAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class DayScheduleFragment extends Fragment implements ScheduledMealAdapter.OnItemClickListener {
    private static final String TAG = "DayScheduleFragment";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipeRef = db.collection("Recipes");
    private CollectionReference usersRef = db.collection("Users");

    //RecyclerView
    private RecyclerView recyclerView;
    private ScheduledMealAdapter scheduledMealAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private List<ScheduledMeal> scheduleEventList = new ArrayList<>();
    private String dateString = "";
    private View view;
    private MaterialToolbar materialToolbar;

    public DayScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_day_schedule, container, false);

        recyclerView = view.findViewById(R.id.dayScheduleFragment_recyclerView);
        materialToolbar = view.findViewById(R.id.dayScheduleFragment_toolbar);

        if (getArguments() != null) {
            DayScheduleFragmentArgs dayScheduleFragmentArgs = DayScheduleFragmentArgs.fromBundle(getArguments());
            dateString = dayScheduleFragmentArgs.getDateString();
            materialToolbar.setTitle(dateString);
        }

        buildRecyclerView();

        getUserSchedule();
        ///
        return view;
    }

    private void getUserSchedule() {
        usersRef.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ScheduleEvents").whereEqualTo("dateString", dateString).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() > 0) {
                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                final ScheduledMeal scheduleEvent = documentSnapshot.toObject(ScheduledMeal.class);
                                if (scheduleEvent != null) {
                                    scheduleEvent.setDocumentID(documentSnapshot.getId());
                                    recipeRef.document(scheduleEvent.getRecipeID()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                            if (e == null && documentSnapshot != null) {
                                                Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                                if (recipe != null) {
                                                    scheduleEvent.setMealTitle(recipe.getTitle());
                                                    scheduleEvent.setMealPicture(recipe.getImageUrls_list().get(0));
                                                    if (!scheduleEventList.contains(scheduleEvent)) {
                                                        scheduleEventList.add(scheduleEvent);
                                                        scheduledMealAdapter.notifyItemInserted(scheduleEventList.size());
                                                    } else {
                                                        scheduleEventList.set(scheduleEventList.indexOf(scheduleEvent), scheduleEvent);
                                                        scheduledMealAdapter.notifyItemChanged(scheduleEventList.indexOf(scheduleEvent));
                                                    }
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }
                        Log.d(TAG, "onSuccess: " + queryDocumentSnapshots.size());
                    }
                });
    }

    private void buildRecyclerView() {
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        scheduledMealAdapter = new ScheduledMealAdapter(scheduleEventList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(scheduledMealAdapter);
        scheduledMealAdapter.setOnItemClickListener(DayScheduleFragment.this);
    }

    @Override
    public void onItemClick(int position) {
        if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.dayScheduleFragment)
            Navigation.findNavController(view).navigate(DayScheduleFragmentDirections
                    .actionDayScheduleFragmentToRecipeDetailedFragment(scheduleEventList.get(position).getRecipeID()));
    }
}
