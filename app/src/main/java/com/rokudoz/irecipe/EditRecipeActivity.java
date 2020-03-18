package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity implements EditRecipeInstructionsAdapter.OnItemClickListener {
    private static final String TAG = "EditRecipeActivity";

    //Components
    ImageView recipeImageView;
    MaterialButton recipePhotoBtn, addIngredientBtn, addInstructionBtn, saveBtn;
    TextInputEditText titleInputEditText, descriptionInputEditText, keywordsInputEditText;
    Spinner categorySpinner, privacySpinner;
    RecyclerView ingredientsRecyclerView, instructionsRecyclerView;
    EditText durationEditText;
    AppCompatSpinner durationTypeSpinner, complexitySpinner;


    EditRecipeIngredientsAdapter editRecipeIngredientsAdapter;
    EditRecipeInstructionsAdapter editRecipeInstructionsAdapter;


    List<Instruction> instructionList = new ArrayList<>();
    List<Ingredient> ingredientList = new ArrayList<>();

    String recipe_id = "";

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;
    Bitmap imageBitmap;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference recipesReference = db.collection("Recipes");
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


        if (getIntent() != null && getIntent().getStringExtra("recipe_id") != null) {
            recipe_id = getIntent().getStringExtra("recipe_id");
            if (recipe_id != null) {
                getRecipeDetails();
            }
        }
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG, "onClick: " + editRecipeInstructionsAdapter.getInstructionList().toString());
                Log.d(TAG, "onClick: " + instructionList.toString());
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
        buildRecyclerViews();

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

    private void getRecipeDetails() {
        recipesReference.document(recipe_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                    if (recipe != null) {
                        Glide.with(EditRecipeActivity.this).load(recipe.getImageUrls_list().get(0)).centerCrop().into(recipeImageView);

                        titleInputEditText.setText(recipe.getTitle());
                        descriptionInputEditText.setText(recipe.getDescription());
                        durationEditText.setText(recipe.getDuration().toString());
                        keywordsInputEditText.setText(recipe.getKeywords().toString());

                        for (Instruction instruction : recipe.getInstruction_list()) {
                            instructionList.add(instruction);
                            editRecipeInstructionsAdapter.notifyItemInserted(instructionList.indexOf(instruction));
                        }
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
