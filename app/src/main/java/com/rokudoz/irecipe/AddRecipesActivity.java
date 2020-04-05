package com.rokudoz.irecipe;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
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
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.Adapters.EditRecipe.EditRecipeIngredientsAdapter;
import com.rokudoz.irecipe.Utils.Adapters.EditRecipe.EditRecipeInstructionsAdapter;
import com.rokudoz.irecipe.Utils.RotateBitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class AddRecipesActivity extends AppCompatActivity implements EditRecipeInstructionsAdapter.OnItemClickListener
        , EditRecipeIngredientsAdapter.OnItemClickListener {
    private static final String TAG = "AddRecipesActivity";

    //Components
    ImageView recipeImageView;
    MaterialButton recipePhotoBtn, addIngredientBtn, addInstructionBtn, addRecipeBtn;
    TextInputEditText titleInputEditText, descriptionInputEditText, keywordsInputEditText;
    Spinner categorySpinner, privacySpinner;
    RecyclerView ingredientsRecyclerView, instructionsRecyclerView;
    EditText durationEditText, portionsEditText;
    AppCompatSpinner durationTypeSpinner, complexitySpinner;

    ProgressDialog pd;

    User mUser = new User();

    EditRecipeIngredientsAdapter editRecipeIngredientsAdapter;
    EditRecipeInstructionsAdapter editRecipeInstructionsAdapter;


    List<Instruction> instructionList = new ArrayList<>();
    List<Ingredient> ingredientList = new ArrayList<>();


    private List<Ingredient> allIngredientsList = new ArrayList<>();
    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;

    String recipe_id = "";
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
        setContentView(R.layout.activity_add_recipes);

        pd = new ProgressDialog(AddRecipesActivity.this);
        pd.setMessage("Please wait...");

        recipeImageView = findViewById(R.id.addRecipes_image);
        recipePhotoBtn = findViewById(R.id.addRecipes_choose_path_btn);
        addIngredientBtn = findViewById(R.id.addRecipes_addIngredient_btn);
        addInstructionBtn = findViewById(R.id.addRecipes_addInstruction_btn);
        addRecipeBtn = findViewById(R.id.addRecipes_add_btn);
        titleInputEditText = findViewById(R.id.addRecipes_title_editText);
        descriptionInputEditText = findViewById(R.id.addRecipes_description_editText);
        keywordsInputEditText = findViewById(R.id.addRecipes_keywords_editText);
        categorySpinner = findViewById(R.id.addRecipes_category_spinner);
        privacySpinner = findViewById(R.id.addRecipes_privacy_spinner);
        ingredientsRecyclerView = findViewById(R.id.addRecipes_ingredients_recyclerview);
        instructionsRecyclerView = findViewById(R.id.addRecipes_instructions_recyclerview);
        durationEditText = findViewById(R.id.addRecipe_duration_editText);
        durationTypeSpinner = findViewById(R.id.addRecipe_durationType_Spinner);
        complexitySpinner = findViewById(R.id.addRecipe_complexity_Spinner);
        portionsEditText = findViewById(R.id.addrecipes_portionsEditText);

        getCurrentUserDetails();

        addRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getIngredientList();
            }
        });
        addInstructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInstruction();
            }
        });
        addIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addIngredient();
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

    private void addIngredient() {
        Ingredient ingredient = new Ingredient();
        ingredient.setName("");
        ingredient.setOwned(false);
        ingredientList.add(ingredient);
        int position = ingredientList.size();
        editRecipeIngredientsAdapter.notifyItemInserted(position);
    }

    private void addInstruction() {
        Instruction instruction = new Instruction();
        instruction.setText("");
        instruction.setStepNumber(instructionList.size() + 1);
        instructionList.add(instruction);
        int position = instructionList.size();
        editRecipeInstructionsAdapter.notifyItemInserted(position);
    }

    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(AddRecipesActivity.this, new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: ", e);
                            return;
                        }
                        if (documentSnapshot != null) {
                            mUser = documentSnapshot.toObject(User.class);
                        }
                    }
                });
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
                            Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

                                addRecipe();
                            }
                        }
                    });
                }
            }
        });
    }

    // Adding Recipes -----------------------------------------------------------------------------
    public void addRecipe() {
        if (recipeImageUrlArray == null) {
            Toast.makeText(this, "Please select a picture for the recipe", Toast.LENGTH_SHORT).show();
            return;
        } else if (Objects.requireNonNull(titleInputEditText.getText()).toString().trim().equals("")) {
            Toast.makeText(this, "Please choose a title for the recipe", Toast.LENGTH_SHORT).show();
            return;
        } else if (Objects.requireNonNull(descriptionInputEditText.getText()).toString().trim().equals("")) {
            Toast.makeText(this, "Please type a description for the recipe", Toast.LENGTH_SHORT).show();
            return;
        } else if (durationEditText.getText().toString().trim().equals("")) {
            Toast.makeText(this, "Please type how much time it takes to cook the recipe", Toast.LENGTH_SHORT).show();
            return;
        } else if (Objects.requireNonNull(keywordsInputEditText.getText()).toString().trim().equals("")) {
            Toast.makeText(this, "Please choose a few keywords for the recipe", Toast.LENGTH_SHORT).show();
            return;
        } else if (portionsEditText.getText().toString().trim().equals("")) {
            Toast.makeText(this, "Please tell us how many portions this recipe is for", Toast.LENGTH_SHORT).show();
            return;
        }

        final String title = titleInputEditText.getText().toString();
        final String description = descriptionInputEditText.getText().toString();
        final String creator_docId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final List<String> imageUrls_list = Arrays.asList(recipeImageUrlArray);

        final String category = categorySpinner.getSelectedItem().toString();
        final List<Ingredient> ingredients_list = new ArrayList<>();
        final List<Ingredient> ingredients_without_category = new ArrayList<>();
        final String privacy = privacySpinner.getSelectedItem().toString();
        final String complexity = complexitySpinner.getSelectedItem().toString();
        Float duration = 0f;
        if (!durationEditText.getText().toString().trim().equals(""))
            duration = Float.parseFloat(durationEditText.getText().toString());
        final String durationType = durationTypeSpinner.getSelectedItem().toString();
        Log.d(TAG, "addRecipe: " + creator_docId);

        final Integer recipePortions = Integer.parseInt(portionsEditText.getText().toString());

        String keywordEt = keywordsInputEditText.getText().toString();
        final List<String> keywords = Arrays.asList(keywordEt.split("\\s*,\\s*"));


        //Get instructions list in order
        for (int i = 0; i < instructionList.size(); i++) {
            if (instructionList.get(i).getText().trim().equals("")) {
                Toast.makeText(this, "Please don't lave any instruction without text", Toast.LENGTH_SHORT).show();
                return;
            }
            instructionList.get(i).setStepNumber(i + 1);
        }

        //Get ingredients list items from edit texts
        for (Ingredient ingredient : ingredientList) {
            if (ingredient.getName().trim().equals("")) {
                Toast.makeText(this, "Please don't lave any ingredient without name", Toast.LENGTH_SHORT).show();
                return;
            }
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

        // If there are any ingredients without category in db, ask the user
        if (ingredients_without_category.size() > 0) {

            final View rootView = LayoutInflater.from(this).inflate(R.layout.dialog_add_recipe_ingredients_unknown, null);
            //Create a dialog to ask the user the category of the ingredient if there is one that isn't in db
            final LinearLayout linearLayout = rootView.findViewById(R.id.dialog_add_recipes_linearLayout);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final List<Spinner> category_spinner_list = new ArrayList<>();
            //create an adapter to describe how the items are displayed, adapters are used in several places in android.
            //There are multiple variations of this, but this is the basic variant.
            ArrayAdapter<String> adapter = new ArrayAdapter<>(AddRecipesActivity.this, android.R.layout.simple_spinner_dropdown_item, categories);
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
                Spinner spinner = new Spinner(AddRecipesActivity.this);
                TextView textView = new TextView(AddRecipesActivity.this);
                spinner.setLayoutParams(params);
                spinner.setAdapter(adapter);
                textView.setText(ingredient.getName());
                textView.setLayoutParams(params);
                category_spinner_list.add(spinner);
                linearLayout.addView(textView);
                linearLayout.addView(spinner);
            }

            MaterialButton materialButton = rootView.findViewById(R.id.dialog_add_recipes_continueBtn);

            final Float finalDuration1 = duration;


            final Dialog materialAlertDialogBuilder = new Dialog(AddRecipesActivity.this
                    , R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
            materialAlertDialogBuilder.setContentView(rootView);
            materialAlertDialogBuilder.setCancelable(true);
            materialButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

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

                    Recipe recipe = new Recipe(title, creator_docId, mUser.getName(), mUser.getUserProfilePicUrl(), category, description, recipePortions
                            , ingredients_list, instructionList, keywords, imageUrls_list, complexity, finalDuration1, durationType
                            , 0f, false, privacy, 0, 0, null);

                    pd.show();
                    // Upload recipe to db
                    recipesReference.add(recipe).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            pd.hide();
                            Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(AddRecipesActivity.this, MainActivity.class);
                            intent.putExtra("recipe_id", documentReference.getId());
                            startActivity(intent);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    materialAlertDialogBuilder.cancel();
                }
            });
            materialAlertDialogBuilder.show();
        } else {
            // All the ingredients are in db with category, proceed to upload
            pd.show();

            Recipe recipe = new Recipe(title, creator_docId, mUser.getName(), mUser.getUserProfilePicUrl(), category, description, recipePortions
                    , ingredients_list, instructionList, keywords, imageUrls_list, complexity, duration, durationType
                    , 0f, false, privacy, 0, 0, null);

            // Upload recipe to db
            recipesReference.add(recipe).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    pd.hide();
                    Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddRecipesActivity.this, MainActivity.class);
                    intent.putExtra("recipe_id", documentReference.getId());
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

        editRecipeInstructionsAdapter.setOnItemClickListener(AddRecipesActivity.this);
        editRecipeIngredientsAdapter.setOnItemClickListener(AddRecipesActivity.this);

//        addInstruction();
//        addIngredient();
        addInstructionTest("eeeeee");
        addIngredientTest("a");
        addIngredientTest("b");
        addIngredientTest("c");
        addIngredientTest("d");
        addIngredientTest("e");
        addIngredientTest("f");
        addIngredientTest("g");
        addIngredientTest("h");
        addIngredientTest("i");
    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
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
                            Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            Toast.makeText(AddRecipesActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void addIngredientTest(String name) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setQuantity(3f);
        ingredient.setOwned(false);
        ingredientList.add(ingredient);
        int position = ingredientList.size();
        editRecipeIngredientsAdapter.notifyItemInserted(position);
    }

    private void addInstructionTest(String text) {
        Instruction instruction = new Instruction();
        instruction.setText(text);
        instruction.setStepNumber(instructionList.size() + 1);
        instructionList.add(instruction);
        int position = instructionList.size();
        editRecipeInstructionsAdapter.notifyItemInserted(position);
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