package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;

public class RecipeDetailed extends AppCompatActivity {
    private static final String TAG = "RecipeDetailed";
    private TextView tvTitle, tvDescription, tvIngredients;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesRef = db.collection("Recipes");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detailed);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvIngredients = findViewById(R.id.tvIngredientsList);

        getIncomingIntent();
    }

    private void getIncomingIntent(){
        Log.d(TAG, "getIncomingIntent: checking for intent");

        if (getIntent().hasExtra("documentID")){
            Log.d(TAG, "getIncomingIntent: found intent extras");

            String documentID = getIntent().getStringExtra("documentID");

            recipesRef.document(documentID).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()){
                                DocumentSnapshot documentSnapshot=task.getResult();
                                if (documentSnapshot.exists()){
                                    String ingredients = "";

                                        Recipe recipe = documentSnapshot.toObject(Recipe.class);
                                        recipe.setDocumentId(documentSnapshot.getId());

                                        String documentId = recipe.getDocumentId();
                                        String title = recipe.getTitle();
                                        String description = recipe.getDescription();
                                        tvTitle.setText(title);
                                        tvDescription.setText(description);

                                        for (String tag : recipe.getTags().keySet()) {
                                            ingredients += "\n- " + tag +" "+ recipe.getTags().get(tag);
                                        }


                                    tvIngredients.setText(ingredients);
                                }else {
                                    Log.d(TAG, "No such document");
                                }

                            }else {
                                Log.d(TAG, "get failed with ",task.getException());
                            }
                        }
                    });
        }
    }
    public void navigateToSearchActivity(View v) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        finish();
    }
}
