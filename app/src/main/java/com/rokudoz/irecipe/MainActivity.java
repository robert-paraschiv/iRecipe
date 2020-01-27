package com.rokudoz.irecipe;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rokudoz.irecipe.Fragments.FeedFragmentDirections;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setUpNavigation();

    }

    private void setUpNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        if (getIntent() != null && getIntent().getStringExtra("friend_id") != null) {
            String friend_id = getIntent().getStringExtra("friend_id");
            Bundle args = new Bundle();
            args.putString("user_id", friend_id);
            navController.navigate(R.id.messageFragment,args );
        }
        if (getIntent() != null && getIntent().getStringExtra("recipe_id") != null) {
            String recipe_id = getIntent().getStringExtra("recipe_id");
            Bundle args = new Bundle();
            args.putString("documentID", recipe_id);
            navController.navigate(R.id.recipeDetailedFragment,args );
        }
        if (getIntent() != null && getIntent().getStringExtra("post_id") != null) {
            String post_id = getIntent().getStringExtra("post_id");
            Bundle args = new Bundle();
            args.putString("documentID", post_id);
            navController.navigate(R.id.postDetailed,args );
        }
    }


}