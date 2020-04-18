package com.rokudoz.irecipe;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Account.LoginActivity;

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

                        //Dialog for sign out
                        View dialogView = getLayoutInflater().inflate(R.layout.dialog_simple_yes_no, null);
                        final Dialog dialog = new Dialog(EditPostActivity.this, R.style.CustomBottomSheetDialogTheme);
                        TextView title = dialogView.findViewById(R.id.dialog_simpleYesNo_title);
                        title.setText("Are you sure you want to delete this post?");
                        MaterialButton confirmBtn = dialogView.findViewById(R.id.dialog_simpleYesNo_confirmBtn);
                        MaterialButton cancelBtn = dialogView.findViewById(R.id.dialog_simpleYesNo_cancelBtn);
                        dialog.setContentView(dialogView);

                        confirmBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
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
                        cancelBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.cancel();
                            }
                        });

                        dialog.show();
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
