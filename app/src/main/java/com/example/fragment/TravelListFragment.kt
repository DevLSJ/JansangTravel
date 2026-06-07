package com.example.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.AddEditActivity
import com.example.DetailActivity
import com.example.adapter.TravelAdapter
import com.example.databinding.FragmentTravelListBinding
import com.example.db.DBHelper
import com.example.model.TravelItem

class TravelListFragment : Fragment() {

    private var _binding: FragmentTravelListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: TravelAdapter
    private var currentSortOrder = "DATE_DESC"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravelListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(requireContext())

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            startActivity(intent)
        }

        loadData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TravelAdapter(
            context = requireContext(),
            travelList = emptyList(),
            onItemClick = { item ->
                // Click to view detail screen
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("TRAVEL_ITEM", item)
                }
                startActivity(intent)
            },
            onEditClick = { item ->
                // Context menu edit action
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("TRAVEL_ITEM", item)
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                // Context menu delete action
                showDeleteConfirmationDialog(item)
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(item: TravelItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("'${item.place}' 여행 기록을 정말로 완전히 삭제하시겠습니까?\n삭제된 내용은 복구할 수 없습니다.")
            .setPositiveButton("삭제") { dialog, _ ->
                val rows = dbHelper.deleteTravel(item.no)
                if (rows > 0) {
                    Toast.makeText(requireContext(), "기록이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).sharedShow()
                } else {
                    Toast.makeText(requireContext(), "삭제 처리에 실패하였습니다.", Toast.LENGTH_SHORT).sharedShow()
                }
                loadData()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Custom helper to chain Toast.show safely or fix compile if extension is wanted,
    // let's just make sure toast is standard Toast.makeText().show() to avoid compile errors!
    private fun Toast.sharedShow() {
        this.show()
    }

    fun loadData() {
        val list = dbHelper.getAllTravels(currentSortOrder)
        adapter.updateList(list)

        if (list.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }

        // Update parent metric references if accessible
        updateSubtitleCount(list.size)
    }

    private fun updateSubtitleCount(count: Int) {
        binding.tvHeaderSubtitle.text = "현재까지 총 ${count}개의 소중한 발자취가 기록되어 있습니다"
    }

    fun updateSortOrder(sortOrder: String) {
        currentSortOrder = sortOrder
        loadData()
        val text = if (sortOrder == "DATE_DESC") "최신순으로 정렬되었습니다." else "오래된순으로 정렬되었습니다."
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    fun clearAllData() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 기록 삭제")
            .setMessage("기록된 모든 여행 기행문을 완전히 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("모두 삭제") { dialog, _ ->
                dbHelper.deleteAllTravels()
                loadData()
                Toast.makeText(requireContext(), "모든 기행문이 완전히 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
