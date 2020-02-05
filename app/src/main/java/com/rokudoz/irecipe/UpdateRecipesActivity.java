package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public class UpdateRecipesActivity extends AppCompatActivity {

    private static final String TAG = "UpdateRecipesActivity";
    private List<Recipe> recipeList = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesReference = db.collection("Recipes");
    private CollectionReference ingredientsReference = db.collection("Ingredients");

    int totalRecipesToUpdate = 0;
    int recipesUpdated = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_recipes);

        Button updateBtn = findViewById(R.id.updateRecipesBtn);

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetRecipesList();
            }
        });
    }


    private void UpdateRecipes() {
        for (final Recipe recipe : recipeList) {
            recipe.setComplexity("Easy");
            recipe.setDuration(5f);
            recipe.setDurationType("Minutes");

            recipesReference.document(recipe.getDocumentId()).set(recipe).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: Updated " + recipe.getTitle());
                    recipesUpdated++;
                    if (recipesUpdated == totalRecipesToUpdate) {
                        Toast.makeText(UpdateRecipesActivity.this, "Updated all of the recipes", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void GetRecipesList() {
        recipesReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Recipe recipe = documentSnapshot.toObject(Recipe.class);
                        recipe.setDocumentId(documentSnapshot.getId());
                        if (!recipeList.contains(recipe)) {
                            recipeList.add(recipe);
                        }
                    }
                    totalRecipesToUpdate = queryDocumentSnapshots.size();
                    UpdateRecipes();
                }
            }
        });
    }

}
