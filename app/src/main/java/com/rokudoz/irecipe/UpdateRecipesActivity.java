package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateRecipesActivity extends AppCompatActivity {

    private String[] possibleIngredientStringArray;
    private List<String> possibleIngredientList;
    private List<Recipe> recipeList;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");
    private CollectionReference ingredientsReference = db.collection("Ingredients");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_recipes);
    }


    private void getIngredientList() {
        ingredientsReference.document("ingredient_list")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            possibleIngredientList = (List<String>) documentSnapshot.get("ingredient_list");
                            possibleIngredientStringArray = possibleIngredientList.toArray(new String[possibleIngredientList.size()]);

                            //UpdateRecipe();
                        }
                    }
                });
    }

    // Update Recipe -----------------------------------------------------------------------------
    public void UpdateRecipe(List<String> recipe_ingredient_list) {

        final Map<String, Boolean> tags = new HashMap<>();

        //Checks if ingredients in recipe ingredient list are included in the PossibleIngredients List
        for (String tag : possibleIngredientList) {
            if (possibleIngredientList.contains(tag) && recipe_ingredient_list.contains(tag)) {
                tags.put(tag, true);
            } else if (possibleIngredientList.contains(tag) && !recipe_ingredient_list.contains(tag)) {
                tags.put(tag, false);
            }
        }

    }
//  ----------------------------------------------------------------------------------------------


}
