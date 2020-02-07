package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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
import com.rokudoz.irecipe.Utils.Adapters.MyIngredients.MyIngredientsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class profileMyIngredientsFragment extends Fragment {
    private static final String TAG = "profileMyIngredientsFra";

    private View view;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    private String userDocId;
    private List<Ingredient> userIngredientList;
    private List<Ingredient> allIngredientsList = new ArrayList<>();

    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;

    private RecyclerView ingredientsRecyclerView;
    private MyIngredientsAdapter ingredientsAdapter;
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

        fab = view.findViewById(R.id.profileFragmentMyIngredients_addIngredientBtn);
        ingredientsRecyclerView = view.findViewById(R.id.profileMyIngredientsFragment_recycler_view);
        progressBar = view.findViewById(R.id.profileMyIngredientsFragment_pbLoading);
        userIngredientList = new ArrayList<>();

        ingredientsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    // Scroll Down
                    if (fab.isShown()) {
                        fab.hide();
                    }
                } else if (dy < 0) {
                    // Scroll Up
                    if (!fab.isShown()) {
                        fab.show();
                    }
                }
            }
        });


        buildRecyclerView();
        getUserInfo();

        Log.d(TAG, "onCreateView: ");
        return view;
    }

    private void buildRecyclerView() {
        ingredientsRecyclerView.setHasFixedSize(true);
        ingredientsLayoutManager = new LinearLayoutManager(getContext());
        ingredientsAdapter = new MyIngredientsAdapter(getActivity(), userIngredientList);
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
                            ingredientsReference.document("ingredient_categories").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if (e == null && documentSnapshot != null) {
                                        ingredient_categories = (List<String>) documentSnapshot.get("categories");
                                        Log.d(TAG, "onEvent: " + ingredient_categories);
                                        categories = ingredient_categories.toArray(new String[0]);
                                        getUserIngredientList();
                                    }
                                }
                            });

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
                            List<Ingredient> ingredientList = new ArrayList<>();

                            for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(queryDocumentSnapshots)) {
                                if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                                    Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                                    ingredient.setDocumentId(queryDocumentSnapshot.getId());
                                    if (!allIngredientsList.contains(ingredient)) {
                                        allIngredientsList.add(ingredient);
                                    } else if (allIngredientsList.contains(ingredient)) {
                                        allIngredientsList.set(allIngredientsList.indexOf(ingredient), ingredient);
                                    }

                                    Collections.sort(allIngredientsList);
                                }
                            }

                            for (Ingredient ingredientt : allIngredientsList) {
                                if (!ingredientList.contains(ingredientt) && ingredientt.getOwned())
                                    ingredientList.add(ingredientt);
                            }
                            Collections.sort(ingredientList);

                            final List<String> categories = new ArrayList<>();
                            for (Ingredient ing : ingredientList) {
                                if (!categories.contains(ing.getCategory()))
                                    categories.add(ing.getCategory());
                            }
                            for (String categoryName : categories) {
                                Ingredient ingg = new Ingredient();
                                ingg.setName(categoryName);
                                ingg.setCategory_name(categoryName);
                                if (!userIngredientList.contains(ingg))
                                    userIngredientList.add(ingg);
                                for (Ingredient inggg : ingredientList) {
                                    if (inggg.getCategory().equals(categoryName) && !userIngredientList.contains(inggg))
                                        userIngredientList.add(inggg);
                                }

                            }
                            ingredientsAdapter.notifyDataSetChanged();


                            //Button to add ingredient manually
                            fab.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    final LinearLayout linearLayout = new LinearLayout(getActivity());
                                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                                    final EditText input = new EditText(getActivity());
                                    final Spinner spinner = new Spinner(getActivity());
                                    //create an adapter to describe how the items are displayed, adapters are used in several places in android.
                                    //There are multiple variations of this, but this is the basic variant.
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, categories);
                                    //set the spinners adapter to the previously created one.
                                    spinner.setAdapter(adapter);
                                    input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                                    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                                    linearLayout.addView(input);
                                    linearLayout.addView(spinner);
                                    materialAlertDialogBuilder.setView(linearLayout);
                                    materialAlertDialogBuilder.setMessage("Add ingredient to list");
                                    materialAlertDialogBuilder.setCancelable(true);
                                    materialAlertDialogBuilder.setPositiveButton(
                                            "Confirm",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    //
                                                    Ingredient ingredient = new Ingredient(input.getText().toString(), spinner.getSelectedItem().toString(), 0f, "g", true);

                                                    if (input.getText().toString().trim().equals("")) {
                                                        Toast.makeText(getActivity(), "You need to write the name of what you want to add to the list", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        if (userIngredientList.contains(ingredient)) {
                                                            Toast.makeText(getActivity(), "" + input.getText().toString() + " is already in your list", Toast.LENGTH_SHORT).show();
                                                        } else if (allIngredientsList.contains(ingredient) && !allIngredientsList.get(allIngredientsList.indexOf(ingredient)).getOwned()) {
                                                            ingredient.setDocumentId(allIngredientsList.get(allIngredientsList.indexOf(ingredient)).getDocumentId());
                                                            usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).collection("Ingredients")
                                                                    .document(ingredient.getDocumentId()).set(ingredient);
                                                        } else {
                                                            usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).collection("Ingredients")
                                                                    .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                @Override
                                                                public void onSuccess(DocumentReference documentReference) {
                                                                    Log.d(TAG, "onSuccess: added to db");
                                                                }
                                                            });

                                                        }
                                                        dialog.cancel();
                                                    }

                                                }
                                            });

                                    materialAlertDialogBuilder.setNegativeButton(
                                            "Cancel",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });

                                    materialAlertDialogBuilder.show();

                                }
                            });

                        }
                    }
                });
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof MyIngredientsAdapter.CategoryViewHolder)
                return 0;
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
            final Ingredient ingredient = userIngredientList.get(viewHolder.getAdapterPosition());
            final int position = viewHolder.getAdapterPosition();

            Log.d(TAG, "onSwiped: " + ingredient.getName() + position + "");

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
