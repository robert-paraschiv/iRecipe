package com.rokudoz.irecipe.Fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShoppingListFragment extends Fragment {

    private static final String TAG = "ShoppingListFragment";
    private ProgressBar pbLoading;
    private LinearLayout ingredientsCheckBoxLinearLayout;

    private String userDocumentID = "";
    private List<Ingredient> shoppingListIngredients = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private Button mEmptyBasketBtn;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");


    public static ShoppingListFragment newInstance() {
        ShoppingListFragment fragment = new ShoppingListFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);
//        textViewData = view.findViewById(R.id.tv_data);
        mEmptyBasketBtn = view.findViewById(R.id.empty_basket_btn);
        ingredientsCheckBoxLinearLayout = view.findViewById(R.id.ingredients_checkBox_linearLayout);


        getUserIngredients();

        return view;
    }

    private void getUserIngredients() {
        userDocumentID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersReference.document(userDocumentID).collection("Ingredients").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }

                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                    ingredient.setDocumentId(documentSnapshot.getId());
                    if (!userIngredientList.contains(ingredient)) {
                        userIngredientList.add(ingredient);
                    }
                }

                getUserShoppingList();
            }
        });
    }

    private void getUserShoppingList() {
        userDocumentID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersReference.document(userDocumentID).collection("ShoppingList").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }

                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                    ingredient.setDocumentId(documentSnapshot.getId());
                    if (userIngredientList.contains(ingredient) && userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                        ingredient.setOwned(true);
                    }
                    if (!shoppingListIngredients.contains(ingredient)) {
                        shoppingListIngredients.add(ingredient);
                        addIngredientCheckBox(ingredient);
                    }
                }

                if (shoppingListIngredients.isEmpty()){
                    mEmptyBasketBtn.setVisibility(View.INVISIBLE);
                }
                // EMPTY basket on click
                mEmptyBasketBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (final Ingredient ingToDelete : shoppingListIngredients) {
                            usersReference.document(userDocumentID).collection("ShoppingList").document(ingToDelete.getDocumentId())
                                    .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: Deleted" + ingToDelete.toString() + " from shopping list");
                                    shoppingListIngredients.remove(ingToDelete);
                                }
                            });
                        }
                        ingredientsCheckBoxLinearLayout.removeAllViews();
                        mEmptyBasketBtn.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }


    private void addIngredientCheckBox(final Ingredient ingredient) {
        if (getActivity() != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 4, 0, 4);

            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(params);

            MaterialCheckBox materialCheckBox = new MaterialCheckBox(getActivity());
            materialCheckBox.setText(ingredient.getName());
            materialCheckBox.setChecked(ingredient.getOwned());

            materialCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    shoppingListIngredients.get(shoppingListIngredients.indexOf(ingredient)).setOwned(isChecked);
                    usersReference.document(userDocumentID).collection("ShoppingList").document(ingredient.getDocumentId()).set(ingredient);
                    if (isChecked) {
                        if ( userIngredientList.contains(ingredient)){
                            if (!userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()){
                                usersReference.document(userDocumentID).collection("Ingredients")
                                        .document(userIngredientList.get(userIngredientList.indexOf(ingredient)).getDocumentId()).set(ingredient);
                            }
                        }else {
                            usersReference.document(userDocumentID).collection("Ingredients")
                                    .document(ingredient.getDocumentId()).set(ingredient);
                        }
                    }
                }
            });
            linearLayout.addView(materialCheckBox);
            ingredientsCheckBoxLinearLayout.addView(linearLayout);
        }
    }
}
