package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class profileMyIngredientsFragment extends Fragment {
    private static final String TAG = "profileMyIngredientsFra";
    private String userDocId;
    private List<Ingredient> userIngredientList;
    private List<Ingredient> allIngredientsList;

    private LinearLayout ingredientsLinearLayout;
    private List<MaterialCheckBox> ingredientCheckBoxList;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");


    public profileMyIngredientsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile_my_ingredients, container, false);

        ingredientsLinearLayout = view.findViewById(R.id.profileMyIngredientsFragment_ingredients_linear_layout);

        userIngredientList = new ArrayList<>();
        allIngredientsList = new ArrayList<>();
        ingredientCheckBoxList = new ArrayList<>();

        getUserInfo();

        return view;
    }


    private void getUserInfo() {
        usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
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
        ingredientsReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
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
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
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
                            for (MaterialCheckBox materialCheckBox : ingredientCheckBoxList) {
                                checkboxNames.add(materialCheckBox.getText().toString());
                            }
                            for (Ingredient ingredient : userIngredientList) {
                                if (!checkboxNames.contains(ingredient.getName()))
                                    addIngredientLayout(ingredient);
                            }
                        }
                    }
                });
    }

    private void addIngredientLayout(final Ingredient ingredient) {
        if (getActivity() != null) {

            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ingredientsLinearLayout.addView(linearLayout);

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
    }

}
