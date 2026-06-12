package com.example

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.databinding.ActivityMainBinding
import com.example.fragment.TravelListFragment
import com.example.fragment.TravelMapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val listFragment = TravelListFragment()
    private val mapFragment = TravelMapFragment()
    private var currentNavigationId = R.id.navigation_list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, listFragment, "LIST")
                .add(R.id.fragment_container, mapFragment, "MAP")
                .hide(mapFragment)
                .commit()
        }

        setupNavigation()
        setupBackStackHandling()

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
            if (item.itemId == currentNavigationId) {
                return@setOnItemSelectedListener true
            }

            val listFrag = supportFragmentManager.findFragmentByTag("LIST") ?: listFragment
            val mapFrag = supportFragmentManager.findFragmentByTag("MAP") ?: mapFragment
            val transaction = supportFragmentManager.beginTransaction().setReorderingAllowed(true)

            when (item.itemId) {
                R.id.navigation_list -> {
                    transaction.show(listFrag)
                    transaction.hide(mapFrag)
                    transaction.addToBackStack(null)
                    transaction.commit()
                    currentNavigationId = R.id.navigation_list
                    true
                }
                R.id.navigation_map -> {
                    transaction.show(mapFrag)
                    transaction.hide(listFrag)
                    transaction.addToBackStack(null)
                    transaction.commit()
                    currentNavigationId = R.id.navigation_map
                    if (mapFrag is TravelMapFragment) {
                        mapFrag.loadMapData()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackStackHandling() {
        supportFragmentManager.addOnBackStackChangedListener {
            val mapVisible = supportFragmentManager.findFragmentByTag("MAP")?.isVisible == true
            currentNavigationId = if (mapVisible) R.id.navigation_map else R.id.navigation_list
            binding.bottomNavigation.menu.findItem(currentNavigationId)?.isChecked = true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })
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
            R.id.menu_delete_selected -> {
                ensureListVisible { listFrag?.showSelectDeleteDialog() }
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
            .setTitle("앱 정보")
            .setMessage(
                "앱 이름: Jansang Travel\n" +
                    "개발 언어: Kotlin\n\n" +
                    "Jansang Travel은 여행 기록을 관리하고 지도에서 위치를 확인할 수 있는 앱입니다.\n\n" +
                    "주요 기능\n" +
                    "- Fragment 2개 이상 구성 및 BottomNavigationView 전환\n" +
                    "- RecyclerView 여행 기록 목록\n" +
                    "- SQLiteOpenHelper 기반 CRUD\n" +
                    "- 카메라 촬영 및 갤러리 사진 선택\n" +
                    "- 사진 GPS 정보 추출\n" +
                    "- Google Maps 지도 및 마커 표시\n" +
                    "- 옵션 메뉴와 컨텍스트 메뉴\n\n" +
                    "2026 모바일 프로그래밍 기말 프로젝트"
            )
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
