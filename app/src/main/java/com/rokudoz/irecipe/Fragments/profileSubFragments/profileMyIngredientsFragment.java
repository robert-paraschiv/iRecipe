package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.MyIngredientAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class profileMyIngredientsFragment extends Fragment {
    private static final String TAG = "profileMyIngredientsFra";

    private View view;
    private ProgressBar progressBar;
    private RelativeLayout relativeLayout;

    private String userDocId;
    private List<Ingredient> userIngredientList;

    private RecyclerView ingredientsRecyclerView;
    private MyIngredientAdapter ingredientsAdapter;
    private RecyclerView.LayoutManager ingredientsLayoutManager;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private ListenerRegistration userDetailsListener, userIngredientListListener, allIngredientsListener;


    public profileMyIngredientsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_profile_my_ingredients, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        relativeLayout = view.findViewById(R.id.profileFragmentMyIngredients_relativelayout);
        ingredientsRecyclerView = view.findViewById(R.id.profileMyIngredientsFragment_recycler_view);
        progressBar = view.findViewById(R.id.profileMyIngredientsFragment_pbLoading);
        userIngredientList = new ArrayList<>();

        buildRecyclerView();
        getUserInfo();

        Log.d(TAG, "onCreateView: ");
        return view;
    }

    private void buildRecyclerView() {
        ingredientsRecyclerView.setHasFixedSize(true);
        ingredientsLayoutManager = new LinearLayoutManager(getContext());
        ingredientsAdapter = new MyIngredientAdapter(userIngredientList);
        ingredientsRecyclerView.setLayoutManager(ingredientsLayoutManager);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(ingredientsRecyclerView);
        ingredientsRecyclerView.setAdapter(ingredientsAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
        if (userIngredientListListener != null) {
            userIngredientListListener.remove();
            userIngredientListListener = null;
        }
    }

    private void getUserInfo() {
        userDetailsListener = usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            User user = Objects.requireNonNull(documentSnapshot).toObject(User.class);
                            userDocId = Objects.requireNonNull(user).getUser_id();
                            getUserIngredientList();
                        }
                    }
                });
    }


    private void getUserIngredientList() {
        userIngredientListListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            progressBar.setVisibility(View.GONE);
                            for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(queryDocumentSnapshots)) {
                                if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                                    Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                                    ingredient.setDocumentId(queryDocumentSnapshot.getId());
                                    if (!userIngredientList.contains(ingredient) && ingredient.getOwned()) {
                                        userIngredientList.add(ingredient);
                                        ingredientsAdapter.notifyDataSetChanged();
                                    } else if (userIngredientList.contains(ingredient)) {
                                        userIngredientList.set(userIngredientList.indexOf(ingredient), ingredient);
                                        ingredientsAdapter.notifyDataSetChanged();
                                    }
                                    Collections.sort(userIngredientList);
                                    ingredientsAdapter.notifyDataSetChanged();
                                }
                            }

                        }
                    }
                });
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
            final Ingredient ingredient = userIngredientList.get(viewHolder.getAdapterPosition());
            final int position = viewHolder.getAdapterPosition();

            usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                    .document(ingredient.getDocumentId()).update("owned", false);
            Snackbar snackbar = Snackbar.make(view.findViewById(R.id.profileFragmentMyIngredients_relativelayout), ingredient.getName() + " removed",
                    Snackbar.LENGTH_SHORT)
                    .setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            userIngredientList.add(position, ingredient);
                            usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                                    .document(ingredient.getDocumentId()).update("owned", true);
                        }
                    });
            snackbar.show();

            userIngredientList.remove(viewHolder.getAdapterPosition());
            ingredientsAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
        }
    };

}
