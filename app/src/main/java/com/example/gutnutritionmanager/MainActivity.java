package com.example.gutnutritionmanager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    public NutritionViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel with factory
        NutritionViewModelFactory factory = new NutritionViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(NutritionViewModel.class);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // Link the ViewPager with the TabLayout
        viewPager.setAdapter(new NutritionPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Set tab titles
                    switch (position) {
                        case 0:
                            tab.setText("Diary");
                            break;
                        case 1:
                            tab.setText("Symptoms");
                            break;
                        case 2:
                            tab.setText("Insights");
                            break;
                        case 3:
                            tab.setText("Recipes");
                            break;

                    }
                }).attach();
    }
    // Show/hide it in your code


    // Adapter to manage the three tabs (fragments)
    private class NutritionPagerAdapter extends FragmentStateAdapter {
        public NutritionPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new DiaryFragment();
                case 1:
                    return new SymptomsFragment();
                case 2:
                    return new InsightsFragment();
                case 3:
                    return new RecipesFragment(); // NEW RECIPES TAB
                default:
                    return new DiaryFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4; // Number of tabs
        }
    }


}