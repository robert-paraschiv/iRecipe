package com.rokudoz.irecipe;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView textViewData;
    ArrayList<String> selectedIngredients;//-----

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Recipes");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedIngredients = new ArrayList<String>();

        textViewData = findViewById(R.id.text_view_data);

    }

    public void onStart() {
        super.onStart();
        //create an instance of ListView
        ListView cbListView = findViewById(R.id.checkable_list);
        //set multiple selection mode
        cbListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        String[] items = {"potatoes", "cheese", "apples", "salt"};
        //supply data itmes to ListView
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.checkable_list_layout, R.id.txt_title, items);
        cbListView.setAdapter(arrayAdapter);
        //set OnItemClickListener
        cbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // selected item
                String selectedItem = ((TextView) view).getText().toString();
                if (selectedIngredients.contains(selectedItem))
                    selectedIngredients.remove(selectedItem); //remove deselected item from the list of selected items
                else
                    selectedIngredients.add(selectedItem); //add selected item to the list of selected items

            }

        });
    }

    public void loadNotes(View v) {

        String ingredientsString = "";
        for (String ingredient : selectedIngredients) {
            if (ingredientsString == "") {
                ingredientsString = ingredient;
            } else {
                ingredientsString += "," + ingredient;
            }
        }

        String[] ingredientsArray = ingredientsString.split("\\s*,\\s*");
        Map<String, Boolean> ingredientsHashMap = new HashMap<>();

        for (String tag : ingredientsArray) {
            ingredientsHashMap.put(tag, true);
        }

        collectionReference.whereEqualTo("tags", ingredientsHashMap).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        String data = "";

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Recipe recipe = documentSnapshot.toObject(Recipe.class);
                            recipe.setDocumentId(documentSnapshot.getId());

                            String documentId = recipe.getDocumentId();
                            String title = recipe.getTitle();

                            data += "ID: " + documentId + "\nTitle: " + title;

                            for (String tag : recipe.getTags().keySet()) {
                                data += "\n-" + tag;
                            }

                            data += "\n\n";
                        }
                        textViewData.setText(data);
                    }
                });
    }

    public void navigateToAddRecipes(View v) {
        Intent intent = new Intent(this, AddRecipesActivity.class);
        startActivity(intent);
    }
}