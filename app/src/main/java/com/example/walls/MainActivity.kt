package com.example.walls

import FilterDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity(), FilterDialog.FilterDialogListener {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewModel: WallpaperViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate called")

        viewModel = ViewModelProvider(this, WallpaperViewModel.Factory(applicationContext))
            .get(WallpaperViewModel::class.java)
        observeFilterChanges()

        // Set up ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = WallpaperPagerAdapter(this, viewModel)
        viewPager.adapter = adapter

        // Initial fetch for both tabs
        Log.d("MainActivity", "Fetching initial wallpapers")
        viewModel.fetchWallpapers("date_added")
        viewModel.fetchWallpapers("toplist")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Recent"
                1 -> "Top"
                else -> throw IllegalArgumentException("Invalid position")
            }
        }.attach()

        findViewById<FloatingActionButton>(R.id.filterFab).setOnClickListener {
            Log.d("MainActivity", "Filter FAB clicked")
            showFilterDialog()
        }

        observeFilterChanges()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d("MainActivity", "Page selected: $position")
                super.onPageSelected(position)
            }
        })
    }


    private fun showFilterDialog() {
        val filterDialog = FilterDialog(viewModel)
        filterDialog.setFilterDialogListener(this)
        filterDialog.show(supportFragmentManager, "FilterDialog")
    }

    override fun onFilterApplied(categories: String, purity: String) {
        viewModel.updateFilters(categories, purity)
    }

    private fun refreshCurrentFragment() {
        val currentItem = viewPager.currentItem
        when (currentItem) {
            0 -> viewModel.fetchWallpapers("date_added")
            1 -> viewModel.fetchWallpapers("toplist")
        }
    }

    // Add this method to observe filter changes
    private fun observeFilterChanges() {
        viewModel.filterChanged.observe(this) { changed ->
            if (changed) {
                refreshCurrentFragment()
            }
        }
    }
}


class WallpaperPagerAdapter(
    activity: AppCompatActivity,
    private val viewModel: WallpaperViewModel
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        Log.d("WallpaperPagerAdapter", "Creating fragment for position: $position")
        return when (position) {
            0 -> WallpaperFragment.newInstance(
                true,
                "date_added",
                viewModel.currentCategories,
                viewModel.currentPurity
            )

            1 -> WallpaperFragment.newInstance(
                false,
                "toplist",
                viewModel.currentCategories,
                viewModel.currentPurity
            )

            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}