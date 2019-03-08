package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddRecipesActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");

    private EditText editTextTitle;
    private EditText editTextDescription;
    private EditText editTextTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipes);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        editTextTags = findViewById(R.id.edit_text_tags);

    }

    public void addNote(View v) {
        final String title = editTextTitle.getText().toString();
        String description = editTextDescription.getText().toString();


        String tagInput = editTextTags.getText().toString();
        String[] tagArray = tagInput.split("\\s*,\\s*");
        Map<String, Boolean> tags = new HashMap<>();

        for (String tag : tagArray) {
            tags.put(tag, true);
        }

        Recipe recipe = new Recipe(title, description, tags);

        collectionReference.add(recipe)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void backToSearch(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
