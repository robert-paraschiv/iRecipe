package com.rokudoz.irecipe.Fragments.profileSubFragments;


import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.provider.MediaStore;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class profileEditProfile extends Fragment {

    private static final String TAG = "profileEditProfile";
    private static final int PICK_IMAGE_REQUEST = 1;
    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("UsersPhotos");
    private StorageTask mUploadTask;

    private Uri mImageUri;
    private String userProfilePicUrl = "";
    private String userDocumentID = "";

    private EditText user_name_TextInput, userName_TextInput, description_TextInput, email_TextInput, nationality_TextInput;
    private Spinner gender_Spinner;
    private ImageView profilePicture_ImageView;
    private MaterialButton save_MaterialButton, changeProfilePic_MaterialButton;

    private View view;

    private User mUser;

    public profileEditProfile() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_profile_edit_profile, container, false);
        } catch (InflateException e) {
            Log.e(TAG, "onCreateView: ", e);
        }

        user_name_TextInput = view.findViewById(R.id.profileFragmentEditProfile_user_name_TextInput);
        userName_TextInput = view.findViewById(R.id.profileFragmentEditProfile_username_TextInput);
        description_TextInput = view.findViewById(R.id.profileFragmentEditProfile_user_description_TextInput);
        email_TextInput = view.findViewById(R.id.profileFragmentEditProfile_email_TextInput);
        nationality_TextInput = view.findViewById(R.id.profileFragmentEditProfile_user_nationality_TextInput);
        gender_Spinner = view.findViewById(R.id.profileFragmentEditProfile_gender_spinner);
        profilePicture_ImageView = view.findViewById(R.id.profileFragmentEditProfile_userProfilePic_ImageView);
        save_MaterialButton = view.findViewById(R.id.profileFragmentEditProfile_save_MaterialButton);
        changeProfilePic_MaterialButton = view.findViewById(R.id.profileFragmentEditProfile_changeProfilePic_MaterialButton);

        mUser = new User();
        getCurrentUserDetails();

        changeProfilePic_MaterialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(getContext(), "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    openFileChooser();
                }
            }
        });
        save_MaterialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDetails();
            }
        });

        return view;
    }

    private void saveUserDetails() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String name = user_name_TextInput.getText().toString();
        String userName = userName_TextInput.getText().toString();
        String email = email_TextInput.getText().toString();
        String description = description_TextInput.getText().toString();
        String nationality = nationality_TextInput.getText().toString();
        String gender = gender_Spinner.getSelectedItem().toString();

        if (name.equals("") || userName.equals("") || email.equals("") || description.equals("") || nationality.equals("") || gender.equals("")) {
            Toast.makeText(getContext(), "Please make sure you've filled your info", Toast.LENGTH_SHORT).show();
        } else {

            final User user = new User(mUser.getUser_id(), name, userName, email, description, gender, nationality, mUser.getUserProfilePicUrl(), mUser.getFavoriteRecipes());

            usersReference.document(userId).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: Updated user");
                    Toast.makeText(getContext(), "Updated your info", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigate(profileEditProfileDirections.actionProfileEditProfileToProfileFragment());
                }
            });
        }
    }

    private void getCurrentUserDetails() {
        usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null) {
                    mUser = documentSnapshot.toObject(User.class);

                    userProfilePicUrl = mUser.getUserProfilePicUrl();
                    userDocumentID = mUser.getUser_id();
                    if (userProfilePicUrl != null && !mUser.getUserProfilePicUrl().equals("")) {
                        Picasso.get()
                                .load(mUser.getUserProfilePicUrl())
                                .error(R.drawable.ic_account_circle_black_24dp)
                                .fit()
                                .centerCrop()
                                .into(profilePicture_ImageView);

                        changeProfilePic_MaterialButton.setText("Change profile picture");
                    } else {
                        Picasso.get()
                                .load(R.drawable.ic_account_circle_black_24dp)
                                .placeholder(R.drawable.ic_account_circle_black_24dp)
                                .into(profilePicture_ImageView);

                        changeProfilePic_MaterialButton.setText("Add profile picture");

                        Toast.makeText(getContext(), "empty", Toast.LENGTH_SHORT).show();
                    }

                    user_name_TextInput.setText(mUser.getName());
                    userName_TextInput.setText(mUser.getUsername());
                    email_TextInput.setText(mUser.getEmail());
                    description_TextInput.setText(mUser.getDescription());
                    nationality_TextInput.setText(mUser.getNationality());
                    if (mUser.getGender() != null) {
                        if (mUser.getGender().equals("Male"))
                            gender_Spinner.setSelection(0);
                        if (mUser.getGender().equals("Female"))
                            gender_Spinner.setSelection(1);
                    }
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

            //Compress Image
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), mImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
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

}