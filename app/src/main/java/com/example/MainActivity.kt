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

        // Custom options menu button listener anchored to the options button of the header
        binding.btnMenu.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_option_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                onOptionsItemSelected(menuItem)
            }
            popup.show()
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
            R.id.menu_sort_title -> {
                ensureListVisible { listFrag?.updateSortOrder("TITLE_ASC") }
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
            .setTitle("Photo Record Map App")
            .setMessage(
                "• 앱 이름: Photo Record Map App\n" +
                "• 개발 언어: Kotlin\n\n" +
                "• 구현 기능:\n" +
                "  - Fragment 2개 이상 구성\n" +
                "  - BottomNavigationView 화면 전환\n" +
                "  - 사진 갤러리 선택\n" +
                "  - 카메라 촬영\n" +
                "  - 상세 화면 사진 표시\n" +
                "  - 옵션 메뉴: 전체 삭제, 정렬 변경, 앱 정보\n" +
                "  - 컨텍스트 메뉴: 수정, 삭제\n" +
                "  - AlertDialog 삭제 확인\n" +
                "  - Coroutine 비동기 처리\n" +
                "  - ProgressBar 표시\n" +
                "  - 사진 GPS 추출\n" +
                "  - 지도 API Marker 표시\n\n" +
                "© 2026 Photo Record Map App. All Rights Reserved."
            )
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
