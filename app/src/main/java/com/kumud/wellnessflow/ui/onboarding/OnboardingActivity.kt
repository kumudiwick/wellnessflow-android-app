package com.kumud.wellnessflow.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.UserProfile
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.databinding.ActivityOnboardingBinding
import com.kumud.wellnessflow.ui.main.MainActivity

/**
 * Collects initial profile details and hands control to the main experience.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val preferences by lazy { PreferencesManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingNameInput.doAfterTextChanged {
            binding.onboardingNameLayout.error = null
        }

        // Validate the name before persisting and advancing to the main flow.
        binding.onboardingContinue.setOnClickListener {
            val name = binding.onboardingNameInput.text?.toString().orEmpty().trim()
            if (name.isEmpty()) {
                binding.onboardingNameLayout.error = getString(R.string.onboarding_name_error)
                return@setOnClickListener
            }

            preferences.saveUserProfile(UserProfile(displayName = name))
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}
