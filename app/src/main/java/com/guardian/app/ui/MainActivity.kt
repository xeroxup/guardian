package com.guardian.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.guardian.app.R
import com.guardian.app.databinding.ActivityMainBinding
import com.guardian.app.ui.screens.BlacklistFragment
import com.guardian.app.ui.screens.HomeFragment
import com.guardian.app.ui.screens.LogFragment
import com.guardian.app.ui.screens.SettingsFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_blacklist -> BlacklistFragment()
                R.id.nav_log -> LogFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
