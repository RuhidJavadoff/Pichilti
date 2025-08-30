package com.ruhidjavadoff.pichilti.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ruhidjavadoff.pichilti.MainActivity
import com.ruhidjavadoff.pichilti.Room
import com.ruhidjavadoff.pichilti.adapters.RoomAdapter
import com.ruhidjavadoff.pichilti.databinding.FragmentRoomsBinding

class RoomsFragment : Fragment() {

    private var _binding: FragmentRoomsBinding? = null
    private val binding get() = _binding!!

    private val database = Firebase.database("https://pichilti-chat-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("rooms")
    private lateinit var roomListener: ValueEventListener

    private val roomList = mutableListOf<Room>()
    private lateinit var roomAdapter: RoomAdapter
    private var isAdmin: Boolean = false

    companion object {
        private const val ARG_IS_ADMIN = "is_admin"
        fun newInstance(isAdmin: Boolean): RoomsFragment {
            val fragment = RoomsFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_ADMIN, isAdmin)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isAdmin = it.getBoolean(ARG_IS_ADMIN)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToRefresh()
        listenForRoomUpdates()
        if (isAdmin) {
            setupSwipeGestures()
        }
    }

    private fun setupRecyclerView() {
        val mainActivity = activity as? MainActivity
        val currentUsername = mainActivity?.currentUsername ?: "Guest"

        roomAdapter = RoomAdapter(roomList) { clickedRoom ->
            // ChatFragment-ə artıq username göndərməyə ehtiyac yoxdur
            mainActivity?.replaceFragment(
                ChatFragment.newInstance(clickedRoom),
                true
            )
        }
        binding.roomsRecyclerView.adapter = roomAdapter
    }

    private fun setupSwipeGestures() {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val room = roomAdapter.getRoomAt(position)
                if (direction == ItemTouchHelper.LEFT) {
                    database.child(room.id).removeValue()
                        .addOnSuccessListener { Toast.makeText(context, "'${room.name}' silindi.", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { Toast.makeText(context, "Otağı silmək mümkün olmadı.", Toast.LENGTH_SHORT).show() }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    (activity as? MainActivity)?.replaceFragment(CreateRoomFragment.newInstance(room), true)
                    roomAdapter.notifyItemChanged(position)
                }
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.roomsRecyclerView)
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.transparent)
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.transparent)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.refreshAnimationView.isVisible = true
            binding.refreshAnimationView.playAnimation()
            Handler(Looper.getMainLooper()).postDelayed({
                binding.refreshAnimationView.cancelAnimation()
                binding.refreshAnimationView.isVisible = false
                binding.swipeRefreshLayout.isRefreshing = false
            }, 2000)
        }
    }

    private fun listenForRoomUpdates() {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentScrollPosition = (binding.roomsRecyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0
                roomList.clear()
                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.getValue(Room::class.java)
                    if (room != null) {
                        roomList.add(room)
                    }
                }
                roomAdapter.notifyDataSetChanged()
                binding.roomsRecyclerView.scrollToPosition(currentScrollPosition)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("RoomsFragment", "Failed to read value.", error.toException())
            }
        }
        database.addValueEventListener(roomListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.removeEventListener(roomListener)
        _binding = null
    }
}