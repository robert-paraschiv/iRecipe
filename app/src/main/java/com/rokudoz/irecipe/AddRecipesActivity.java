package com.rokudoz.irecipe;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.PossibleIngredients;
import com.rokudoz.irecipe.Models.Recipe;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AddRecipesActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");
    private StorageTask mUploadTask;

    private EditText editTextTitle, editTextDescription, editTextTags;
    private Button mChooseFileBtn, mAddRecipeBtn;
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipes);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        editTextTags = findViewById(R.id.edit_text_tags);
        mProgressBar = findViewById(R.id.addRecipes_progressbar);
        mImageView = findViewById(R.id.addRecipes_image);
        mChooseFileBtn = findViewById(R.id.addRecipes_choose_path_btn);
        mAddRecipeBtn = findViewById(R.id.addRecipes_add_btn);


        mChooseFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        mAddRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(AddRecipesActivity.this, "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    addRecipe();
                }
            }
        });

    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();

            //Picasso.with(this).load(mImageUri).into(mImageView);  --OLD lib
            Picasso.get().load(mImageUri).into(mImageView);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    // Adding Recipes -----------------------------------------------------------------------------
    public void addRecipe() {
        final String title = editTextTitle.getText().toString();
        final String description = editTextDescription.getText().toString();


        String tagInput = editTextTags.getText().toString();
        String[] possibleIngredients = PossibleIngredients.getIngredientsNames();
        String[] tagArray = tagInput.split("\\s*,\\s*");

        final Map<String, Boolean> tags = new HashMap<>();
        for (String ingredient : possibleIngredients) {
            tags.put(ingredient, false);
        }

        //Checks if ingredients in edit text are included in the PossibleIngredients Array
        if (!tagInput.trim().equals("")) {
            for (String tag : tagArray) {
                if (Arrays.asList(possibleIngredients).contains(tag)) {
                    tags.put(tag, true);
                }

            }
        }


        // Uploading image to Firestore
        if (mImageUri != null) {
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            mUploadTask = fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String imageUrl = uri.toString();

                                    // Sends recipe data to Firestore database
                                    Recipe recipe = new Recipe(title, description, tags, imageUrl,false);

                                    collectionReference.add(recipe)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(0);
                                }
                            }, 50);
                            Toast.makeText(AddRecipesActivity.this, "Upload Succesfull", Toast.LENGTH_SHORT).show();
                            editTextTags.setText("");
                            editTextDescription.setText("");
                            editTextTitle.setText("");
                            mImageView.setImageResource(android.R.color.transparent);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mProgressBar.setProgress((int) progress);
                        }
                    });

        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }
//  ----------------------------------------------------------------------------------------------


    public void backToSearch(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
