package com.ruhidjavadoff.pichilti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.ruhidjavadoff.pichilti.databinding.ActivityMainBinding
import com.ruhidjavadoff.pichilti.fragments.ChatFragment
import com.ruhidjavadoff.pichilti.fragments.CreateRoomFragment
import com.ruhidjavadoff.pichilti.fragments.RoomsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAdmin: Boolean = false
    var currentUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userRoleString = intent.getStringExtra("USER_ROLE")
        currentUsername = intent.getStringExtra("USERNAME")
        isAdmin = userRoleString == UserRole.ADMIN.name

        setupEdgeToEdge()
        setupUIBasedOnRole()

        if (savedInstanceState == null) {
            replaceFragment(RoomsFragment.newInstance(isAdmin))
            supportFragmentManager.executePendingTransactions()
        }

        binding.createRoomButton.setOnClickListener {
            replaceFragment(CreateRoomFragment.newInstance(), true)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateGlobalUiVisibility()
        }
        updateGlobalUiVisibility()
    }

    private fun updateGlobalUiVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.bodyFragmentContainer)
        val shouldShowGlobalUi = currentFragment is RoomsFragment
        binding.mainHeader.root.isVisible = shouldShowGlobalUi
        binding.createRoomButton.isVisible = shouldShowGlobalUi && isAdmin
    }

    internal fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        transaction.replace(R.id.bodyFragmentContainer, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun setupUIBasedOnRole() {
        binding.createRoomButton.isVisible = isAdmin
    }
}