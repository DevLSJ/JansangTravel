package com.example.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.AddEditActivity
import com.example.DetailActivity
import com.example.adapter.TravelAdapter
import com.example.databinding.FragmentTravelListBinding
import com.example.db.RecordEntity
import com.example.db.RecordViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TravelListFragment : Fragment() {

    private var _binding: FragmentTravelListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordViewModel
    private lateinit var adapter: TravelAdapter

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

        viewModel = ViewModelProvider(requireActivity())[RecordViewModel::class.java]

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditActivity::class.java))
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TravelAdapter(
            context = requireContext(),
            records = emptyList(),
            onItemClick = { item ->
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("TRAVEL_ITEM", item)
                }
                startActivity(intent)
            },
            onEditClick = { item ->
                val intent = Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra("TRAVEL_ITEM", item)
                }
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(item: RecordEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("'${item.title}' 여행 기록을 삭제하시겠습니까?\n삭제한 내용은 복구할 수 없습니다.")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteRecord(item) { rows ->
                    val message = if (rows > 0) {
                        "기록이 삭제되었습니다."
                    } else {
                        "삭제 처리에 실패했습니다."
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.records.collectLatest { list ->
                adapter.updateList(list)
                if (list.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
                updateSubtitleCount(list.size)
            }
        }
    }

    private fun updateSubtitleCount(count: Int) {
        binding.tvHeaderSubtitle.text = "현재 ${count}개의 여행지가 등록되어 있습니다"
    }

    fun updateSortOrder(sortOrder: String) {
        viewModel.setSortOrder(sortOrder)
        val text = when (sortOrder) {
            "DATE_DESC" -> "최신순으로 정렬했습니다."
            "DATE_ASC" -> "오래된순으로 정렬했습니다."
            "TITLE_ASC" -> "제목순으로 정렬했습니다."
            else -> "정렬 기준이 변경되었습니다."
        }
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    fun clearAllData() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 기록 삭제")
            .setMessage("모든 여행 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("모두 삭제") { dialog, _ ->
                viewModel.deleteAllRecords {
                    Toast.makeText(requireContext(), "모든 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }
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
        viewModel.loadRecords()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
