package com.rokudoz.irecipe.Fragments;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Fragments.profileSubFragments.profileMyFriendList;
import com.rokudoz.irecipe.Fragments.profileSubFragments.profileMyIngredientsFragment;
import com.rokudoz.irecipe.Fragments.profileSubFragments.profileMyRecipesFragment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.Adapters.SectionsPagerAdapter;
import com.rokudoz.irecipe.Utils.RotateBitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    private MaterialButton mSignOutBtn, mEditProfileBtn, mAddPhotoBtn;
    private RelativeLayout userDetailsLayout;

    private ViewPager viewPager;

    private String userDocumentID = "";
    private String userProfilePicUrl = "";

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
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);

        UserNameTv = view.findViewById(R.id.profileFragment_user_name_TextView);
        UserUsernameTv = view.findViewById(R.id.profileFragment_userName_TextView);
        UserDescriptionTv = view.findViewById(R.id.profileFragment_user_description_TextView);
        mProfileImage = view.findViewById(R.id.profileFragment_profileImage);
        mSignOutBtn = view.findViewById(R.id.profileFragment_signOut_materialButton);
        mEditProfileBtn = view.findViewById(R.id.profileFragment_editProfile_materialButton);
        mAddPhotoBtn = view.findViewById(R.id.profileFragment_changePic_Btn);
        userDetailsLayout = view.findViewById(R.id.profileFragment_userDetailsLayout);
        //Tab layout
        viewPager = view.findViewById(R.id.profileFragment_container);
        setupViewPager(viewPager);
        TabLayout tabLayout = view.findViewById(R.id.profileFragment_tabLayout);
        tabLayout.setupWithViewPager(viewPager);


        getUserInfo();

        mSignOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usersReference.document(userDocumentID).update("user_tokenID","").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: updated token to null");
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });

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

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new profileMyIngredientsFragment(), "Fridge");
        adapter.addFragment(new profileMyRecipesFragment(), "My recipes");
        adapter.addFragment(new profileMyFriendList(), "Friend List");
        viewPager.setAdapter(adapter);
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
                }
            }
        });
    }
}
