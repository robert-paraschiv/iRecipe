package com.rokudoz.irecipe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Recipe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipesActivity extends AppCompatActivity {

    private static final String TAG = "AddRecipesActivity";

    private static final int SELECT_PICTURES = 1;
    private int nrOfPhotosUploaded = 0;
    private Uri mImageUri;
    private Uri[] mImageUriArray;
    private String[] imageUrlArray;
    private List<String> possibleIngredientList;
    private String[] possibleIngredientStringArray;
    private List<String> recipeIngredientList;
    private String category;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");
    private StorageTask mUploadTask;

    private EditText editTextTitle, editTextDescription, editTextTags, editTextInstructions;
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
        editTextInstructions = findViewById(R.id.edit_text_instructions);

        setUpCategorySpinner();


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
                    getIngredientList();
                }
            }
        });

    }

    private void setUpCategorySpinner() {
        Spinner dropdown = findViewById(R.id.spinner_category);
        String[] items = new String[]{"breakfast", "lunch", "dinner"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                category = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select picture"), SELECT_PICTURES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PICTURES && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                //Multiple images have been selected

                int count = data.getClipData().getItemCount();
                mImageUriArray = new Uri[count];

                for (int i = 0; i < count; i++) {
                    mImageUriArray[i] = data.getClipData().getItemAt(i).getUri();
                    mImageUri = data.getClipData().getItemAt(i).getUri();
                }
                Picasso.get().load(mImageUri).into(mImageView);

            } else if (data.getData() != null) {
                //Only one image has been selected
                mImageUri = data.getData();
                Picasso.get().load(mImageUri).into(mImageView);
            }

        } else
            Log.d(TAG, "onActivityResult: FailedToGetActiviayReasuslt");
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    private void getIngredientList() {
        ingredientsReference.document("ingredient_list")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            possibleIngredientList = (List<String>) documentSnapshot.get("ingredient_list");
                            possibleIngredientStringArray = possibleIngredientList.toArray(new String[possibleIngredientList.size()]);


                            getPhotoUploadCount();
                        }
                    }
                });
    }

    private void addPhotoToFirestore(Uri uri, final int position) {
        // Uploading image to Firestore
        if (mImageUriArray != null) {
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUriArray[position]));

            mUploadTask = fileReference.putFile(mImageUriArray[position])
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String imageUrl = uri.toString();
                                    imageUrlArray[position] = imageUrl;
                                    nrOfPhotosUploaded++;

                                    if (nrOfPhotosUploaded == mImageUriArray.length)
                                        addRecipe();
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

    // Adding Recipes -----------------------------------------------------------------------------
    public void addRecipe() {
        final String title = editTextTitle.getText().toString();
        final String description = editTextDescription.getText().toString();
        final String instructions = editTextInstructions.getText().toString();

        String tagInput = editTextTags.getText().toString();
        List<String> inputIngredientList = Arrays.asList(tagInput.split("\\s*,\\s*"));

        recipeIngredientList = new ArrayList<>();

        final Map<String, Boolean> tags = new HashMap<>();

        if (!tagInput.trim().equals(""))
            for (String tag : possibleIngredientList) {
                if (possibleIngredientList.contains(tag) && inputIngredientList.contains(tag)) {
                    tags.put(tag, true);
                    recipeIngredientList.add(tag);
                } else if (possibleIngredientList.contains(tag) && !inputIngredientList.contains(tag)) {
                    tags.put(tag, false);
                }
            }

        // Sends recipe data to Firestore database
        Recipe recipe = new Recipe(title, category, description, tags, Arrays.asList(imageUrlArray), false, recipeIngredientList, instructions, 0);

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

    private void getPhotoUploadCount() {
        //Getting uploaded to FireStore
        imageUrlArray = new String[mImageUriArray.length];
        for (int i = 0; i < mImageUriArray.length; i++) {
            addPhotoToFirestore(mImageUriArray[i], i);
        }
    }
//  ----------------------------------------------------------------------------------------------


    public void backToSearch(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
