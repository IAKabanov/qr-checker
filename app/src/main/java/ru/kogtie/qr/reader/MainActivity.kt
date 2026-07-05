package ru.kogtie.qr.reader

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = FragmentContainerView(this).apply {
            id = R.id.fragment_container
        }
        setContentView(container)

        if (savedInstanceState == null) {
            val navHostFragment = NavHostFragment.create(R.navigation.nav_graph)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, navHostFragment)
                .setPrimaryNavigationFragment(navHostFragment)
                .commit()
        }
    }
}
