package com.rokudoz.irecipe;

import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.rokudoz.irecipe.Fragments.FavoritesFragment;
import com.rokudoz.irecipe.Fragments.HomeFragment;
import com.rokudoz.irecipe.Fragments.ProfileFragment;
import com.rokudoz.irecipe.Fragments.SearchFragment;

import java.util.ArrayDeque;
import java.util.Deque;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String FRAGMENT_HOME = "HomeFragment";
    private static final String FRAGMENT_SEARCH = "SearchFragment";
    private static final String FRAGMENT_FAVORITES = "FavoritesFragment";
    private static final String FRAGMENT_PROFILE = "ProfileFragment";

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        //I added this if statement to keep the selected fragment when rotating the device
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
//                    new HomeFragment()).commit();
//        }
        setBottomNavigationView();
    }


    Deque<Integer> mStack = new ArrayDeque<>();
    boolean isBackPressed = false;

    private void setBottomNavigationView() {
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        if (!isBackPressed) {
                            pushFragmentIntoStack(R.id.nav_home);
                        }
                        isBackPressed = false;
                        setFragment(HomeFragment.newInstance(), FRAGMENT_HOME);
                        return true;
                    case R.id.nav_search:
                        if (!isBackPressed) {
                            pushFragmentIntoStack(R.id.nav_search);
                        }
                        isBackPressed = false;
                        setFragment(SearchFragment.newInstance(), FRAGMENT_SEARCH);
                        return true;
                    case R.id.nav_favorites:
                        if (!isBackPressed){
                            pushFragmentIntoStack(R.id.nav_favorites);
                        }
                        isBackPressed=false;
                        setFragment(FavoritesFragment.newInstance(),FRAGMENT_FAVORITES);
                        return true;
                    case R.id.nav_account:
                        if (!isBackPressed) {
                            pushFragmentIntoStack(R.id.nav_account);
                        }
                        isBackPressed = false;
                        setFragment(ProfileFragment.newInstance(), FRAGMENT_PROFILE);
                        return true;

                    default:
                        return false;
                }
            }
        });
        bottomNav.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                if (item.getItemId()==R.id.nav_home){
                    setFragment(HomeFragment.newInstance(), FRAGMENT_HOME);
                }
            }
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
        pushFragmentIntoStack(R.id.nav_home);
    }

    private void pushFragmentIntoStack(int id) {
        if (mStack.size() < 3) {
            mStack.push(id);
        } else {
            mStack.removeLast();
            mStack.push(id);
        }
    }

    private void setFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (mStack.size() > 1) {
            isBackPressed = true;
            mStack.pop();
            bottomNav.setSelectedItemId(mStack.peek());
        } else {
            super.onBackPressed();
        }
    }
}