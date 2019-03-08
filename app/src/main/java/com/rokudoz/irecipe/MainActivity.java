package com.rokudoz.irecipe;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private EditText editTextTitle;
    private EditText editTextDescription;
    private EditText editTextPriority;
    private EditText editTextContPotatos;
    private TextView textViewData;
    private CheckBox cbPotatoes;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference notebookRef = db.collection("Recipes");

    private DocumentSnapshot lastResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        editTextPriority = findViewById(R.id.edit_text_priority);
        editTextContPotatos = findViewById(R.id.edit_text_contPotatos);
        textViewData = findViewById(R.id.text_view_data);
        cbPotatoes = findViewById(R.id.cb_contPotatoes);
    }

    @Override
    protected void onStart() {
        super.onStart();
        notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }

                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    DocumentSnapshot documentSnapshot = dc.getDocument();
                    String id = documentSnapshot.getId();
                    int oldIndex = dc.getOldIndex();
                    int newIndex = dc.getNewIndex();

//                    switch (dc.getType()) {
//                        case ADDED:
//                            textViewData.append("\nAdded: " + id +
//                                    "\nOld Index: " + oldIndex + "New Index: " + newIndex);
//                            break;
//                        case MODIFIED:
//                            textViewData.append("\nModified: " + id +
//                                    "\nOld Index: " + oldIndex + "New Index: " + newIndex);
//                            break;
//                        case REMOVED:
//                            textViewData.append("\nRemoved: " + id +
//                                    "\nOld Index: " + oldIndex + "New Index: " + newIndex);
//                            break;
//                    }
                }
            }
        });
    }

    public void addNote(View v) {
        String title = editTextTitle.getText().toString();
        String description = editTextDescription.getText().toString();

        if (editTextPriority.length() == 0) {
            editTextPriority.setText("0");
        }
        if (editTextContPotatos.length()==0){
            editTextContPotatos.setText("0");
        }

        int contPotatos = Integer.parseInt(editTextContPotatos.getText().toString());

        int priority = Integer.parseInt(editTextPriority.getText().toString());

        Note note = new Note(title, description, priority, contPotatos);

        notebookRef.add(note);
    }

    public void loadNotes(View v) {
//        int contPotatos = 0;
//        contPotatos = Integer.parseInt(editTextContPotatos.getText().toString());

        int contPotatos=0;
        if (cbPotatoes.isChecked()) {
            contPotatos=1;
        } else if(!cbPotatoes.isChecked()) {
            contPotatos=0;
        }

        Query query;
//        if (lastResult == null) {
//            query = notebookRef.orderBy("priority")
//                    .whereEqualTo("contPotatos",contPotatos)
//                    .limit(3);
//        } else {
//            query = notebookRef.orderBy("priority")
//                    .whereEqualTo("contPotatos",contPotatos)
//                    .startAfter(lastResult)
//                    .limit(3);
//        }

        query = notebookRef.orderBy("title")
                .whereEqualTo("contPotatos",contPotatos);

        query.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        String data = "";

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Note note = documentSnapshot.toObject(Note.class);
                            note.setDocumentId(documentSnapshot.getId());

                            String documentId = note.getDocumentId();
                            String title = note.getTitle();
                            String description = note.getDescription();
                            int contPotatos = note.getContPotatos();
                            int priority = note.getPriority();

                            data += "ID: " + documentId
                                    + "\nTitle: " + title + "\nDescription: " + description
                                    + "\nContains Potatos: " + contPotatos
                                    + "\nPriority: " + priority + "\n\n";
                        }

                        if (queryDocumentSnapshots.size() > 0) {
                            data += "___________\n\n";
                            textViewData.append(data);

                            lastResult = queryDocumentSnapshots.getDocuments()
                                    .get(queryDocumentSnapshots.size() - 1);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: "+e.toString());
                Toast.makeText(MainActivity.this, ""+e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}