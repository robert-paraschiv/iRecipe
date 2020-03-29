package com.rokudoz.irecipe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class EditPostActivity extends AppCompatActivity {

    String postID = "";
    String post_imageUrl = "";
    String post_text = "";
    String post_privacy = "";
    String post_referencedRecipe = "";
    int updatedfields = 0;

    ImageView postImage;
    EditText postTextEditText;
    MaterialButton changeRecipe, updatePost, deletePost;
    private Spinner privacy_spinner;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);


        postImage = findViewById(R.id.editPost_image);
        postTextEditText = findViewById(R.id.editPost_description_editText);
        deletePost = findViewById(R.id.editPost_delete_btn);
        changeRecipe = findViewById(R.id.editPost_selectRecipe_btn);
        privacy_spinner = findViewById(R.id.editPost_privacy_spinner);
        updatePost = findViewById(R.id.editPost_post_btn);


        if (getIntent() != null) {
            if (getIntent().getStringExtra("post_id") != null) {
                postID = getIntent().getStringExtra("post_id");
                deletePost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(EditPostActivity.this,
                                R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                        materialAlertDialogBuilder.setMessage("Are you sure you want to delete your post?");
                        materialAlertDialogBuilder.setCancelable(true);
                        materialAlertDialogBuilder.setPositiveButton(
                                "Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        postsRef.document(postID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });
                                        dialog.cancel();
                                    }
                                });

                        materialAlertDialogBuilder.setNegativeButton(
                                "No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        materialAlertDialogBuilder.show();
                    }
                });


                updatePost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WriteBatch batch = db.batch();
                        batch.update(postsRef.document(postID), "privacy", privacy_spinner.getSelectedItem().toString());
                        batch.update(postsRef.document(postID), "referenced_recipe_docId", post_referencedRecipe);
                        batch.update(postsRef.document(postID), "text", postTextEditText.getText().toString());
                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(EditPostActivity.this, "Successfully updated your post", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
            }
            if (getIntent().getStringExtra("post_imageUrl") != null) {
                post_imageUrl = getIntent().getStringExtra("post_imageUrl");
                Glide.with(postImage).load(post_imageUrl).centerCrop().into(postImage);
            }
            if (getIntent().getStringExtra("post_text") != null) {
                post_text = getIntent().getStringExtra("post_text");
                postTextEditText.setText(post_text);
            }
            if (getIntent().getStringExtra("post_privacy") != null) {
                post_privacy = getIntent().getStringExtra("post_privacy");
                if (post_privacy.equals("Everyone"))
                    privacy_spinner.setSelection(0);
                else if (post_privacy.equals("Only Friends"))
                    privacy_spinner.setSelection(1);
                else if (post_privacy.equals("Only Me"))
                    privacy_spinner.setSelection(2);
            }
            if (getIntent().getStringExtra("post_referencedRecipe") != null) {
                post_referencedRecipe = getIntent().getStringExtra("post_referencedRecipe");

                changeRecipe.setText("Change Recipe");
                changeRecipe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(EditPostActivity.this, SelectRecipeActivity.class);
                        intent.putExtra("coming_from", "EditPostActivity");
                        intent.putExtra("post_id", postID);
                        intent.putExtra("post_imageUrl", post_imageUrl);
                        intent.putExtra("post_text", postTextEditText.getText().toString());
                        intent.putExtra("post_privacy", privacy_spinner.getSelectedItem().toString());
                        intent.putExtra("post_referencedRecipe", post_referencedRecipe);
                        startActivity(intent);
                    }
                });
            }


        }


    }


}
