package com.rokudoz.irecipe.Fragments;


import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.ShoppingListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ShoppingListFragment extends Fragment {

    private static final String TAG = "ShoppingListFragment";
    private FloatingActionButton addIngredientToListFab;

    private ShoppingListAdapter mAdapter;
    private RecyclerView recyclerView;

    private String userDocumentID = "";
    private List<Ingredient> allIngredientsList = new ArrayList<>();
    private List<Ingredient> userIngredientList = new ArrayList<>();
    private List<Ingredient> shoppingList_withCategories;
    private List<Ingredient> initialIngredientList = new ArrayList<>();
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
        return new ShoppingListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);
//        textViewData = view.findViewById(R.id.tv_data);
        mEmptyBasketBtn = view.findViewById(R.id.empty_basket_btn);
        recyclerView = view.findViewById(R.id.shoppingListFragment_recyclerView);
        addIngredientToListFab = view.findViewById(R.id.fab_add_ingredient_toShoppingList);

        shoppingList_withCategories = new ArrayList<>();

        buildRecyclerView();
        return view;
    }

    private void buildRecyclerView() {
        recyclerView.setHasFixedSize(true);
        mAdapter = new ShoppingListAdapter(getContext(), shoppingList_withCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        getUserIngredients();
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();

        saveIngredientsIfNecessary();
        Log.d(TAG, "onStop: " + shoppingList_withCategories.toString());
    }

    private void saveIngredientsIfNecessary() {
        Log.d(TAG, "saveIngredientsIfNecessary: " + userIngredientList.toString());

        for (final Ingredient ingredient : shoppingList_withCategories) {
            if (ingredient.getDocumentId() == null) {
                if (ingredient.getQuantity() != null && ingredient.getOwned() != null) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ShoppingList")
                            .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "onSuccess: added ingredient to db " + ingredient.toString());
                        }
                    });
                }
                if (userIngredientList.contains(ingredient)
                        && userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned() != ingredient.getOwned()) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                            .document(userIngredientList.get(userIngredientList.indexOf(ingredient)).getDocumentId())
                            .update("owned", ingredient.getOwned())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: updated in user ingredients list " + ingredient.toString());
                                }
                            });
                } else if (ingredient.getOwned() != null && !userIngredientList.contains(ingredient) && ingredient.getOwned()) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                            .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "onSuccess: added ingredient to db " + ingredient.toString());
                        }
                    });
                }
            } else {
                if (initialIngredientList.contains(ingredient) &&
                        initialIngredientList.get(initialIngredientList.indexOf(ingredient)).getOwned() != ingredient.getOwned()) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("ShoppingList")
                            .document(ingredient.getDocumentId()).update("owned", ingredient.getOwned())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: updated ingredient in db " + ingredient.toString());
                                }
                            });
                }
                if (userIngredientList.contains(ingredient)
                        && !userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned() && ingredient.getOwned()) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                            .document(userIngredientList.get(userIngredientList.indexOf(ingredient)).getDocumentId())
                            .update("owned", ingredient.getOwned())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: updated in user ingredients list " + ingredient.toString());
                                }
                            });
                } else if (!userIngredientList.contains(ingredient) && ingredient.getOwned()) {
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                            .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "onSuccess: added ingredient to db " + ingredient.toString());
                        }
                    });
                }
            }
        }
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
                            Log.d(TAG, "onEvent: categories " + ingredient_categories);
                            categories = ingredient_categories.toArray(new String[0]);
                        }
                    }
                });


                List<Ingredient> ingredientList = new ArrayList<>();

                for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(queryDocumentSnapshots)) {
                    if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                        Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                        ingredient.setDocumentId(queryDocumentSnapshot.getId());
                        initialIngredientList.add(new Ingredient(ingredient.getName(), ingredient.getCategory(), ingredient.getQuantity(),
                                ingredient.getQuantity_type(), ingredient.getOwned()));
                        if (!allIngredientsList.contains(ingredient)) {
                            allIngredientsList.add(ingredient);
                        } else if (allIngredientsList.contains(ingredient)) {
                            allIngredientsList.set(allIngredientsList.indexOf(ingredient), ingredient);
                        }

                        Collections.sort(allIngredientsList);
                    }
                }

                for (Ingredient ingredientt : allIngredientsList) {
                    if (!ingredientList.contains(ingredientt))
                        ingredientList.add(ingredientt);
                }
                Collections.sort(ingredientList);

                final List<String> categorylist = new ArrayList<>();
                for (Ingredient ing : ingredientList) {
                    if (!categorylist.contains(ing.getCategory()))
                        categorylist.add(ing.getCategory());
                }
                for (String categoryName : categorylist) {
                    Ingredient categoryIngredient = new Ingredient();
                    categoryIngredient.setName(categoryName);
                    categoryIngredient.setCategory_name(categoryName);
                    categoryIngredient.setCategory(categoryName);
                    if (!shoppingList_withCategories.contains(categoryIngredient))
                        shoppingList_withCategories.add(categoryIngredient);
                    for (Ingredient ingredient : ingredientList) {
                        if (ingredient.getCategory().equals(categoryName) && !shoppingList_withCategories.contains(ingredient)) {
                            shoppingList_withCategories.add(shoppingList_withCategories.indexOf(categoryIngredient) + 1, ingredient);
                        }
                    }

                }
                mAdapter.notifyDataSetChanged();


                if (allIngredientsList.size() > 0 && queryDocumentSnapshots.size() > 0) {
                    mEmptyBasketBtn.setVisibility(View.VISIBLE);
                    Log.d(TAG, "onEvent: NOT EMPTY" + " docs " + queryDocumentSnapshots.size());
                } else {
                    mEmptyBasketBtn.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onEvent: EMPTY");
                }
                // EMPTY basket on click
                mEmptyBasketBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (final Ingredient ingToDelete : allIngredientsList) {
                            usersReference.document(userDocumentID).collection("ShoppingList").document(ingToDelete.getDocumentId())
                                    .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: Deleted" + ingToDelete.toString() + " from shopping list");

                                }
                            });
                        }
                        shoppingList_withCategories.clear();
                        mAdapter.notifyDataSetChanged();
                        mEmptyBasketBtn.setVisibility(View.GONE);
                    }
                });


                // Add ingredient Manually
                addIngredientToListFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(8, 0, 8, 0);

                        String[] ingredientQuantitySpinnerItems = getResources().getStringArray(R.array.ingredient_quantity_type);
                        final LinearLayout linearLayout = new LinearLayout(getActivity());
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        final EditText input = new EditText(getActivity());
                        input.setHint("Ingredient name");
                        final EditText inputQuantity = new EditText(getActivity());
                        inputQuantity.setHint("Quantity");
                        final Spinner categorySpinner = new Spinner(getActivity());
                        final Spinner quantityTypeSpinner = new Spinner(getActivity());

                        input.setLayoutParams(params);
                        inputQuantity.setLayoutParams(params);

                        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
                        //There are multiple variations of this, but this is the basic variant.
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, categories);
                        ArrayAdapter<String> quantityAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, ingredientQuantitySpinnerItems);
                        //set the spinners adapter to the previously created one.
                        categorySpinner.setAdapter(adapter);
                        quantityTypeSpinner.setAdapter(quantityAdapter);
                        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        inputQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);

                        linearLayout.addView(input);
                        linearLayout.addView(categorySpinner);
                        linearLayout.addView(inputQuantity);
                        linearLayout.addView(quantityTypeSpinner);
                        materialAlertDialogBuilder.setView(linearLayout);
                        materialAlertDialogBuilder.setMessage("Add ingredient to shopping list");
                        materialAlertDialogBuilder.setCancelable(true);
                        materialAlertDialogBuilder.setPositiveButton(
                                "Confirm",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //
                                        int quantity = Integer.parseInt(inputQuantity.getText().toString());
                                        Ingredient ingredient = new Ingredient(input.getText().toString(), categorySpinner.getSelectedItem().toString(),
                                                (float) quantity, quantityTypeSpinner.getSelectedItem().toString(), false);

                                        if (input.getText().toString().trim().equals("")) {
                                            Toast.makeText(getActivity(), "You need to write the name of what you want to add to the shopping list", Toast.LENGTH_SHORT).show();
                                        } else {
                                            if (shoppingList_withCategories.contains(ingredient)) {
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

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
}
