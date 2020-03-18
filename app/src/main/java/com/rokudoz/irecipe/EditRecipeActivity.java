package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
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
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Utils.Adapters.EditRecipe.EditRecipeIngredientsAdapter;
import com.rokudoz.irecipe.Utils.Adapters.EditRecipe.EditRecipeInstructionsAdapter;
import com.rokudoz.irecipe.Utils.RotateBitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity implements EditRecipeInstructionsAdapter.OnItemClickListener {
    private static final String TAG = "EditRecipeActivity";

    //Components
    ImageView recipeImageView;
    MaterialButton recipePhotoBtn, addIngredientBtn, addInstructionBtn, saveBtn, removeInstructionBtn, removeIngredientBtn;
    TextInputEditText titleInputEditText, descriptionInputEditText, keywordsInputEditText;
    Spinner categorySpinner, privacySpinner;
    RecyclerView ingredientsRecyclerView, instructionsRecyclerView;
    EditText durationEditText;
    AppCompatSpinner durationTypeSpinner, complexitySpinner;


    EditRecipeIngredientsAdapter editRecipeIngredientsAdapter;
    EditRecipeInstructionsAdapter editRecipeInstructionsAdapter;


    List<Instruction> instructionList = new ArrayList<>();
    List<Ingredient> ingredientList = new ArrayList<>();


    private List<Ingredient> allIngredientsList = new ArrayList<>();
    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;

    String recipe_id = "";
    Recipe mRecipe = new Recipe();
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;
    Bitmap imageBitmap;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesReference = db.collection("Recipes");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");
    private StorageTask mUploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);


        recipeImageView = findViewById(R.id.editRecipes_image);
        recipePhotoBtn = findViewById(R.id.editRecipes_choose_path_btn);
        addIngredientBtn = findViewById(R.id.editRecipes_addIngredient_btn);
        addInstructionBtn = findViewById(R.id.editRecipes_addInstruction_btn);
        saveBtn = findViewById(R.id.editRecipes_save_btn);
        titleInputEditText = findViewById(R.id.editRecipes_title_editText);
        descriptionInputEditText = findViewById(R.id.editRecipes_description_editText);
        keywordsInputEditText = findViewById(R.id.editRecipes_keywords_editText);
        categorySpinner = findViewById(R.id.editRecipes_category_spinner);
        privacySpinner = findViewById(R.id.editRecipes_privacy_spinner);
        ingredientsRecyclerView = findViewById(R.id.editRecipe_ingredients_recyclerview);
        instructionsRecyclerView = findViewById(R.id.editRecipe_instructions_recyclerview);
        durationEditText = findViewById(R.id.editRecipes_duration_editText);
        durationTypeSpinner = findViewById(R.id.editRecipes_durationType_Spinner);
        complexitySpinner = findViewById(R.id.editRecipes_complexity_Spinner);
        removeIngredientBtn = findViewById(R.id.editRecipes_removeIngredient_btn);
        removeInstructionBtn = findViewById(R.id.editRecipes_removeInstruction_btn);


        if (getIntent() != null && getIntent().getStringExtra("recipe_id") != null) {
            recipe_id = getIntent().getStringExtra("recipe_id");
            if (recipe_id != null) {
                getRecipeDetails();
            }
        }
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getIngredientList();
            }
        });
        addInstructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Instruction instruction = new Instruction();
                instruction.setText("");
                instruction.setStepNumber(instructionList.size() + 1);
                instructionList.add(instruction);
                int position = instructionList.size();
                editRecipeInstructionsAdapter.notifyItemInserted(position);
            }
        });
        addIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ingredient ingredient = new Ingredient();
                ingredient.setName("");
                ingredientList.add(ingredient);
                int position = ingredientList.size();
                editRecipeIngredientsAdapter.notifyItemInserted(position);
            }
        });
        removeInstructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionList.remove(instructionList.get(instructionList.size() - 1));
                editRecipeInstructionsAdapter.notifyItemRemoved(instructionList.size());
            }
        });
        removeIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingredientList.remove(ingredientList.get(ingredientList.size() - 1));
                editRecipeIngredientsAdapter.notifyItemRemoved(ingredientList.size());
            }
        });
        recipePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        buildRecyclerViews();

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
                    ingredientsReference.document("ingredient_categories").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot != null) {
                                ingredient_categories = (List<String>) documentSnapshot.get("categories");
                                Log.d(TAG, "onEvent: " + ingredient_categories);
                                categories = ingredient_categories.toArray(new String[0]);

                                saveRecipe();
                            }
                        }
                    });
                }
            }
        });
    }

    // Adding Recipes -----------------------------------------------------------------------------
    public void saveRecipe() {
        final String title = titleInputEditText.getText().toString();
        final String description = descriptionInputEditText.getText().toString();
        final String creator_docId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final List<String> imageUrls_list = mRecipe.getImageUrls_list();
        final String category = categorySpinner.getSelectedItem().toString();
        final List<Ingredient> ingredients_list = new ArrayList<>();
        final List<Ingredient> ingredients_without_category = new ArrayList<>();
        final Boolean isFavorite = false;
        final String privacy = privacySpinner.getSelectedItem().toString();
        final String complexity = complexitySpinner.getSelectedItem().toString();
        Float duration = 0f;
        if (!durationEditText.getText().toString().trim().equals(""))
            duration = Float.parseFloat(durationEditText.getText().toString());
        final String durationType = durationTypeSpinner.getSelectedItem().toString();
        Log.d(TAG, "addRecipe: " + creator_docId);


        String keywordEt = keywordsInputEditText.getText().toString();
        final List<String> keywords = Arrays.asList(keywordEt.split("\\s*,\\s*"));

        //Get ingredients list items from edit texts
        for (Ingredient ingredient : ingredientList) {
            if (!ingredient.getName().equals("") && !ingredient.getQuantity().toString().equals("")) {
                if (allIngredientsList.contains(ingredient)) {
                    ingredient.setCategory(allIngredientsList.get(allIngredientsList.indexOf(ingredient)).getCategory());
                    ingredients_list.add(ingredient);
                } else {
                    ingredients_without_category.add(ingredient);
                }
            } else {
                Toast.makeText(this, "Make sure ingredient boxes are not empty", Toast.LENGTH_SHORT).show();
            }
        }

        if (ingredients_without_category.size() > 0) {


            //
            final LinearLayout linearLayout = new LinearLayout(EditRecipeActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final List<Spinner> category_spinner_list = new ArrayList<>();
            //create an adapter to describe how the items are displayed, adapters are used in several places in android.
            //There are multiple variations of this, but this is the basic variant.
            ArrayAdapter<String> adapter = new ArrayAdapter<>(EditRecipeActivity.this, android.R.layout.simple_spinner_dropdown_item, categories);
            //set the spinners adapter to the previously created one.
            for (Ingredient ingredient : ingredients_without_category) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

                params.setMargins(convertDpToPixel(16),
                        convertDpToPixel(16),
                        convertDpToPixel(16),
                        0
                );
                Spinner spinner = new Spinner(EditRecipeActivity.this);
                TextView textView = new TextView(EditRecipeActivity.this);
                spinner.setLayoutParams(params);
                spinner.setAdapter(adapter);
                textView.setText(ingredient.getName());
                textView.setLayoutParams(params);
                category_spinner_list.add(spinner);
                linearLayout.addView(textView);
                linearLayout.addView(spinner);
            }


            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(EditRecipeActivity.this
                    , R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
            materialAlertDialogBuilder.setView(linearLayout);
            materialAlertDialogBuilder.setMessage("This ingredient isn't in our Database yet, please specify the category");
            materialAlertDialogBuilder.setCancelable(true);
            final Float finalDuration = duration;
            materialAlertDialogBuilder.setPositiveButton(
                    "Confirm",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //
                            for (int i = 0; i < category_spinner_list.size(); i++) {
                                ingredients_without_category.get(i).setCategory(category_spinner_list.get(i).getSelectedItem().toString());
                                ingredients_without_category.get(i).setQuantity(0f);
                                ingredients_without_category.get(i).setQuantity_type("g");
                                ingredients_list.add(ingredients_without_category.get(i));

                                ingredientsReference.add(ingredients_without_category.get(i)).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d(TAG, "onSuccess: added ingredient to db");
                                    }
                                });

                            }

                            Recipe recipe = new Recipe(title, creator_docId, mRecipe.getCreator_name(), mRecipe.getCreator_imageUrl(), category, description, ingredients_list
                                    , instructionList, keywords, imageUrls_list, complexity, finalDuration, durationType, 0f, isFavorite, privacy, mRecipe.getNumber_of_likes()
                                    , mRecipe.getNumber_of_comments());

                            Log.d(TAG, "addRecipe: " + recipe.toString());

                            // Sends recipe data to Firestore database
                            recipesReference.document(mRecipe.getDocumentId()).set(recipe)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(EditRecipeActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(EditRecipeActivity.this, MainActivity.class);
                                            intent.putExtra("recipe_id", mRecipe.getDocumentId());
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(EditRecipeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            dialog.cancel();
                        }
                    });

            materialAlertDialogBuilder.setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            materialAlertDialogBuilder.show();
        } else {
            Recipe recipe = new Recipe(title, creator_docId, mRecipe.getCreator_name(), mRecipe.getCreator_imageUrl(), category, description, ingredients_list
                    , instructionList, keywords, imageUrls_list, complexity, duration, durationType, 0f, isFavorite, privacy, mRecipe.getNumber_of_likes()
                    , mRecipe.getNumber_of_comments());

            Log.d(TAG, "addRecipe: " + recipe.toString());

            // Sends recipe data to Firestore database
            recipesReference.document(mRecipe.getDocumentId()).set(recipe)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(EditRecipeActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EditRecipeActivity.this, MainActivity.class);
                            intent.putExtra("recipe_id", mRecipe.getDocumentId());
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditRecipeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    private void buildRecyclerViews() {
//        ingredientsRecyclerView.setHasFixedSize(true);
//        instructionsRecyclerView.setHasFixedSize(true);

        editRecipeIngredientsAdapter = new EditRecipeIngredientsAdapter(ingredientList);
        editRecipeInstructionsAdapter = new EditRecipeInstructionsAdapter(instructionList);

        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ingredientsRecyclerView.setAdapter(editRecipeIngredientsAdapter);
        instructionsRecyclerView.setAdapter(editRecipeInstructionsAdapter);

        editRecipeInstructionsAdapter.setOnItemClickListener(EditRecipeActivity.this);
    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    private void getRecipeDetails() {
        recipesReference.document(recipe_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                    if (recipe != null)
                        recipe.setDocumentId(documentSnapshot.getId());
                    mRecipe = recipe;
                    if (recipe != null) {
                        Glide.with(EditRecipeActivity.this).load(recipe.getImageUrls_list().get(0)).centerCrop().into(recipeImageView);

                        titleInputEditText.setText(recipe.getTitle());
                        descriptionInputEditText.setText(recipe.getDescription());
                        durationEditText.setText(recipe.getDuration().toString());

                        String keywords = "";

                        if (recipe.getKeywords().size() == 1) {
                            keywords = recipe.getKeywords().get(0);
                        } else {
                            for (int i = 0; i < recipe.getKeywords().size(); i++) {
                                if (i == 0) {
                                    keywords += recipe.getKeywords().get(0) + ", ";
                                } else if (i == recipe.getKeywords().size() - 1) {
                                    keywords += recipe.getKeywords().get(i);
                                }
                            }
                        }


                        keywordsInputEditText.setText(keywords);

                        if (recipe.getInstruction_list() != null)
                            for (Instruction instruction : recipe.getInstruction_list()) {
                                instructionList.add(instruction);
                                editRecipeInstructionsAdapter.notifyItemInserted(instructionList.indexOf(instruction));
                            }
                        if (recipe.getIngredient_list() != null)
                            for (Ingredient ingredient : recipe.getIngredient_list()) {
                                ingredientList.add(ingredient);
                                editRecipeIngredientsAdapter.notifyItemInserted(ingredientList.indexOf(ingredient));
                            }
//
//                        editRecipeIngredientsAdapter.notifyDataSetChanged();
//                        editRecipeInstructionsAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    @Override
    public void onAddImageClick(int position) {
        Toast.makeText(this, "CLICKED " + position, Toast.LENGTH_SHORT).show();
        openFileChooser(position);
    }

    private void openFileChooser(int position) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST + position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int position = requestCode - 1;

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData();
            try {
                RotateBitmap rotateBitmap = new RotateBitmap();
                imageBitmap = rotateBitmap.HandleSamplingAndRotationBitmap(this, mImageUri);
                uploadPostPic(position);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            Picasso.get().load(mImageUri).into(imageView);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = this.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadPostPic(final int position) {
        if (mImageUri != null) {
            final StorageReference newFileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            //Compress image
            Bitmap bitmap = imageBitmap;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] data = baos.toByteArray();

            //Upload image to FireStore Storage
            mUploadTask = newFileReference.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            newFileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String imageUrl = uri.toString();
//                                    Instruction instruction = editRecipeInstructionsAdapter.getInstructionList().get(position);
//                                    instruction.setImgUrl(imageUrl);
//                                    instructionList.set(position, instruction);
                                    instructionList.get(position).setImgUrl(imageUrl);
                                    editRecipeInstructionsAdapter.notifyItemChanged(position);
                                    Log.d(TAG, "onSuccess: position" + position + instructionList.get(position).toString());
                                    //
                                }
                            });
                            Log.d(TAG, "onSuccess: Upload Succesfull");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditRecipeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            Toast.makeText(EditRecipeActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }
}
