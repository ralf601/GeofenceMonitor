package com.hsn.trackme.ui.common.controller;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hsn.trackme.R;
import com.hsn.trackme.ui.common.view.RootViewImpl;
import com.hsn.trackme.ui.common.view.ViewMvc;
import com.hsn.trackme.ui.home.controller.HomeFragment;

public class MainActivity extends AppCompatActivity implements BaseFragment.AbstractFragmentCallback {

    ViewMvc viewMvc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewMvc = new RootViewImpl(this, null);

        // Set the root view of the associated MVC view as the content of this activity
        setContentView(viewMvc.getRootView());

        // Show the default fragment if the application is not restored
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment.class, false, null);
        }
    }

    @Override
    public void replaceFragment(Class<? extends Fragment> claz, boolean addToBackStack,
                                Bundle args) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();


        Fragment newFragment;

        try {
            // Create new fragment
            newFragment = claz.newInstance();
            if (args != null) newFragment.setArguments(args);
        } catch (InstantiationException e) {
            e.printStackTrace();
            return;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        if (addToBackStack) {
            // Add this transaction to the back stack
            ft.addToBackStack(null);
        }

        // Change to a new fragment
        ft.replace(R.id.frame_contents, newFragment, claz.getClass().getSimpleName());
        ft.commit();


    }
}
