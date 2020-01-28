package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class profileMyIngredientsFragment extends Fragment {
    private static final String TAG = "profileMyIngredientsFra";

    private View view;

    private String userDocId;
    private List<Ingredient> userIngredientList;
    private List<Ingredient> allIngredientsList;

    private LinearLayout ingredientsLinearLayout;
    private List<MaterialCheckBox> ingredientCheckBoxList;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private ListenerRegistration userDetailsListener,userIngredientListListener,allIngredientsListener;


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


        ingredientsLinearLayout = view.findViewById(R.id.profileMyIngredientsFragment_ingredients_linear_layout);

        userIngredientList = new ArrayList<>();
        allIngredientsList = new ArrayList<>();
        ingredientCheckBoxList = new ArrayList<>();

        getUserInfo();

        Log.d(TAG, "onCreateView: ");
        return view;
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
        if (allIngredientsListener != null) {
            allIngredientsListener.remove();
            allIngredientsListener = null;
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
                            getAllIngredientsList();
                        }
                    }
                });
    }

    private void getAllIngredientsList() {
        allIngredientsListener = ingredientsReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e == null) {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                            Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                            ingredient.setDocumentId(queryDocumentSnapshot.getId());
                            if (!allIngredientsList.contains(ingredient))
                                allIngredientsList.add(ingredient);
                        }
                    }
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
                            for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(queryDocumentSnapshots)) {
                                if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                                    Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                                    ingredient.setDocumentId(queryDocumentSnapshot.getId());
                                    if (!userIngredientList.contains(ingredient))
                                        userIngredientList.add(ingredient);
                                }
                            }
                            for (Ingredient ing : allIngredientsList) {
                                if (!userIngredientList.contains(ing)) {
                                    userIngredientList.add(ing);
                                }
                            }

                            List<String> checkboxNames = new ArrayList<>();
                            List<String> ingredientCategoryList = new ArrayList<>();

                            //setup ingredient names so that they don't duplicate later
                            for (MaterialCheckBox materialCheckBox : ingredientCheckBoxList) {
                                checkboxNames.add(materialCheckBox.getText().toString());
                            }
                            //Setup categories
                            for (Ingredient ingredient : userIngredientList) {
                                if (!ingredientCategoryList.contains(ingredient.getCategory())) {
                                    ingredientCategoryList.add(ingredient.getCategory());
                                }
                            }
                            Collections.sort(ingredientCategoryList);
                            for (String category : ingredientCategoryList) {
                                List<Ingredient> ingredientsByCategoryList = new ArrayList<>();
                                for (Ingredient ingredient : userIngredientList) {
                                    if (ingredient.getCategory().equals(category) && !checkboxNames.contains(ingredient.getName()))
                                        ingredientsByCategoryList.add(ingredient);
                                }
                                addCategoryOfIngredientsLayout(ingredientsByCategoryList, category);
                            }

                        }
                    }
                });
    }

    private void addCategoryOfIngredientsLayout(List<Ingredient> ingredientList, String categoryName) {
        if (getActivity() != null && !ingredientList.isEmpty()) {
            Log.d(TAG, "addCategoryOfIngredientsLayout: ");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 4, 0, 4);

            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(params);

            TextView categoryNameTextView = new TextView(getActivity());
            categoryNameTextView.setText(categoryName);
            categoryNameTextView.setTextSize(12);
//            categoryNameTextView.setTypeface(Typeface.DEFAULT_BOLD);
            categoryNameTextView.setTextAppearance(R.style.TextAppearance_MaterialComponents_Headline6);
            categoryNameTextView.setPadding(convertDpToPixel(8), 4, 0, 0);
            linearLayout.addView(categoryNameTextView);

            for (Ingredient ingredient : ingredientList) {
                addIngredientLayout(ingredient, linearLayout);
            }
            ingredientsLinearLayout.addView(linearLayout);
        }
    }

    private void addIngredientLayout(final Ingredient ingredient, LinearLayout linearLayout) {

        MaterialCheckBox materialCheckBox = new MaterialCheckBox(getActivity());
        materialCheckBox.setText(ingredient.getName());
        materialCheckBox.setChecked(ingredient.getOwned());
        materialCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userIngredientList.get(userIngredientList.indexOf(ingredient)).setOwned(isChecked);
                usersReference.document(userDocId).collection("Ingredients").document(ingredient.getDocumentId()).set(ingredient);
            }
        });
        linearLayout.addView(materialCheckBox);

        ingredientCheckBoxList.add(materialCheckBox);

    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

}
