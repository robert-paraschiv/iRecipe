package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.TestRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";

    TextView textView;
    MaterialCheckBox checkBox;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("RecipesTest");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        textView = findViewById(R.id.textView);
        Button btn = findViewById(R.id.button);
        Button getBtn = findViewById(R.id.getRecipesButton);
        Button updateBtn = findViewById(R.id.updateRecipeButton);
        checkBox = findViewById(R.id.checkBox);

        getRecipes();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecipe();
            }
        });
        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRecipes();
            }
        });
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRecipe();
            }
        });
    }

    private void updateRecipe() {
        collectionReference.whereEqualTo("title", "Test").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        TestRecipe testRecipe = documentSnapshot.toObject(TestRecipe.class);
                        testRecipe.setDocumentId(documentSnapshot.getId());
                        List<Ingredient> ingredientList = testRecipe.getIngredients_list();

                        for (Ingredient ingredient : ingredientList) {
                            if (ingredient.getName().equals(checkBox.getText()))
                                ingredient.setOwned(checkBox.isChecked());
                            Log.d(TAG, "onSuccess: " + ingredient.getName() + " " + ingredient.getOwned());
                            textView.append(ingredient.getName() + " " + ingredient.getOwned() + "\n");
                        }

                        collectionReference.document(testRecipe.getDocumentId()).update("ingredients_list", ingredientList)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(TestActivity.this, "Updated fields", Toast.LENGTH_SHORT).show();
                                getRecipes();
                            }
                        });

                    }
                }
            }
        });
    }

    private void getRecipes() {
        collectionReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        TestRecipe testRecipe = documentSnapshot.toObject(TestRecipe.class);
                        List<Ingredient> ingredientList = testRecipe.getIngredients_list();

                        for (Ingredient ingredient : ingredientList) {
                            Log.d(TAG, "onSuccess: " + ingredient.getName() + " " + ingredient.getOwned());
                            textView.append(ingredient.getName() + " " + ingredient.getOwned() + "\n");
                        }
                    }
                }
            }
        });
    }

    // Adding Recipes -----------------------------------------------------------------------------
    public void addRecipe() {
        String title = "Test";
        String category = "TestCateg";
        String description = "TestDesc";
        List<String> keywords = new ArrayList<>();
        List<String> imageUrls_list = new ArrayList<>();
        List<String> ratings_docId_list = new ArrayList<>();
        List<Ingredient> ingredients_list = new ArrayList<>();
        List<Instruction> instructions_list = new ArrayList<>();
        Boolean isFavorite = false;


        Ingredient ing1 = new Ingredient("mere", "fruit", 10f, "g", false);
        Ingredient ing2 = new Ingredient("pere", "fruit", 23f, "g", false);
        Ingredient ing3 = new Ingredient("potatoes", "vegetable", 33f, "g", false);

        Instruction instruction1 = new Instruction(1, "FIRST STEP TEXT", "https://www.fitnessmusicshop.com/media/catalog/product/cache/2/image/9df78eab33525d08d6e5fb8d27136e95/r/e/reebok-step-black-white-01.jpg");
        Instruction instruction2 = new Instruction(2, "SECOND STEP TEXT", "https://resources.t-fitness.com/bilder/cardiostrong/smallfitness/step-2/cardiostrong-step-board-01_600.jpg");

        instructions_list.add(instruction1);
        instructions_list.add(instruction2);

        ingredients_list.add(ing1);
        ingredients_list.add(ing2);
        ingredients_list.add(ing3);

        TestRecipe testRecipe = new TestRecipe(title, "", category, description, keywords, imageUrls_list, ingredients_list
                , instructions_list, ratings_docId_list, 0f, isFavorite);

        collectionReference.add(testRecipe).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d(TAG, "onSuccess: added to db");
                Toast.makeText(TestActivity.this, "Added test recipe to Db", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "onFailure: ", e);
            }
        });

    }
}