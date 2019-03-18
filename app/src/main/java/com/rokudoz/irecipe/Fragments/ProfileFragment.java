package com.rokudoz.irecipe.Fragments;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Account.LoginActivity;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;
    private TextView textViewData, tvHelloUserName;
    private ProgressBar pbLoading;
    private ListView cbListView;
    private Button signOutBtn;
    private CircleImageView mProfileImage;

    private String userDocumentID = "";
    private String userProfilePicUrl = "";

    private List<String> ingredientList;
    private String[] ingStringArray;

    Map<String, Boolean> ingredientsUserHas = new HashMap<>();

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");
    private CollectionReference ingredientsReference = db.collection("Ingredients");
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("RecipePhotos");
    private StorageTask mUploadTask;


    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewData = view.findViewById(R.id.tv_data);
        cbListView = view.findViewById(R.id.checkable_list);
        pbLoading = view.findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.VISIBLE);
        signOutBtn = view.findViewById(R.id.profileFragment_signOut);
        tvHelloUserName = view.findViewById(R.id.profileFragment_helloUser_textview);
        mProfileImage = view.findViewById(R.id.profileFragment_profileImage);

        setupFirebaseAuth();
        getUserInfo();

        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
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

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
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
            final StorageReference oldPicReference = FirebaseStorage.getInstance().getReferenceFromUrl(userProfilePicUrl);

            mUploadTask = newFileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            newFileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String imageUrl = uri.toString();
                                    if (!userProfilePicUrl.equals("")) {
                                        oldPicReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getContext(), "deleted oldfile", Toast.LENGTH_SHORT).show();
                                                updateUserProfilePicUrl(imageUrl);
                                            }
                                        });
                                    }

                                }
                            });

                            Toast.makeText(getContext(), "Upload Succesfull", Toast.LENGTH_SHORT).show();

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

    private void getIngredientList() {
        ingredientsReference.document("ingredient_list")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            ingredientList = (List<String>) documentSnapshot.get("ingredient_list");
                            ingStringArray = ingredientList.toArray(new String[ingredientList.size()]);

                            setupCheckList();
                        }
                    }
                });
    }

    private void getUserInfo() {
        usersReference.whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null) {
                            String data = "";

                            User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                            userDocumentID = queryDocumentSnapshots.getDocuments().get(0).getId();
                            userProfilePicUrl = user.getUserProfilePicUrl();

                            for (String tag : user.getTags().keySet()) {
                                data += "\n " + tag + " " + user.getTags().get(tag);
                                ingredientsUserHas.put(tag, Objects.requireNonNull(user.getTags().get(tag)));
                            }
                            Log.d(TAG, "onEvent: " + ingredientsUserHas.toString());

                            textViewData.setText(data);

                            tvHelloUserName.setText(String.format("Hello, %s", user.getName()));

                            getIngredientList();

                            pbLoading.setVisibility(View.INVISIBLE);

                            if (!userProfilePicUrl.equals("")) {
                                Picasso.get()
                                        .load(userProfilePicUrl)
                                        .error(R.drawable.ic_home_black_24dp)
                                        .fit()
                                        .centerCrop()
                                        .into(mProfileImage);
                            } else if (userProfilePicUrl.equals("")) {
                                Picasso.get()
                                        .load(R.drawable.ic_home_black_24dp)
                                        .placeholder(R.drawable.ic_home_black_24dp)
                                        .into(mProfileImage);
                                Toast.makeText(getContext(), "empty", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }


    private void setupCheckList() {
        //create an instance of ListView
        //set multiple selection mode
        if (getActivity() != null) {
            cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
//            final String[] items = PossibleIngredients.getIngredientsNames();
            final String[] items = ingStringArray;

            //supply data items to ListView
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.checkable_list_layout, R.id.txt_title, items);
            cbListView.setAdapter(arrayAdapter);

            // sets the initial checkbox values taken from database
            int index = 0;
            for (String item : items) {
                cbListView.setItemChecked(index, ingredientsUserHas.get(item));
                Log.d(TAG, item + " index " + index + " value " + ingredientsUserHas.get(item));
                index++;
            }

            cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // gets checkBox value and updates database with it
                    Map<String, Boolean> selectedIngredientsMap = new HashMap<>();
                    int i = 0;
                    for (String tag : items) {
                        selectedIngredientsMap.put(tag, cbListView.isItemChecked(i));
                        i++;
                    }

                    db.collection("Users").document(userDocumentID)
                            .update("tags", selectedIngredientsMap);
                }

            });
        }
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /*
    ----------------------------- Firebase setup ---------------------------------
 */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    //check if email is verified
                    if (user.isEmailVerified()) {
//                        Log.d(TAG, "onAuthStateChanged: signed_in: " + user.getUid());
//                        Toast.makeText(MainActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Email is not Verified\nCheck your Inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out");
                    Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
                // ...
            }
        };
    }
}
