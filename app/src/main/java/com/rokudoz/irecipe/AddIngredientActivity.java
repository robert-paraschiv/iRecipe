package com.rokudoz.irecipe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class AddIngredientActivity extends AppCompatActivity {

    private static final String TAG = "AddIngredientActivity";

    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference ingredientsReference = db.collection("Ingredients");

    String category;
    List<String> ingredient_list;

    TextInputEditText textInputEditText;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ingredient);

        textInputEditText = findViewById(R.id.textinput_ingredientName);
        MaterialButton addIngredientBtn = findViewById(R.id.materialBtn_addIngredientToDb);
        ingredientsReference.document("ingredient_categories").addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e == null && documentSnapshot != null) {
                    ingredient_categories = (List<String>) documentSnapshot.get("categories");
                    Log.d(TAG, "onEvent: " + ingredient_categories);
                    categories = ingredient_categories.toArray(new String[0]);
                    setUpCategorySpinner();
                }
            }
        });

        addIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Ingredient ingredient = new Ingredient(textInputEditText.getText().toString(), category, 0f, "g", false);
                ingredient_list = new ArrayList<>();


                ingredientsReference.document("ingredient_list").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot != null) {
                            ingredient_list = (List<String>) documentSnapshot.get("ingredient_list");
                            if (!ingredient_list.contains(ingredient.getName())) {
                                ingredient_list.add(ingredient.getName());

                                ingredientsReference.document("ingredient_list").update("ingredient_list", ingredient_list)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onSuccess: updated ingredient_List");

                                    }
                                });
                                ingredientsReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        if (queryDocumentSnapshots != null) {
                                            List<Ingredient> ingredients = new ArrayList<>();
                                            for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                                                Ingredient ing = queryDocumentSnapshot.toObject(Ingredient.class);
                                                ingredients.add(ing);
                                            }
                                            if (!ingredients.contains(ingredient)) {
                                                ingredientsReference.add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        Toast.makeText(AddIngredientActivity.this, "Added ingredient +" + ingredient.getName()
                                                                , Toast.LENGTH_SHORT).show();
                                                        Log.d(TAG, "onSuccess: ADDED " + ingredient.getName());
                                                        textInputEditText.setText("");
                                                    }
                                                });
                                            }

                                        }
                                    }
                                });
                            }
                        }
                    }
                });


            }
        });
    }

    private void setUpCategorySpinner() {
        Spinner dropdown = findViewById(R.id.spinner_ingredientCategory);
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                category = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


}
