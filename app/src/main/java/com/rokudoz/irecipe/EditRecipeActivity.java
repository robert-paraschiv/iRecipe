package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity implements EditRecipeInstructionsAdapter.OnItemClickListener
        , EditRecipeIngredientsAdapter.OnItemClickListener {
    private static final String TAG = "EditRecipeActivity";

    //Components
    ImageView recipeImageView;
    MaterialButton recipePhotoBtn, addIngredientBtn, addInstructionBtn, saveBtn;
    TextInputEditText titleInputEditText, descriptionInputEditText, keywordsInputEditText;
    Spinner categorySpinner, privacySpinner;
    RecyclerView ingredientsRecyclerView, instructionsRecyclerView;
    EditText durationEditText, portionsEditText;
    AppCompatSpinner durationTypeSpinner, complexitySpinner;

    ProgressDialog pd;


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

    private Uri mRecipeImageUri;

    private Uri[] mRecipeImageUriArray;
    private String[] recipeImageUrlArray;

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

        pd = new ProgressDialog(EditRecipeActivity.this);
        pd.setMessage("Please wait...");

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
        portionsEditText = findViewById(R.id.editRecipes_portionsEditText);

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
                ingredient.setOwned(false);
                ingredientList.add(ingredient);
                int position = ingredientList.size();
                editRecipeIngredientsAdapter.notifyItemInserted(position);
            }
        });
        recipePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select picture"), 0);
            }
        });
        buildRecyclerViews();

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

        Log.d(TAG, "onActivityResult: request code = " + requestCode);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                //Multiple images have been selected

                int count = data.getClipData().getItemCount();
                mRecipeImageUriArray = new Uri[count];

                recipeImageUrlArray = new String[count];
                for (int i = 0; i < count; i++) {
                    mRecipeImageUriArray[i] = data.getClipData().getItemAt(i).getUri();
                    mRecipeImageUri = data.getClipData().getItemAt(i).getUri();
                    addPhotoToFirestore(i);
                }
                Glide.with(recipeImageView).load(mRecipeImageUri).centerCrop().into(recipeImageView);

            } else if (data.getData() != null) {
                //Only one image has been selected
                mRecipeImageUri = data.getData();
                mRecipeImageUriArray = new Uri[1];
                mRecipeImageUriArray[0] = mRecipeImageUri;
                recipeImageUrlArray = new String[mRecipeImageUriArray.length];
                addPhotoToFirestore(0);
                Glide.with(recipeImageView).load(mRecipeImageUri).centerCrop().into(recipeImageView);
            }

        } else {
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
    }

    private void addPhotoToFirestore(final int position) {
        // Uploading image to Firestore
        if (mRecipeImageUriArray != null) {
            pd.show();
            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mRecipeImageUriArray[position]));

            //Compress Image
            Bitmap bitmap = null;
            try {
                RotateBitmap rotateBitmap = new RotateBitmap();
                bitmap = rotateBitmap.HandleSamplingAndRotationBitmap(this, mRecipeImageUriArray[position]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] data = baos.toByteArray();

            mUploadTask = fileReference.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String imageUrl = uri.toString();
                                    recipeImageUrlArray[position] = imageUrl;
                                    pd.hide();

                                }
                            });


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditRecipeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
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
        List<String> imageUrls_list = new ArrayList<>();
        if (recipeImageUrlArray == null) {
            imageUrls_list = mRecipe.getImageUrls_list();
        } else {
            imageUrls_list = Arrays.asList(recipeImageUrlArray);
        }

        final String category = categorySpinner.getSelectedItem().toString();
        final List<Ingredient> ingredients_list = new ArrayList<>();
        final List<Ingredient> ingredients_without_category = new ArrayList<>();
        final String privacy = privacySpinner.getSelectedItem().toString();
        final String complexity = complexitySpinner.getSelectedItem().toString();
        Integer duration = 0;
        if (!durationEditText.getText().toString().trim().equals(""))
            duration = Integer.parseInt(durationEditText.getText().toString());
        final String durationType = durationTypeSpinner.getSelectedItem().toString();
        Log.d(TAG, "addRecipe: " + creator_docId);


        String keywordEt = keywordsInputEditText.getText().toString();
        final List<String> keywords = Arrays.asList(keywordEt.split("\\s*,\\s*"));

        if (portionsEditText.getText().toString().trim().equals("")) {
            Toast.makeText(this, "Please tell us how many portions this recipe is for", Toast.LENGTH_SHORT).show();
            return;
        }

        final Integer portions = Integer.parseInt(portionsEditText.getText().toString());

        //Get instructions list in order
        for (int i = 0; i < instructionList.size(); i++) {
            instructionList.get(i).setStepNumber(i + 1);
        }

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
            final Integer finalDuration = duration;
            final List<String> finalImageUrls_list = imageUrls_list;
            materialAlertDialogBuilder.setPositiveButton(
                    "Confirm",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //
                            for (int i = 0; i < category_spinner_list.size(); i++) {
                                ingredients_without_category.get(i).setCategory(category_spinner_list.get(i).getSelectedItem().toString());
                                ingredients_list.add(ingredients_without_category.get(i));

                                ingredientsReference.add(ingredients_without_category.get(i)).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d(TAG, "onSuccess: added ingredient to db");
                                    }
                                });

                            }

                            //Update recipe in db
                            pd.show();
                            WriteBatch batch = db.batch();
                            DocumentReference documentReference = recipesReference.document(mRecipe.getDocumentId());
                            batch.update(documentReference, "title", title);
                            batch.update(documentReference, "category", category);
                            batch.update(documentReference, "description", description);
                            batch.update(documentReference, "ingredient_list", ingredients_list);
                            batch.update(documentReference, "instruction_list", instructionList);
                            batch.update(documentReference, "keywords", keywords);
                            batch.update(documentReference, "imageUrls_list", finalImageUrls_list);
                            batch.update(documentReference, "complexity", complexity);
                            batch.update(documentReference, "duration", finalDuration);
                            batch.update(documentReference, "durationType", durationType);
                            batch.update(documentReference, "privacy", privacy);
                            batch.update(documentReference, "portions", portions);
                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    pd.hide();
                                    Toast.makeText(EditRecipeActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(EditRecipeActivity.this, MainActivity.class);
                                    intent.putExtra("recipe_id", mRecipe.getDocumentId());
                                    startActivity(intent);
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
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
            pd.show();

            //Update recipe in db
            WriteBatch batch = db.batch();
            DocumentReference documentReference = recipesReference.document(mRecipe.getDocumentId());
            batch.update(documentReference, "title", title);
            batch.update(documentReference, "category", category);
            batch.update(documentReference, "description", description);
            batch.update(documentReference, "ingredient_list", ingredients_list);
            batch.update(documentReference, "instruction_list", instructionList);
            batch.update(documentReference, "keywords", keywords);
            batch.update(documentReference, "imageUrls_list", imageUrls_list);
            batch.update(documentReference, "complexity", complexity);
            batch.update(documentReference, "duration", duration);
            batch.update(documentReference, "durationType", durationType);
            batch.update(documentReference, "privacy", privacy);
            batch.update(documentReference, "portions", portions);
            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    pd.hide();
                    Toast.makeText(EditRecipeActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditRecipeActivity.this, MainActivity.class);
                    intent.putExtra("recipe_id", mRecipe.getDocumentId());
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditRecipeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void buildRecyclerViews() {
        editRecipeIngredientsAdapter = new EditRecipeIngredientsAdapter(ingredientList);
        editRecipeInstructionsAdapter = new EditRecipeInstructionsAdapter(instructionList);

        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ingredientsRecyclerView.setAdapter(editRecipeIngredientsAdapter);
        instructionsRecyclerView.setAdapter(editRecipeInstructionsAdapter);

        editRecipeInstructionsAdapter.setOnItemClickListener(EditRecipeActivity.this);
        editRecipeIngredientsAdapter.setOnItemClickListener(EditRecipeActivity.this);
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
                        durationEditText.setText(MessageFormat.format("{0}", recipe.getDuration()));
                        List<String> durationArray = Arrays.asList(getResources().getStringArray(R.array.DurationType));
                        durationTypeSpinner.setSelection(durationArray.indexOf(recipe.getDurationType()));
                        portionsEditText.setText(MessageFormat.format("{0}", recipe.getPortions()));

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


    private String getFileExtension(Uri uri) {
        ContentResolver cR = this.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadPostPic(final int position) {
        if (mImageUri != null) {
            final StorageReference newFileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            pd.show();

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
                                    instructionList.get(position).setImgUrl(imageUrl);
                                    editRecipeInstructionsAdapter.notifyItemChanged(position);
                                    Log.d(TAG, "onSuccess: position" + position + instructionList.get(position).toString());
                                    pd.hide();
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onRemoveIngredientClick(int position) {
        ingredientList.remove(position);
        editRecipeIngredientsAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onRemoveStepClick(int position) {
        instructionList.remove(position);
        editRecipeInstructionsAdapter.notifyItemRemoved(position);
    }


}
