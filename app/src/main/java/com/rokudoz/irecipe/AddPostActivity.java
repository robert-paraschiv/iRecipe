package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.Utils.RotateBitmap;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddPostActivity extends AppCompatActivity {
    private static final String TAG = "AddPostActivity";

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri mImageUri;
    private String postPicUrl = "";
    Bitmap imageBitmap;
    //FireBase refs
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("PostPhotos");
    private StorageTask mUploadTask;


    MaterialButton searchRecipeBtn, publishBtn, choosePhotoBtn;
    TextInputEditText descriptionInputText;
    Spinner privacySpinner;
    ImageView imageView;

    String referencedRecipeDocID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        descriptionInputText = findViewById(R.id.addPost_description_editText);
        searchRecipeBtn = findViewById(R.id.addPost_selectRecipe_btn);
        publishBtn = findViewById(R.id.addPost_post_btn);
        choosePhotoBtn = findViewById(R.id.addPost_choose_path_btn);
        privacySpinner = findViewById(R.id.addPost_privacy_spinner);
        imageView = findViewById(R.id.addPost_image);

        String postText = "";
        if (getIntent() != null) {
            if (getIntent().getStringExtra("post_text") != null) {
                postText = getIntent().getStringExtra("post_text");
                descriptionInputText.setText(postText);
            }
            if (getIntent().getStringExtra("recipe_doc_id") != null) {
                referencedRecipeDocID = getIntent().getStringExtra("recipe_doc_id");
                searchRecipeBtn.setText("Change recipe");
            }
        }
        searchRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddPostActivity.this, SearchRecipeActivity.class);
                intent.putExtra("coming_from","AddPostActivity");
                intent.putExtra("post_text", descriptionInputText.getText().toString());
                startActivity(intent);
            }
        });

        choosePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(AddPostActivity.this, "Upload in progress...", Toast.LENGTH_SHORT).show();

                } else {
                    openFileChooser();
                }
            }
        });
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (referencedRecipeDocID.equals("")) {
                    Toast.makeText(AddPostActivity.this, "You need to select a recipe you've followed", Toast.LENGTH_SHORT).show();
                } else if (descriptionInputText.getText().toString().equals("")) {
                    Toast.makeText(AddPostActivity.this, "Description can't be empty", Toast.LENGTH_SHORT).show();
                } else
                    uploadUserProfilePic();
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
                imageBitmap = rotateBitmap.HandleSamplingAndRotationBitmap(this, mImageUri);
                imageView.setImageBitmap(imageBitmap);
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

    private void uploadUserProfilePic() {
        if (mImageUri != null) {
            final StorageReference newFileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

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
                                    postPicUrl = imageUrl;
                                    //
                                    addPost();

                                }
                            });
                            Log.d(TAG, "onSuccess: Upload Succesfull");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddPostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            Toast.makeText(AddPostActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void addPost() {
        String creatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String text = descriptionInputText.getText().toString();
        String privacy = privacySpinner.getSelectedItem().toString();

        Post post = new Post(referencedRecipeDocID, creatorId, text, postPicUrl, false, privacy, null);
        if (postPicUrl.equals("")) {
            Toast.makeText(this, "Please select a photo for your post", Toast.LENGTH_SHORT).show();
        } else {
            db.collection("Posts").add(post).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Toast.makeText(AddPostActivity.this, "Successfully published your post", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddPostActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }


    }
}
