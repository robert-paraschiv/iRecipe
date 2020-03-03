package com.rokudoz.irecipe;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Utils.RotateBitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddRecipesActivity extends AppCompatActivity {

    private static final String TAG = "AddRecipesActivity";

    //FireBase refs
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesReference = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");

    private User mUser = new User();

    private static final int SELECT_PICTURES = 1;
    private static final int RECIPE_PICTURE = 0;
    private static final int INSTRUCTION_PICTURE = 1;

    private int nrOfPhotosUploaded = 0;
    private Uri mRecipeImageUri;

    private Uri[] mRecipeImageUriArray;
    private String[] recipeImageUrlArray;
    private StorageTask mUploadTask;

    //Ingredient
    private LinearLayout ingredientsLinearLayout;
    private ArrayList<EditText> ingredientNameEtList;
    private ArrayList<EditText> ingredientQuantityEtList;
    private ArrayList<Spinner> ingredientQuantityTypeSpinnerList;
    private List<Ingredient> allIngredientsList;
    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;
    //Instruction
    private LinearLayout instructionsLinearLayout;
    private ArrayList<EditText> instructionTextEtList;
    private Uri mInstructionStepUri;
    private List<Uri> mInstructionStepImageUriList;
    private String[] instructionStepImageUrlArray;
    private List<ImageView> instructionStepImageViewList;

    private EditText durationEditText;
    private TextInputEditText editTextTitle, editTextDescription, editTextKeywords;
    private Spinner recipeCategorySpinner, privacySpinner, durationTypeSpinner, complexitySpinner;
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipes);

        ingredientsLinearLayout = findViewById(R.id.addRecipes_ingredients_linear_layout);
        instructionsLinearLayout = findViewById(R.id.addRecipes_instructions_linear_layout);

        editTextTitle = findViewById(R.id.addRecipes_title_editText);
        editTextDescription = findViewById(R.id.addRecipes_description_editText);
        mProgressBar = findViewById(R.id.addRecipes_progressbar);
        mImageView = findViewById(R.id.addRecipes_image);
        Button mChooseFileBtn = findViewById(R.id.addRecipes_choose_path_btn);
        Button mPostRecipeBtn = findViewById(R.id.addRecipes_add_btn);
        editTextKeywords = findViewById(R.id.addRecipes_keywords_editText);
        Button mAddIngredientBtn = findViewById(R.id.addRecipes_addIngredient_btn);
        Button mAddInstructionBtn = findViewById(R.id.addRecipes_addInstruction_btn);
        recipeCategorySpinner = findViewById(R.id.addRecipes_category_spinner);
        privacySpinner = findViewById(R.id.addRecipes_privacy_spinner);
        durationEditText = findViewById(R.id.addRecipe_duration_editText);
        durationTypeSpinner = findViewById(R.id.addRecipe_durationType_Spinner);
        complexitySpinner = findViewById(R.id.addRecipe_complexity_Spinner);
        ingredientNameEtList = new ArrayList<>();
        ingredientQuantityEtList = new ArrayList<>();
        ingredientQuantityTypeSpinnerList = new ArrayList<>();

        instructionTextEtList = new ArrayList<>();
        mInstructionStepImageUriList = new ArrayList<>();
        instructionStepImageViewList = new ArrayList<>();


        allIngredientsList = new ArrayList<>();

        mRecipeImageUriArray = new Uri[0];

        setUpRecipeCategorySpinner();
        addIngredientLayout();
        addInstructionLayout();

        mAddInstructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                openFileChooser(INSTRUCTION_PICTURE);
                addInstructionLayout();
            }
        });
        mChooseFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(RECIPE_PICTURE);
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
                addIngredientLayout();
            }
        });

        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e == null && documentSnapshot != null) {
                    mUser = documentSnapshot.toObject(User.class);
                }
            }
        });
    }


    private void openFileChooser(Integer functionThatCalled) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select picture"), SELECT_PICTURES + functionThatCalled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            Log.d(TAG, "onActivityResult: RecipePic");
        } else if (requestCode == 2) {
            Log.d(TAG, "onActivityResult: InstructionPic");
        }

        if (requestCode == SELECT_PICTURES && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                //Multiple images have been selected

                int count = data.getClipData().getItemCount();
                mRecipeImageUriArray = new Uri[count];

                for (int i = 0; i < count; i++) {
                    mRecipeImageUriArray[i] = data.getClipData().getItemAt(i).getUri();
                    mRecipeImageUri = data.getClipData().getItemAt(i).getUri();
                }
                Glide.with(mImageView).load(mRecipeImageUri).centerCrop().into(mImageView);

            } else if (data.getData() != null) {
                //Only one image has been selected
                mRecipeImageUri = data.getData();
                mRecipeImageUriArray = new Uri[1];
                mRecipeImageUriArray[0] = mRecipeImageUri;
                Glide.with(mImageView).load(mRecipeImageUri).centerCrop().into(mImageView);
            }

        } else if (requestCode >= SELECT_PICTURES + INSTRUCTION_PICTURE && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {

                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    mInstructionStepUri = data.getClipData().getItemAt(i).getUri();
                }
                Glide.with(getApplication()).load(mInstructionStepImageUriList.get(requestCode - (SELECT_PICTURES + INSTRUCTION_PICTURE))).centerCrop()
                        .into(instructionStepImageViewList.get(instructionStepImageViewList.size() - 1));

            } else if (data.getData() != null) {
                //Only one image has been selected
                mInstructionStepUri = data.getData();
                mInstructionStepImageUriList.set(requestCode - (SELECT_PICTURES + INSTRUCTION_PICTURE), mInstructionStepUri);

                Glide.with(getApplication()).load(mInstructionStepImageUriList.get(requestCode - (SELECT_PICTURES + INSTRUCTION_PICTURE))).centerCrop()
                        .into(instructionStepImageViewList.get(instructionStepImageViewList.size() - 1));
            }
        }
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
                    ingredientsReference.document("ingredient_categories").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            if (e == null && documentSnapshot != null) {
                                ingredient_categories = (List<String>) documentSnapshot.get("categories");
                                Log.d(TAG, "onEvent: " + ingredient_categories);
                                categories = ingredient_categories.toArray(new String[0]);

                                getRecipePhotoUploadCount();
                            }
                        }
                    });
                }
            }
        });
    }

    private void addStepPhotoToFireStore(Uri uri, final int position) {
        if (mInstructionStepImageUriList != null) {
            if (mInstructionStepImageUriList.get(position) != null && !mInstructionStepImageUriList.get(position).toString().equals("")) {
                final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                        + "." + getFileExtension(mInstructionStepImageUriList.get(position)));

                //Compress Image
                Bitmap bitmap = null;
                try {
                    RotateBitmap rotateBitmap = new RotateBitmap();
                    bitmap = rotateBitmap.HandleSamplingAndRotationBitmap(this, mInstructionStepImageUriList.get(position));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
                byte[] data = baos.toByteArray();

                fileReference.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String imageUrl = uri.toString();
                                instructionStepImageUrlArray[position] = imageUrl;
                                nrOfPhotosUploaded++;
                                if (nrOfPhotosUploaded == mRecipeImageUriArray.length + mInstructionStepImageUriList.size())
                                    addRecipe();
                            }
                        });
                    }
                });
            } else {
                final String imageUrl = "";
                instructionStepImageUrlArray[position] = imageUrl;
                nrOfPhotosUploaded++;
                if (nrOfPhotosUploaded == mRecipeImageUriArray.length + mInstructionStepImageUriList.size())
                    addRecipe();
            }

        }
    }

    private void addPhotoToFirestore(Uri uri, final int position) {
        // Uploading image to Firestore
        if (mRecipeImageUriArray != null) {
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 35, baos);
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
                                    nrOfPhotosUploaded++;

                                    if (nrOfPhotosUploaded == mRecipeImageUriArray.length) {
                                        getInstructionsPhotoUploadCount();
//                                        addRecipe();
                                    }

                                }
                            });

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(0);
                                }
                            }, 50);

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
        final String creator_docId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final List<String> imageUrls_list = Arrays.asList(recipeImageUrlArray);
        final String category = recipeCategorySpinner.getSelectedItem().toString();
        final List<Ingredient> ingredients_list = new ArrayList<>();
        final List<Ingredient> ingredients_without_category = new ArrayList<>();
        final List<Instruction> instructions_list = new ArrayList<>();
        final Boolean isFavorite = false;
        final String privacy = privacySpinner.getSelectedItem().toString();
        final String complexity = complexitySpinner.getSelectedItem().toString();
        Float duration = 0f;
        if (!durationEditText.getText().toString().trim().equals(""))
            duration = Float.parseFloat(durationEditText.getText().toString());
        final String durationType = durationTypeSpinner.getSelectedItem().toString();
        Log.d(TAG, "addRecipe: " + creator_docId);


        String keywordEt = editTextKeywords.getText().toString();
        final List<String> keywords = Arrays.asList(keywordEt.split("\\s*,\\s*"));

        //Get ingredients list items from edit texts
        for (int i = 0; i < ingredientNameEtList.size(); i++) {
            if (!ingredientNameEtList.get(i).getText().toString().equals("") && !ingredientQuantityEtList.get(i).getText().toString().equals("")) {
                final Ingredient ingredientToAdd = new Ingredient(ingredientNameEtList.get(i).getText().toString(), ""
                        , Float.parseFloat(ingredientQuantityEtList.get(i).getText().toString())
                        , ingredientQuantityTypeSpinnerList.get(i).getSelectedItem().toString(), false);
                if (allIngredientsList.contains(ingredientToAdd)) {
                    ingredientToAdd.setCategory(allIngredientsList.get(allIngredientsList.indexOf(ingredientToAdd)).getCategory());
                    ingredients_list.add(ingredientToAdd);
                } else {
                    ingredients_without_category.add(ingredientToAdd);
                }
            } else {
                Toast.makeText(this, "Make sure ingredient boxes are not empty", Toast.LENGTH_SHORT).show();
            }

        }

        //

        final LinearLayout linearLayout = new LinearLayout(AddRecipesActivity.this);
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


        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(AddRecipesActivity.this, R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
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

                        //

                        for (int j = 0; j < instructionTextEtList.size(); j++) {
                            String url = "";
                            if (!mInstructionStepImageUriList.get(j).toString().equals("")) {
                                url = instructionStepImageUrlArray[j];
                            }
                            Instruction instruction = new Instruction(j + 1, instructionTextEtList.get(j).getText().toString(), url);
                            instructions_list.add(instruction);
                        }


                        Recipe recipe = new Recipe(title, creator_docId, mUser.getName(), mUser.getUserProfilePicUrl(), category, description, ingredients_list, instructions_list
                                , keywords, imageUrls_list, complexity, finalDuration, durationType, 0f, isFavorite, privacy, 0, 0);

                        Log.d(TAG, "addRecipe: " + recipe.toString());

                        // Sends recipe data to Firestore database
                        recipesReference.add(recipe)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(AddRecipesActivity.this, "Succesfully added " + title + " to the recipes list", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onSuccess: doc id " + documentReference.getId());
                                        Intent intent = new Intent(AddRecipesActivity.this, MainActivity.class);
                                        intent.putExtra("recipe_id", documentReference.getId());
                                        startActivity(intent);
                                        finish();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(AddRecipesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
    }

    private void addIngredientLayout() {
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ingredientsLinearLayout.addView(linearLayout);

        final EditText editText = new EditText(this);
        editText.setHint("Ingredient ");
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        setEditTextAttributes(editText);
        linearLayout.addView(editText);
        ingredientNameEtList.add(editText);

        final EditText editText2 = new EditText(this);
        editText2.setHint("Quantity ");
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        setEditTextAttributes(editText2);
        linearLayout.addView(editText2);
        ingredientQuantityEtList.add(editText2);

        final Spinner spinner = new Spinner(this);
        String[] spinnerItems = {"g", "kg"};
        linearLayout.addView(spinner);
        setUpIngredientQuantitySpinner(spinner, spinnerItems);
        ingredientQuantityTypeSpinnerList.add(spinner);

        final MaterialButton button = new MaterialButton(this, null, R.attr.materialButtonOutlinedStyle);
        button.setText("Remove ingredient");
        button.setCornerRadius(convertDpToPixel(18));
        setButtonAttributes(button);
        linearLayout.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayout.removeAllViews();
                ingredientsLinearLayout.removeView(linearLayout);
                ingredientNameEtList.remove(editText);
                ingredientQuantityEtList.remove(editText2);
                ingredientQuantityTypeSpinnerList.remove(spinner);
            }
        });
    }

    private void addInstructionLayout() {
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        instructionsLinearLayout.addView(linearLayout);
        linearLayout.setLayoutTransition(new LayoutTransition());
        final Uri uri = Uri.parse("");
        mInstructionStepImageUriList.add(uri);
        Log.d(TAG, "addInstructionLayout: " + uri.toString());

        final EditText editText = new EditText(this);
        editText.setHint("Step Instructions ");
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        setEditTextAttributes(editText);
        linearLayout.addView(editText);
        instructionTextEtList.add(editText);

        final ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, convertDpToPixel(100));
        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(params);
        linearLayout.addView(imageView);
        imageView.setVisibility(View.INVISIBLE);

        final MaterialButton button = new MaterialButton(this, null, R.attr.materialButtonOutlinedStyle);
        button.setText("Add photo to this step");
        button.setCornerRadius(convertDpToPixel(18));
        setButtonAttributes(button);
        linearLayout.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(INSTRUCTION_PICTURE + instructionTextEtList.indexOf(editText));
                button.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                instructionStepImageViewList.add(imageView);
            }
        });

        final MaterialButton removeStepButton = new MaterialButton(this, null, R.attr.materialButtonOutlinedStyle);
        removeStepButton.setText("Remove Step");
        removeStepButton.setCornerRadius(convertDpToPixel(18));
        setButtonAttributes(removeStepButton);
        linearLayout.addView(removeStepButton);
        removeStepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayout.removeAllViews();
                instructionsLinearLayout.removeView(linearLayout);
                mInstructionStepImageUriList.remove(instructionTextEtList.indexOf(editText));
                if (instructionStepImageViewList.contains(imageView)) {
                    instructionStepImageViewList.remove(imageView);
                }
                instructionTextEtList.remove(editText);
                instructionsLinearLayout.removeView(linearLayout);
            }
        });

    }


    private void setUpRecipeCategorySpinner() {
        String[] items = new String[]{"breakfast", "lunch", "dinner"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        recipeCategorySpinner.setAdapter(adapter);
    }

    private void setUpIngredientQuantitySpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );
        spinner.setLayoutParams(params);
        spinner.setAdapter(adapter);
    }

    private void setEditTextAttributes(EditText editText) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );

        editText.setLayoutParams(params);
    }

    private void setButtonAttributes(MaterialButton button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );

        button.setLayoutParams(params);
    }

    //This function to convert DPs to pixels
    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    private void getRecipePhotoUploadCount() {
        if (mRecipeImageUriArray.length == 0) {
            Toast.makeText(this, "Make sure to select a photo for the recipe", Toast.LENGTH_SHORT).show();
        }
        //Getting uploaded to FireStore
        recipeImageUrlArray = new String[mRecipeImageUriArray.length];
        for (int i = 0; i < mRecipeImageUriArray.length; i++) {
            addPhotoToFirestore(mRecipeImageUriArray[i], i);
        }
    }

    private void getInstructionsPhotoUploadCount() {
        //Getting uploaded to FireStore
        instructionStepImageUrlArray = new String[mInstructionStepImageUriList.size()];
        for (int j = 0; j < mInstructionStepImageUriList.size(); j++) {
            addStepPhotoToFireStore(mInstructionStepImageUriList.get(j), j);
        }
    }


//  ----------------------------------------------------------------------------------------------

}
