package com.example

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.databinding.ActivityMainBinding
import com.example.fragment.TravelListFragment
import com.example.fragment.TravelMapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val listFragment = TravelListFragment()
    private val mapFragment = TravelMapFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize fragments on cold start
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, listFragment, "LIST")
                .add(R.id.fragment_container, mapFragment, "MAP")
                .hide(mapFragment)
                .commit()
        }

        setupNavigation()

        // Custom menu button listener
        binding.btnMenu.setOnClickListener {
            openOptionsMenu()
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val transaction = supportFragmentManager.beginTransaction()
            val listFrag = supportFragmentManager.findFragmentByTag("LIST") ?: listFragment
            val mapFrag = supportFragmentManager.findFragmentByTag("MAP") ?: mapFragment

            when (item.itemId) {
                R.id.navigation_list -> {
                    transaction.show(listFrag)
                    transaction.hide(mapFrag)
                    transaction.commit()
                    true
                }
                R.id.navigation_map -> {
                    transaction.show(mapFrag)
                    transaction.hide(listFrag)
                    transaction.commit()
                    if (mapFrag is TravelMapFragment) {
                        mapFrag.loadMapData()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val listFrag = supportFragmentManager.findFragmentByTag("LIST") as? TravelListFragment
        
        when (item.itemId) {
            R.id.menu_sort_latest -> {
                ensureListVisible { listFrag?.updateSortOrder("DATE_DESC") }
                return true
            }
            R.id.menu_sort_oldest -> {
                ensureListVisible { listFrag?.updateSortOrder("DATE_ASC") }
                return true
            }
            R.id.menu_delete_all -> {
                ensureListVisible { listFrag?.clearAllData() }
                return true
            }
            R.id.menu_app_info -> {
                showAppInfoDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun ensureListVisible(action: () -> Unit) {
        val listFrag = supportFragmentManager.findFragmentByTag("LIST") as? TravelListFragment
        if (listFrag == null || !listFrag.isVisible) {
            // Pivot back to the list screen
            binding.bottomNavigation.selectedItemId = R.id.navigation_list
            binding.bottomNavigation.post {
                val currentListFrag = supportFragmentManager.findFragmentByTag("LIST") as? TravelListFragment
                currentListFrag?.let { action() }
            }
        } else {
            action()
        }
    }

    private fun showAppInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("JansangTravel (잔상트래블)")
            .setMessage("버전: 1.0.0\n\n개발자: 세계 최고 Android 개발자\n\n이 앱은 다녀온 여행 기록을 지도와 함께 소중한 '잔상'으로 간직할 수 있는 가벼운 Airbnb 스타일 다이어리 애플리케이션입니다.\n\n© 2026 JansangTravel All Rights Reserved.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
