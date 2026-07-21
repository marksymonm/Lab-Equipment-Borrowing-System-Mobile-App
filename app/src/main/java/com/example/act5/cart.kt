package com.example.act5

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class cart : Fragment() {

    private lateinit var itemsContainer: LinearLayout
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_cart, container, false)

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val borrowButton = view.findViewById<Button>(R.id.borrowButton)
        itemsContainer = view.findViewById(R.id.itemsContainer)

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_studentDashboard)
        }

        database = FirebaseDatabase.getInstance().reference
        loadCartItems()

        borrowButton.setOnClickListener {
            submitBorrowRequest()
        }

        return view
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.child("cart").child(userId)

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                itemsContainer.removeAllViews()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItem::class.java) ?: continue
                    val equipmentId = itemSnapshot.key ?: continue
                    addCartItemView(equipmentId, item)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addCartItemView(equipmentId: String, item: CartItem) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 10)
        }

        val nameView = TextView(requireContext()).apply {
            text = item.name
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(8, 8, 8, 8)
            setTextColor(resources.getColor(android.R.color.black))
            textSize = 18f
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
        }

        val decreaseBtn = Button(requireContext()).apply {
            text = "-"
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 105
                marginEnd = 8
                gravity = Gravity.CENTER_VERTICAL
            }
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            background = ContextCompat.getDrawable(context, R.drawable.round_red_button)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
            stateListAnimator = null
            setPadding(0, 6, 0, 6)
            gravity = Gravity.CENTER
            setOnClickListener {
                updateQuantity(equipmentId, item.quantity - 1)
            }
        }

        val quantityView = TextView(requireContext()).apply {
            text = item.quantity.toString()
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 4
                marginEnd = 4
            }
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
        }

        val increaseBtn = Button(requireContext()).apply {
            text = "+"
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 8
                marginEnd = 105
                gravity = Gravity.CENTER_VERTICAL
            }
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            background = ContextCompat.getDrawable(context, R.drawable.round_red_button)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
            stateListAnimator = null
            setPadding(0, 6, 0, 6)
            setOnClickListener {
                updateQuantity(equipmentId, item.quantity + 1)
            }
        }

        row.addView(nameView)
        row.addView(decreaseBtn)
        row.addView(quantityView)
        row.addView(increaseBtn)

        itemsContainer.addView(row)

        val separator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        }
        itemsContainer.addView(separator)
    }

    private fun updateQuantity(equipmentId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        val cartItemRef = database.child("cart").child(userId).child(equipmentId)

        if (newQuantity < 1) {
            cartItemRef.removeValue()
        } else {
            cartItemRef.child("quantity").setValue(newQuantity)
        }

        loadCartItems()
    }

    private fun submitBorrowRequest() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.child("cart").child(userId)

        cartRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            for (itemSnapshot in snapshot.children) {
                val item = itemSnapshot.getValue(CartItem::class.java) ?: continue
                val equipmentId = itemSnapshot.key ?: continue

                database.child("equipment").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(equipmentSnapshot: DataSnapshot) {
                        outer@ for (categorySnapshot in equipmentSnapshot.children) {
                            val equipment = categorySnapshot.child(item.name)
                            if (equipment.exists()) {
                                val currentQty = equipment.child("quantity").getValue(Int::class.java) ?: 0
                                val newQty = currentQty - item.quantity

                                if (newQty < 0) {
                                    Toast.makeText(requireContext(), "Not enough stock for ${item.name}", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // Update stock
                                database.child("equipment")
                                    .child(categorySnapshot.key!!)
                                    .child(item.name)
                                    .child("quantity")
                                    .setValue(newQty)

                                // ✅ Set category and save to borrowedEquipments
                                val itemWithCategory = item.copy(category = categorySnapshot.key!!)
                                val borrowedRef = database.child("borrowedEquipments").child(userId).push()
                                borrowedRef.setValue(itemWithCategory)

                                // Remove from cart
                                database.child("cart").child(userId).child(equipmentId).removeValue()
                                break@outer
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error fetching equipment stock", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            Toast.makeText(requireContext(), "Borrow request submitted", Toast.LENGTH_SHORT).show()
            loadCartItems()

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to process cart", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Now includes category
    data class CartItem(
        var name: String = "",
        var quantity: Int = 0,
        var category: String = ""
    )
}
