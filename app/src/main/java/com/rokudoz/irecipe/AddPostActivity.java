package com.rokudoz.irecipe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;

public class AddPostActivity extends AppCompatActivity {

    MaterialButton searchRecipeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        searchRecipeBtn = findViewById(R.id.addPost_selectRecipe_btn);

        searchRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddPostActivity.this, SearchRecipeActivity.class);
                startActivity(intent);
            }
        });
    }
}
