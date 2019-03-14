package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class RecipeDetailed extends AppCompatActivity {
    private static final String TAG = "RecipeDetailed";
    private TextView tvTitle, tvDescription, tvIngredients;
    private ImageView mImageView;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesRef = db.collection("Recipes");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detailed);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvIngredients = findViewById(R.id.tvIngredientsList);
        mImageView = findViewById(R.id.recipeDetailed_image);

        getIncomingIntent();
    }

    private void getIncomingIntent() {
        Log.d(TAG, "getIncomingIntent: checking for intent");

        if (getIntent().hasExtra("documentID")) {
            Log.d(TAG, "getIncomingIntent: found intent extras");

            String documentID = getIntent().getStringExtra("documentID");
            String title = getIntent().getStringExtra("title");
            String description = getIntent().getStringExtra("description");
            String imageUrl = getIntent().getStringExtra("imageUrl");
            String ingredients = "";

            @SuppressWarnings("unchecked")
            HashMap<String, Boolean> tags = (HashMap<String, Boolean>) getIntent().getSerializableExtra("ingredients");

            for (String tag : tags.keySet()) {
                if (tags.get(tag)) {
                    ingredients += "\n- " + tag;
                }
            }

            tvTitle.setText(title);
            tvDescription.setText(description);
            tvIngredients.setText(ingredients);

            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .centerCrop()
                    .into(mImageView);
        }
    }

    public void navigateToSearchActivity(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
