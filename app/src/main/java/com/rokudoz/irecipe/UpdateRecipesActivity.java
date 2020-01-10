//package com.rokudoz.irecipe;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.EventListener;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.FirebaseFirestoreException;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.QuerySnapshot;
//import com.rokudoz.irecipe.Models.Recipe;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//import javax.annotation.Nullable;
//
//public class UpdateRecipesActivity extends AppCompatActivity {
//
//    private static final String TAG = "UpdateRecipesActivity";
//    private List<String> possibleIngredientList;
//
//    private FirebaseFirestore db = FirebaseFirestore.getInstance();
//    private CollectionReference recipesReference = db.collection("Recipes");
//    private CollectionReference ingredientsReference = db.collection("Ingredients");
//
//    int totalRecipesToUpdate = 0;
//    int recipesUpdated = 0;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_update_recipes);
//
//        Button updateBtn = findViewById(R.id.updateRecipesBtn);
//
//        updateBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getIngredientList();
//            }
//        });
//    }
//
//
//    private void getIngredientList() {
//        ingredientsReference.document("ingredient_list")
//                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
//                    @Override
//                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
//                        if (e == null) {
//                            possibleIngredientList = (List<String>) documentSnapshot.get("ingredient_list");
//
//                            GetRecipesList();
//                        }
//                    }
//                });
//    }
//
//    private void UpdateRecipe(final Recipe recipe) {
//
//        final Map<String, Boolean> tags = new HashMap<>();
//
//        //Checks if ingredients in recipe ingredient list are included in the PossibleIngredients List
//        for (String tag : possibleIngredientList) {
//            if (possibleIngredientList.contains(tag) && recipe.getIngredient_array().contains(tag)) {
//                tags.put(tag, true);
//            } else if (possibleIngredientList.contains(tag) && !recipe.getIngredient_array().contains(tag)) {
//                tags.put(tag, false);
//            }
//        }
//
//        recipesReference.document(recipe.getDocumentId()).update("tags", tags).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                Log.d(TAG, "onComplete: Updated Recipe : " + recipe.getTitle());
//
//                recipesUpdated++;
//                if (recipesUpdated == totalRecipesToUpdate) {
//                    Toast.makeText(UpdateRecipesActivity.this, "Updated all of the recipes", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG, "onComplete: Updated all of the Recipes");
//                }
//
//            }
//        });
//    }
//
//    private void GetRecipesList() {
//        recipesReference.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if (task.isSuccessful()) {
//                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
//                        Recipe recipe = document.toObject(Recipe.class);
//                        recipe.setDocumentId(document.getId());
//
//                        totalRecipesToUpdate++;
//                        UpdateRecipe(recipe);
//                    }
//                }
//            }
//        });
//    }
//
//}
