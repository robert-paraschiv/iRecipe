package com.rokudoz.irecipe.Fragments.profileSubFragments;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.AddRecipesActivity;
import com.rokudoz.irecipe.Fragments.profileSubFragments.ProfileFragmentDirections;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.SectionsPagerAdapter;
import com.rokudoz.irecipe.Utils.RotateBitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;
    private Bitmap imageBitmap;
    private TextView UserNameTv;
    private TextView UserUsernameTv;
    private TextView UserDescriptionTv;
    private CircleImageView mProfileImage;
    private MaterialButton mSettingsBtn, mEditProfileBtn, mAddPhotoBtn;
    private RelativeLayout userDetailsLayout;
    private View view;
    private ViewPager viewPager;

    private String userDocumentID = "";
    private String userProfilePicUrl = "";
    private List<String> ingredient_categories = new ArrayList<>();
    private String[] categories;
    private List<Ingredient> userIngredientList = new ArrayList<>();

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("UsersPhotos");
    private ListenerRegistration userDetailsListener;
    private StorageTask mUploadTask;


    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        UserNameTv = view.findViewById(R.id.profileFragment_user_name_TextView);
        UserUsernameTv = view.findViewById(R.id.profileFragment_userName_TextView);
        UserDescriptionTv = view.findViewById(R.id.profileFragment_user_description_TextView);
        mProfileImage = view.findViewById(R.id.profileFragment_profileImage);
        mSettingsBtn = view.findViewById(R.id.profileFragment_settings_materialButton);
        mEditProfileBtn = view.findViewById(R.id.profileFragment_editProfile_materialButton);
        mAddPhotoBtn = view.findViewById(R.id.profileFragment_changePic_Btn);
        userDetailsLayout = view.findViewById(R.id.profileFragment_userDetailsLayout);
        //Tab layout
        viewPager = view.findViewById(R.id.profileFragment_container);
        setupViewPager(viewPager);
        TabLayout tabLayout = view.findViewById(R.id.profileFragment_tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        if (getActivity() != null) {
            BottomNavigationView navBar = getActivity().findViewById(R.id.bottom_navigation);
            navBar.setVisibility(View.VISIBLE);
        }
        getUserInfo();

        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToSettingsFragment());
            }
        });
        userDetailsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToProfileEditProfile());
            }
        });
        mEditProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(ProfileFragmentDirections.actionProfileFragmentToProfileEditProfile());
            }
        });
        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(getContext(), "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    openFileChooser();
                }
            }
        });
        mAddPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(getContext(), "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    openFileChooser();
                }
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        DetachFireStoreListeners();
    }

    private void DetachFireStoreListeners() {
        if (userDetailsListener != null) {
            userDetailsListener.remove();
            userDetailsListener = null;
        }
    }

    public void navigateToAddRecipe() {
        Intent intent = new Intent(getContext(), AddRecipesActivity.class);
        startActivity(intent);
    }

    private void addIngredientManually() {
        //Button to add ingredient manually
        final LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText input = new EditText(getActivity());
        final Spinner spinner = new Spinner(getActivity());
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getActivity()), android.R.layout.simple_spinner_dropdown_item, categories);
        //set the spinners adapter to the previously created one.
        spinner.setAdapter(adapter);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
        linearLayout.addView(input);
        linearLayout.addView(spinner);
        materialAlertDialogBuilder.setView(linearLayout);
        materialAlertDialogBuilder.setMessage("Add ingredient to list");
        materialAlertDialogBuilder.setCancelable(true);
        materialAlertDialogBuilder.setPositiveButton(
                "Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //
                        Ingredient ingredient = new Ingredient(input.getText().toString(), spinner.getSelectedItem().toString(), 0f, "g", true);

                        if (input.getText().toString().trim().equals("")) {
                            Toast.makeText(getActivity(), "You need to write the name of what you want to add to the list", Toast.LENGTH_SHORT).show();
                        } else {
                            if (userIngredientList.contains(ingredient) && userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                                Toast.makeText(getActivity(), "" + input.getText().toString() + " is already in your list", Toast.LENGTH_SHORT).show();
                            } else if (userIngredientList.contains(ingredient) && !userIngredientList.get(userIngredientList.indexOf(ingredient)).getOwned()) {
                                ingredient.setDocumentId(userIngredientList.get(userIngredientList.indexOf(ingredient)).getDocumentId());
                                usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).collection("Ingredients")
                                        .document(ingredient.getDocumentId()).set(ingredient);
                            } else {
                                usersReference.document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).collection("Ingredients")
                                        .add(ingredient).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d(TAG, "onSuccess: added to db");
                                    }
                                });

                            }
                            dialog.cancel();
                        }

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

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new profileMyIngredientsFragment(), "Fridge");
        adapter.addFragment(new profileMyRecipesFragment(), "My recipes");
        adapter.addFragment(new FavoritesFragment(), "Favorites");
        adapter.addFragment(new profileMyFriendList(), "Friend List");
        viewPager.setAdapter(adapter);

        final FloatingActionButton floatingActionButton = view.findViewById(R.id.profileFragment_fab);
        final int[] positionn = {0};

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(final int position) {
                positionn[0] = position;
                if (positionn[0] == 0 || positionn[0] == 1) {
                    floatingActionButton.show();
                } else
                    floatingActionButton.hide();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (positionn[0] == 0 || positionn[0] == 1)
            floatingActionButton.show();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (positionn[0] == 0) {
                    addIngredientManually();
                } else if (positionn[0] == 1) {
                    navigateToAddRecipe();
                } else {
                    floatingActionButton.hide();
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();
            try {
                RotateBitmap rotateBitmap = new RotateBitmap();
                imageBitmap = rotateBitmap.HandleSamplingAndRotationBitmap(getActivity(), mImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            uploadUserProfilePic();
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadUserProfilePic() {
        if (mImageUri != null) {
            final StorageReference newFileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            StorageReference oldPicReference = FirebaseStorage.getInstance().getReference();

            Boolean profilePicNotInFireStore = false;
            if (userProfilePicUrl != null && !userProfilePicUrl.equals("")) {
                if (userProfilePicUrl.contains("googleusercontent.com")) {
                    profilePicNotInFireStore = true;
                } else {
                    oldPicReference = FirebaseStorage.getInstance().getReferenceFromUrl(userProfilePicUrl);

                }

            }
            final StorageReference finalOldPicReference = oldPicReference;
            final Boolean finalProfilePicNotInFireStore = profilePicNotInFireStore;

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
                                    if (userProfilePicUrl != null && !userProfilePicUrl.equals("") && !finalProfilePicNotInFireStore) {
                                        finalOldPicReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getContext(), "deleted oldfile", Toast.LENGTH_SHORT).show();
                                                updateUserProfilePicUrl(imageUrl);
                                            }
                                        });
                                    } else {
                                        updateUserProfilePicUrl(imageUrl);
                                    }

                                }
                            });
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Upload Succesfull", Toast.LENGTH_SHORT).show();
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfilePicUrl(String imageUrl) {
        usersReference.document(userDocumentID).update("userProfilePicUrl", imageUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Updated db with profile pic");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to update db with profile pic");
                    }
                });
    }

    private void getUserInfo() {
        userDetailsListener = usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e == null) {
                    User user = documentSnapshot.toObject(User.class);
                    userDocumentID = documentSnapshot.getId();
                    userProfilePicUrl = user.getUserProfilePicUrl();

                    UserNameTv.setText(user.getName());
                    UserUsernameTv.setText(user.getUsername());
                    UserDescriptionTv.setText(user.getDescription());

                    if (userProfilePicUrl != null && !userProfilePicUrl.equals("")) {
                        Glide.with(mProfileImage).load(userProfilePicUrl).centerCrop().into(mProfileImage);
                    } else {
                        Glide.with(mProfileImage).load(R.drawable.ic_account_circle_black_24dp).centerCrop().into(mProfileImage);
                        Log.d(TAG, "onEvent: Empty profile pic");
                    }
                    //Get categories list
                    ingredientsReference.document("ingredient_categories").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            if (e == null && documentSnapshot != null) {
                                ingredient_categories = (List<String>) documentSnapshot.get("categories");
                                Log.d(TAG, "onEvent: " + ingredient_categories);
                                if (ingredient_categories != null)
                                    categories = ingredient_categories.toArray(new String[0]);
                                getUserIngredientList();
                            }
                        }
                    });
                }
            }
        });
    }

    private void getUserIngredientList() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).collection("Ingredients")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(queryDocumentSnapshots)) {
                                if (!Objects.requireNonNull(queryDocumentSnapshot).getId().equals("ingredient_list")) {
                                    Ingredient ingredient = queryDocumentSnapshot.toObject(Ingredient.class);
                                    ingredient.setDocumentId(queryDocumentSnapshot.getId());
                                    if (!userIngredientList.contains(ingredient)) {
                                        userIngredientList.add(ingredient);
                                    } else if (userIngredientList.contains(ingredient)) {
                                        userIngredientList.set(userIngredientList.indexOf(ingredient), ingredient);
                                    }
                                }
                            }
                        }
                    }
                });
    }
}
