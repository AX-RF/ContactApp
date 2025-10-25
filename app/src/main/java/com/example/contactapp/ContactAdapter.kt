package com.example.contactapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onClick: (Contact) -> Unit,
    private val onLongClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        val tvInitial: TextView = itemView.findViewById(R.id.tvInitial)
        val imgPhoto: ImageView = itemView.findViewById(R.id.imgPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        holder.tvFullName.text = "${contact.firstName} ${contact.lastName}".trim()
        holder.tvPhoneNumber.text = contact.phoneNumber

        // Handle photo or initial
        if (contact.photoUri != null) {
            holder.imgPhoto.setImageURI(Uri.parse(contact.photoUri))
            holder.tvInitial.visibility = View.GONE
        } else {
            holder.imgPhoto.setImageDrawable(null)
            holder.tvInitial.visibility = View.VISIBLE

            // Get initial from first name
            val initial = contact.firstName.firstOrNull()?.uppercase() ?: "?"
            holder.tvInitial.text = initial

            // Set random background color for the circle
            val backgroundColor = getColorForInitial(initial)
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(backgroundColor)
            holder.imgPhoto.background = drawable
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            onClick(contact)
        }

        // Set long click listener with haptic feedback
        holder.itemView.setOnLongClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongClick(contact)
            true // Consume the event
        }
    }

    override fun getItemCount() = contacts.size

    private fun getColorForInitial(initial: String): Int {
        val colors = listOf(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
            "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E2",
            "#F8B195", "#F67280", "#C06C84", "#6C5B7B"
        )

        val index = initial.hashCode() % colors.size
        return Color.parseColor(colors[index.coerceIn(0, colors.size - 1)])
    }
}