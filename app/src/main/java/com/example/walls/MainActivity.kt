package com.example.walls


import FilterDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    @Inject
    lateinit var favoritesManager: FavoritesManager
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewModel: WallpaperViewModel
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = WallpaperViewModelFactory(application, favoritesManager)
        viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]
        Log.d("MainActivity", "onCreate called")

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set up the navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle home action
            }

            R.id.nav_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
            }

            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)
        val apiKey = sharedPreferences.getString("API_KEY", null)
        Log.d("MainActivity", "Current API key: $apiKey")
        if (apiKey != null) {
            // Refresh wallpapers if API key is set
            viewModel.fetchWallpapers("date_added")
            viewModel.fetchWallpapers("toplist")
        }
    }

    private fun showFilterDialog() {
        val filterDialog = FilterDialog(viewModel)
        filterDialog.setOnFilterAppliedListener { categories, purity ->
            onFilterApplied(categories, purity)
        }
        filterDialog.show(supportFragmentManager, "FilterDialog")
    }

    private fun onFilterApplied(categories: String, purity: String) {
        Log.d("MainActivity", "Filter applied: categories=$categories, purity=$purity")
        viewModel.updateFilters(categories, purity)
        refreshCurrentFragment()
    }

    private fun refreshCurrentFragment() {
        val currentItem = viewPager.currentItem
        when (currentItem) {
            0 -> viewModel.fetchWallpapers("date_added")
            1 -> viewModel.fetchWallpapers("toplist")
        }
    }

    private fun observeFilterChanges() {
        lifecycleScope.launch {
            viewModel.filterChanged.collectLatest { changed ->
                if (changed) {
                    refreshCurrentFragment()
                }
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