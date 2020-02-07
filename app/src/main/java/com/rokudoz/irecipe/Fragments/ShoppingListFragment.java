package com.rokudoz.irecipe.Fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShoppingListFragment extends Fragment {

    private static final String TAG = "ShoppingListFragment";
    private FloatingActionButton addIngredientToListFab;
    private LinearLayout ingredientsCheckBoxLinearLayout;

    private String userDocumentID = "";
    private List<Ingredient> shoppingListIngredients = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private List<MaterialCheckBox> ingredientCheckBoxList = new ArrayList<>();
    private Button mEmptyBasketBtn;
    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private ListenerRegistration userIngredientsListener, userShoppingListListener;


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
        addIngredientToListFab = view.findViewById(R.id.fab_add_ingredient_toShoppingList);
        getUserIngredients();
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (userIngredientsListener != null) {
            userIngredientsListener.remove();
            userIngredientsListener = null;
        }
        if (userShoppingListListener != null) {
            userShoppingListListener.remove();
            userShoppingListListener = null;
        }
    }

    private void getUserIngredients() {
        userDocumentID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userIngredientsListener = usersReference.document(userDocumentID).collection("Ingredients").addSnapshotListener(new EventListener<QuerySnapshot>() {
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
        userShoppingListListener = usersReference.document(userDocumentID).collection("ShoppingList").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                ingredientsReference.document("ingredient_categories").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e == null && documentSnapshot != null) {
                            ingredient_categories = (List<String>) documentSnapshot.get("categories");
                            Log.d(TAG, "onEvent: " + ingredient_categories);
                            categories = ingredient_categories.toArray(new String[0]);
                        }
                    }
                });

                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                    ingredient.setDocumentId(documentSnapshot.getId());
                    if (userIngredientList.contains(ingredient) && userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                        ingredient.setOwned(true);
                    }
                    if (!shoppingListIngredients.contains(ingredient)) {
                        shoppingListIngredients.add(ingredient);
                    }
                }

                List<String> checkboxNames = new ArrayList<>();
                List<String> ingredientCategoryList = new ArrayList<>();

                //setup ingredient names so that they don't duplicate later
                for (MaterialCheckBox materialCheckBox : ingredientCheckBoxList) {
                    checkboxNames.add(materialCheckBox.getText().toString());
                }
                //Setup categories
                for (Ingredient ing : shoppingListIngredients) {
                    if (!ingredientCategoryList.contains(ing.getCategory())) {
                        ingredientCategoryList.add(ing.getCategory());
                    }
                }
                Collections.sort(ingredientCategoryList);
                for (String category : ingredientCategoryList) {
                    List<Ingredient> ingredientsByCategoryList = new ArrayList<>();
                    for (Ingredient ing : shoppingListIngredients) {
                        if (ing.getCategory().equals(category) && !checkboxNames.contains(ing.getName()))
                            ingredientsByCategoryList.add(ing);
                    }
                    addCategoryOfIngredientsLayout(ingredientsByCategoryList, category);
                }

                if (shoppingListIngredients.isEmpty()) {
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
                addIngredientToListFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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
                        materialAlertDialogBuilder.setMessage("Add ingredient to shopping list");
                        materialAlertDialogBuilder.setCancelable(true);
                        materialAlertDialogBuilder.setPositiveButton(
                                "Confirm",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //
                                        Ingredient ingredient = new Ingredient(input.getText().toString(), spinner.getSelectedItem().toString(), 0f, "g", false);

                                        if (input.getText().toString().trim().equals("")) {
                                            Toast.makeText(getActivity(), "You need to write the name of what you want to add to the shopping list", Toast.LENGTH_SHORT).show();
                                        } else {
                                            if (shoppingListIngredients.contains(ingredient)) {
                                                Toast.makeText(getActivity(), "" + input.getText().toString() + " is already in your shopping list", Toast.LENGTH_SHORT).show();
                                            } else {
                                                usersReference.document(userDocumentID).collection("ShoppingList").add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
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
                addIngredientCheckBox(ingredient, linearLayout);
            }
            ingredientsCheckBoxLinearLayout.addView(linearLayout);
        }
    }


    private void addIngredientCheckBox(final Ingredient ingredient, LinearLayout linearLayout) {
        if (getActivity() != null) {

            final MaterialCheckBox materialCheckBox = new MaterialCheckBox(getActivity());
            materialCheckBox.setText(ingredient.getName());
            materialCheckBox.setChecked(ingredient.getOwned());
            if (ingredient.getOwned()) {
                materialCheckBox.setPaintFlags(materialCheckBox.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            materialCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    shoppingListIngredients.get(shoppingListIngredients.indexOf(ingredient)).setOwned(isChecked);
                    usersReference.document(userDocumentID).collection("ShoppingList").document(ingredient.getDocumentId()).set(ingredient);
                    if (isChecked) {
                        if (userIngredientList.contains(ingredient)) {
                            if (!userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                                usersReference.document(userDocumentID).collection("Ingredients")
                                        .document(userIngredientList.get(userIngredientList.indexOf(ingredient)).getDocumentId()).set(ingredient);
                                materialCheckBox.setPaintFlags(materialCheckBox.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            }
                        } else {
                            usersReference.document(userDocumentID).collection("Ingredients")
                                    .document(ingredient.getDocumentId()).set(ingredient);

                        }
                    } else {
                        materialCheckBox.setPaintFlags(materialCheckBox.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }
                }
            });
            linearLayout.addView(materialCheckBox);
            ingredientCheckBoxList.add(materialCheckBox);
        }
    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
}
