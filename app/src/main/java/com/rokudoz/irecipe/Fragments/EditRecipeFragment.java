package com.rokudoz.irecipe.Fragments;


import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditRecipeFragment extends Fragment {
    private static final String TAG = "EditRecipeFragment";

    private String documentID;
    private String loggedInUserDocumentId;
    private View view;

    private Recipe mCurrentRecipe = new Recipe();

    //FireBase refs
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recipesReference = db.collection("Recipes");
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");

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
    private List<Ingredient> recipeIngredientList = new ArrayList<>();

    private List<Ingredient> allIngredientsList;

    //Instructions
    private LinearLayout instructionsLinearLayout;
    private ArrayList<EditText> instructionTextEtList;
    private Uri mInstructionStepUri;
    private List<Uri> mInstructionStepImageUriList;
    private String[] instructionStepImageUrlArray;
    private List<ImageView> instructionStepImageViewList;
    private List<Instruction> recipeInstructionsList = new ArrayList<>();

    private TextInputEditText editTextTitle, editTextDescription, editTextKeywords;
    private Spinner recipeCategorySpinner, privacySpinner;
    private ImageView mImageView;
    private ProgressBar mProgressBar;


    public EditRecipeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_edit_recipe, container, false);


        EditRecipeFragmentArgs editRecipeFragmentArgs = EditRecipeFragmentArgs.fromBundle(getArguments());
        documentID = editRecipeFragmentArgs.getDocumentID();
        loggedInUserDocumentId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        ingredientsLinearLayout = view.findViewById(R.id.editRecipe_ingredients_linear_layout);
        instructionsLinearLayout = view.findViewById(R.id.editRecipe_instructions_linear_layout);

        editTextTitle = view.findViewById(R.id.editRecipe_title_editText);
        editTextDescription = view.findViewById(R.id.editRecipe_description_editText);
        mProgressBar = view.findViewById(R.id.editRecipe_progressbar);
        mImageView = view.findViewById(R.id.editRecipe_image);
        Button mChooseFileBtn = view.findViewById(R.id.editRecipe_choose_path_btn);
        Button mPostRecipeBtn = view.findViewById(R.id.editRecipe_add_btn);
        editTextKeywords = view.findViewById(R.id.editRecipe_keywords_editText);
        Button mAddIngredientBtn = view.findViewById(R.id.editRecipe_addIngredient_btn);
        Button mAddInstructionBtn = view.findViewById(R.id.editRecipe_addInstruction_btn);
        recipeCategorySpinner = view.findViewById(R.id.editRecipe_category_spinner);
        privacySpinner = view.findViewById(R.id.editRecipe_privacy_spinner);
        ingredientNameEtList = new ArrayList<>();
        ingredientQuantityEtList = new ArrayList<>();
        ingredientQuantityTypeSpinnerList = new ArrayList<>();

        instructionTextEtList = new ArrayList<>();
        mInstructionStepImageUriList = new ArrayList<>();
        instructionStepImageViewList = new ArrayList<>();


        allIngredientsList = new ArrayList<>();

        mRecipeImageUriArray = new Uri[0];

        getCurrentRecipe();


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
//                openFileChooser(RECIPE_PICTURE);
            }
        });
        mPostRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(getActivity(), "Upload in progress...", Toast.LENGTH_SHORT).show();

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


        //
        return view;
    }

    private void getCurrentRecipe() {
        recipesReference.document(documentID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mCurrentRecipe = documentSnapshot.toObject(Recipe.class);

                editTextTitle.setText(mCurrentRecipe.getTitle());
                if (mCurrentRecipe.getCategory().equals("breakfast")) {
                    recipeCategorySpinner.setSelection(0);
                } else if (mCurrentRecipe.getCategory().equals("lunch")) {
                    recipeCategorySpinner.setSelection(1);
                } else if (mCurrentRecipe.getCategory().equals("dinner")) {
                    recipeCategorySpinner.setSelection(2);
                }
                editTextDescription.setText(mCurrentRecipe.getDescription());
                List<String> keywords = mCurrentRecipe.getKeywords();
                StringBuilder keywordsString = new StringBuilder();
                for (String keyword : keywords) {
                    keywordsString.append(keyword).append(",");
                }
                editTextKeywords.setText(keywordsString);
                if (mCurrentRecipe.getPrivacy().equals("Everyone")) {
                    privacySpinner.setSelection(0);
                } else {
                    privacySpinner.setSelection(1);
                }
                Picasso.get().load(mCurrentRecipe.getImageUrls_list().get(0)).fit().centerCrop().into(mImageView);

                setUpRecipeCategorySpinner();
                getRecipeIngredients();

            }
        });
    }

    private void getRecipeIngredients() {
        recipesReference.document(documentID).collection("RecipeIngredients").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Ingredient ingredient = documentSnapshot.toObject(Ingredient.class);
                    ingredient.setDocumentId(documentSnapshot.getId());
                    if (!recipeIngredientList.contains(ingredient))
                        recipeIngredientList.add(ingredient);
                }

                setUpInitialRecipeIngredients(recipeIngredientList);
                getRecipeInstructions();
            }
        });


    }

    private void getRecipeInstructions() {
        recipesReference.document(documentID).collection("RecipeInstructions").orderBy("stepNumber").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Instruction instruction = documentSnapshot.toObject(Instruction.class);
                    if (!recipeInstructionsList.contains(instruction))
                        recipeInstructionsList.add(instruction);
                }
                setUpInitialRecipeInstructions(recipeInstructionsList);
            }
        });
    }

    private void setUpInitialRecipeIngredients(List<Ingredient> recipeIngredientList) {
        for (Ingredient ingredient : recipeIngredientList) {
            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ingredientsLinearLayout.addView(linearLayout);

            final EditText editText = new EditText(getActivity());
            editText.setHint("Ingredient ");
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            setEditTextAttributes(editText);
            editText.setText(ingredient.getName());
            linearLayout.addView(editText);
            ingredientNameEtList.add(editText);

            final EditText editText2 = new EditText(getActivity());
            editText2.setHint("Quantity ");
            editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText2.setText(ingredient.getQuantity().toString());
            setEditTextAttributes(editText2);
            linearLayout.addView(editText2);
            ingredientQuantityEtList.add(editText2);

            final Spinner spinner = new Spinner(getActivity());
            String[] spinnerItems = {"g", "kg"};
            if (ingredient.getQuantity_type().equals("g")) {
                spinner.setSelection(0);
            } else {
                spinner.setSelection(1);
            }
            linearLayout.addView(spinner);
            setUpIngredientQuantitySpinner(spinner, spinnerItems);
            ingredientQuantityTypeSpinnerList.add(spinner);

            final MaterialButton button = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
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

    }

    private void setUpInitialRecipeInstructions(List<Instruction> recipeInstructionsList) {
        for (Instruction instruction : recipeInstructionsList) {
            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            instructionsLinearLayout.addView(linearLayout);
            linearLayout.setLayoutTransition(new LayoutTransition());
            Uri uri;
            if (instruction.getImgUrl().equals("")) {
                uri = Uri.parse("");
            } else {
                uri = Uri.parse(instruction.getImgUrl());
            }
            mInstructionStepImageUriList.add(uri);

//            Log.d(TAG, "addInstructionLayout: " + uri.toString());

            final EditText editText = new EditText(getActivity());
            editText.setHint("Step Instructions ");
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            setEditTextAttributes(editText);
            editText.setText(instruction.getText());
            linearLayout.addView(editText);
            instructionTextEtList.add(editText);

            final ImageView imageView = new ImageView(getActivity());
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

            final MaterialButton button = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
            button.setText("Add photo to this step");
            button.setCornerRadius(convertDpToPixel(18));
            setButtonAttributes(button);
            linearLayout.addView(button);
            if (instruction.getImgUrl().equals("")) {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                    openFileChooser(INSTRUCTION_PICTURE + instructionTextEtList.indexOf(editText));
                        button.setVisibility(View.GONE);
                        imageView.setVisibility(View.VISIBLE);
                        instructionStepImageViewList.add(imageView);
                    }
                });
            } else {
                instructionStepImageViewList.add(imageView);
                button.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                Picasso.get().load(instruction.getImgUrl()).fit().centerCrop().into(imageView);
            }

            final MaterialButton removeStepButton = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
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
        Log.d(TAG, "setUpInitialRecipeInstructions: " + mInstructionStepImageUriList);

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

//                    getRecipePhotoUploadCount();
                }

            }
        });
    }


    private void addIngredientLayout() {
        final LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ingredientsLinearLayout.addView(linearLayout);

        final EditText editText = new EditText(getActivity());
        editText.setHint("Ingredient ");
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        setEditTextAttributes(editText);
        linearLayout.addView(editText);
        ingredientNameEtList.add(editText);

        final EditText editText2 = new EditText(getActivity());
        editText2.setHint("Quantity ");
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        setEditTextAttributes(editText2);
        linearLayout.addView(editText2);
        ingredientQuantityEtList.add(editText2);

        final Spinner spinner = new Spinner(getActivity());
        String[] spinnerItems = {"g", "kg"};
        linearLayout.addView(spinner);
        setUpIngredientQuantitySpinner(spinner, spinnerItems);
        ingredientQuantityTypeSpinnerList.add(spinner);

        final MaterialButton button = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
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
        final LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        instructionsLinearLayout.addView(linearLayout);
        linearLayout.setLayoutTransition(new LayoutTransition());
        final Uri uri = Uri.parse("");
        mInstructionStepImageUriList.add(uri);
        Log.d(TAG, "addInstructionLayout: " + uri.toString());

        final EditText editText = new EditText(getActivity());
        editText.setHint("Step Instructions ");
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        setEditTextAttributes(editText);
        linearLayout.addView(editText);
        instructionTextEtList.add(editText);

        final ImageView imageView = new ImageView(getActivity());
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

        final MaterialButton button = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
        button.setText("Add photo to this step");
        button.setCornerRadius(convertDpToPixel(18));
        setButtonAttributes(button);
        linearLayout.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                openFileChooser(INSTRUCTION_PICTURE + instructionTextEtList.indexOf(editText));
                button.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                instructionStepImageViewList.add(imageView);
            }
        });

        final MaterialButton removeStepButton = new MaterialButton(getActivity(), null, R.attr.materialButtonOutlinedStyle);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        recipeCategorySpinner.setAdapter(adapter);
    }

    private void setUpIngredientQuantitySpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
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
}
