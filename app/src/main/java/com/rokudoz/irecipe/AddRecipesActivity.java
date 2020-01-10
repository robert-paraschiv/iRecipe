package com.rokudoz.irecipe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipesActivity extends AppCompatActivity {

    private static final String TAG = "AddRecipesActivity";

    //FireBase refs
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesReference = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");

    private static final int SELECT_PICTURES = 1;
    private int nrOfPhotosUploaded = 0;
    private Uri mImageUri;
    private Uri[] mImageUriArray;
    private String[] imageUrlArray;
    private StorageTask mUploadTask;

    private LinearLayout ingredientsLinearLayout;
    private ArrayList<EditText> ingredientEtList;
    private ArrayList<EditText> ingredientQuantityEtList;
    private ArrayList<Spinner> ingredientQuantityTypeSpinnerList;
    private List<Ingredient> allIngredientsList;
    private EditText editTextTitle, editTextDescription, editTextKeywords;
    private Button mChooseFileBtn, mPostRecipeBtn, mAddIngredientBtn;
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipes);

        ingredientsLinearLayout = findViewById(R.id.addRecipes_ingredients_linear_layout);

        editTextTitle = findViewById(R.id.addRecipes_title_editText);
        editTextDescription = findViewById(R.id.addRecipes_description_editText);
        mProgressBar = findViewById(R.id.addRecipes_progressbar);
        mImageView = findViewById(R.id.addRecipes_image);
        mChooseFileBtn = findViewById(R.id.addRecipes_choose_path_btn);
        mPostRecipeBtn = findViewById(R.id.addRecipes_add_btn);
        editTextKeywords = findViewById(R.id.addRecipes_keywords_editText);
        mAddIngredientBtn = findViewById(R.id.addRecipes_addIngredient_btn);

        ingredientEtList = new ArrayList<>();
        ingredientQuantityEtList = new ArrayList<>();
        ingredientQuantityTypeSpinnerList = new ArrayList<>();
        allIngredientsList = new ArrayList<>();

        mImageUriArray = new Uri[0];

        setUpRecipeCategorySpinner();
        addEditText();

        mChooseFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        mPostRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(AddRecipesActivity.this, "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    getIngredientList();
                }
            }
        });
        mAddIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEditText();
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
                mImageUriArray = new Uri[1];
                mImageUriArray[0] = mImageUri;
                Picasso.get().load(mImageUri).into(mImageView);
            }

        } else
            Log.d(TAG, "onActivityResult: Failed to get ActivityResult");
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    private void getIngredientList() {
        ingredientsReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                        allIngredientsList.add(ingredient);
                    }

                    addRecipe();
//                    getPhotoUploadCount();
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
        final List<Ingredient> ingredient_list = new ArrayList<>();
        final List<Instruction> instruction_list = new ArrayList<>();
        final String creator_docId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "addRecipe: " + creator_docId);

        for (int i = 0; i < ingredientEtList.size(); i++) {
            if (!ingredientEtList.get(i).getText().toString().equals("") && !ingredientQuantityEtList.get(i).getText().toString().equals("")) {
                Ingredient ingredientToAdd = new Ingredient(ingredientEtList.get(i).getText().toString(), ""
                        , Float.parseFloat(ingredientQuantityEtList.get(i).getText().toString())
                        , ingredientQuantityTypeSpinnerList.get(i).getSelectedItem().toString(), false);
                if (allIngredientsList.contains(ingredientToAdd)) {
                    ingredientToAdd.setCategory(allIngredientsList.get(allIngredientsList.indexOf(ingredientToAdd)).getCategory());
                    ingredient_list.add(ingredientToAdd);
                }
            } else {
                Toast.makeText(this, "Make sure ingredient boxes are not empty", Toast.LENGTH_SHORT).show();
            }
        }

//        // Sends recipe data to Firestore database
//        Recipe recipe = new Recipe(title, description);
//
//        collectionReference.add(recipe)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
//
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });


    }

    private void addEditText() {
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        ingredientsLinearLayout.addView(linearLayout);

        EditText editText = new EditText(this);
        editText.setHint("Ingredient ");
        setEditTextAttributes(editText);
        linearLayout.addView(editText);
        ingredientEtList.add(editText);

        EditText editText2 = new EditText(this);
        editText2.setHint("Quantity ");
        setEditTextAttributes(editText2);
        linearLayout.addView(editText2);
        ingredientQuantityEtList.add(editText2);

        Spinner spinner = new Spinner(this);
        String[] spinnerItems = {"g", "kg"};
        linearLayout.addView(spinner);
        setUpIngredientQuantitySpinner(spinner, spinnerItems);
    }

    private void setUpRecipeCategorySpinner() {
        Spinner dropdown = findViewById(R.id.addRecipes_category_spinner);
        String[] items = new String[]{"breakfast", "lunch", "dinner"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
    }

    private void setUpIngredientQuantitySpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );
        spinner.setLayoutParams(params);
        spinner.setAdapter(adapter);
        ingredientQuantityTypeSpinnerList.add(spinner);
    }

    private void setEditTextAttributes(EditText editText) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );

        editText.setLayoutParams(params);
    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    private void getPhotoUploadCount() {
        //Getting uploaded to FireStore
        imageUrlArray = new String[mImageUriArray.length];
        for (int i = 0; i < mImageUriArray.length; i++) {
            addPhotoToFirestore(mImageUriArray[i], i);
        }
    }
//  ----------------------------------------------------------------------------------------------

}
