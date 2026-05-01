package com.kumud.wellnessflow.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.content.Intent
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.ui.onboarding.OnboardingActivity
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.databinding.ActivityMainBinding
import com.kumud.wellnessflow.ui.habits.HabitsFragment
import com.kumud.wellnessflow.ui.home.HomeFragment
import com.kumud.wellnessflow.ui.hydration.HydrationFragment
import com.kumud.wellnessflow.ui.mood.MoodFragment
import com.kumud.wellnessflow.ui.todo.TodoFragment

/**
 * Entry activity that guards onboarding and swaps the main feature fragments via bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val preferences by lazy { PreferencesManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = preferences.getUserProfile()
        if (profile == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            handleNavigation(item.itemId)
        }

        if (savedInstanceState == null) {
            val startDestination = intent?.getIntExtra(EXTRA_DESTINATION, R.id.menu_home) ?: R.id.menu_home
            binding.bottomNav.selectedItemId = startDestination
            handleNavigation(startDestination)
        }
    }

    // Reuse existing fragments when present so bottom navigation keeps state.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val requested = intent.getIntExtra(EXTRA_DESTINATION, binding.bottomNav.selectedItemId)
        if (requested != binding.bottomNav.selectedItemId) {
            binding.bottomNav.selectedItemId = requested
            handleNavigation(requested)
        }
    }

    private fun handleNavigation(menuId: Int): Boolean {
        return when (menuId) {
            R.id.menu_home -> {
                loadFragment(HomeFragment(), HOME_TAG)
                true
            }
            R.id.menu_habits -> {
                loadFragment(HabitsFragment(), HABITS_TAG)
                true
            }
            R.id.menu_mood -> {
                loadFragment(MoodFragment(), MOOD_TAG)
                true
            }
            R.id.menu_todo -> {
                loadFragment(TodoFragment(), TODO_TAG)
                true
            }
            R.id.menu_hydration -> {
                loadFragment(HydrationFragment(), HYDRATION_TAG)
                true
            }
            else -> false
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction().setReorderingAllowed(true)

        val fragmentToShow = fragmentManager.findFragmentByTag(tag) ?: fragment.also {
            transaction.add(R.id.fragment_container, it, tag)
        }

        fragmentManager.fragments.forEach { existing ->
            if (existing == fragmentToShow) {
                transaction.show(existing)
            } else {
                transaction.hide(existing)
            }
        }

        transaction.commit()
    }

    companion object {
        private const val HOME_TAG = "home_fragment"
        private const val HABITS_TAG = "habits_fragment"
        private const val MOOD_TAG = "mood_fragment"
        private const val HYDRATION_TAG = "hydration_fragment"
        private const val TODO_TAG = "todo_fragment"
        const val EXTRA_DESTINATION = "com.kumud.wellnessflow.EXTRA_DESTINATION"
    }
}
