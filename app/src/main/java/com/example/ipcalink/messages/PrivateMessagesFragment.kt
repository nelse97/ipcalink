package com.example.ipcalink.messages

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ipcalink.R
import com.example.ipcalink.databinding.FragmentPrivateMessagesBinding
import com.example.ipcalink.models.PrivateUserChat
import com.example.ipcalink.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PrivateMessagesFragment : Fragment() {

    private lateinit var binding: FragmentPrivateMessagesBinding
    var userChats = mutableListOf<PrivateUserChat>()
    private lateinit var db: FirebaseFirestore
    private lateinit var chatsAdapter: PrivateChatsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var authUserUid: String
    var userExistingPrivateChats = ArrayList<String>()
    var c: Calendar = Calendar.getInstance()
    lateinit var dataPasser: OnDataPass

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        binding = FragmentPrivateMessagesBinding.inflate(layoutInflater)

        //instantiate firestore object
        db = FirebaseFirestore.getInstance()

        //get current user uid
        authUserUid = FirebaseAuth.getInstance().currentUser!!.uid

        linearLayoutManager = LinearLayoutManager(activity)
        binding.rvPrivateChats.layoutManager = linearLayoutManager

        chatsAdapter = PrivateChatsAdapter {
            val intent = Intent(activity, PrivateChatActivity::class.java)
            intent.putExtra("chatId", it.chatId)
            intent.putExtra("chatName", it.chatName)
            intent.putExtra("chatType", it.chatType)
            intent.putExtra("chatPhotoUrl", it.photoUrl)
            intent.putExtra("receiverUserId", it.chatUserId)
            startActivity(intent)
        }

        //bind adapter to private chats recycler view
        binding.rvPrivateChats.adapter = chatsAdapter

        /*val newChat = UsersChats("x34mjmazUy4srm97QZhx", "Eduardo", "private", "", "", "",
            null)

        userChats.add(newChat)
        chatsAdapter.notifyDataSetChanged()*/


        // return the fragment layout
        return binding.root
    }

    interface OnDataPass {
        fun onDataPass(data: ArrayList<String>)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataPasser = context as OnDataPass
    }

    fun passData(data: ArrayList<String>){
        dataPasser.onDataPass(data)
    }

    override fun onStart() {
        super.onStart()
        //get list of all user chats
        db.collection("users").document(authUserUid).collection("chats")
            .addSnapshotListener { chats, e ->
                if (e != null) {
                    Toast.makeText(
                        activity,
                        "Ocorreu um erro ao tentar listar todos os seus chats. Tente novamente mais tarde.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("chatsFragment", e.message.toString())
                    return@addSnapshotListener
                }
                userChats.clear()
                for (chat in chats!!) {
                    val newChat = chat.toObject<PrivateUserChat>()
                    userChats.add(newChat)
                }
                Log.d("PrivateMessages", userChats.size.toString())
                if (userChats.size == 0) {
                    noChatsShowNotice()
                } else {
                    //get a list of the users current private chats
                    chatsAdapter.notifyDataSetChanged()
                    verifyCurrentPrivateChats()
                }

            }
    }

    override fun onStop() {
        super.onStop()
        db.clearPersistence()
    }

    private fun verifyCurrentPrivateChats() {
        //clear existing private chats
        userExistingPrivateChats.clear()
        for (userChat in userChats) {
            if (userChat.chatType == "private") {
                db.collection("chats").document(userChat.chatId!!).collection("users")
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        for (document in documentSnapshot) {
                            val chatUser = document.toObject<User>()
                            if (chatUser.userId != authUserUid) {
                                userExistingPrivateChats.add(chatUser.userId)
                            }
                        }
                        passData(userExistingPrivateChats)
                    }
            }
        }
    }

    inner class PrivateChatsAdapter(private val clickListener: (PrivateUserChat) -> Unit) :
            RecyclerView.Adapter<PrivateChatsAdapter.MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val row = LayoutInflater.from(activity).inflate(R.layout.chat_row, parent, false)
            return MyViewHolder(row)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val userChat = userChats[position]

            //set image chat row
            if (userChat.photoUrl!!.isNotEmpty()) {
                try {
                    Glide.with(activity!!).load(userChat.photoUrl).into(holder.chatImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                holder.chatImage.setImageResource(R.drawable.padrao)
            }

            holder.chatTitle.text = userChat.chatName
            holder.chatLastMessage.text = userChat.lastMessage

            if (userChat.lastMessageTimestamp != null) {
                holder.chatRowTime.visibility = View.VISIBLE
                val date = getDate(userChat.lastMessageTimestamp!!.seconds * 1000, "HH:mm")
                holder.chatRowTime.text = date
            } else {
                holder.chatRowTime.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                clickListener(userChats[position])
            }


        }

        override fun getItemCount(): Int {
            return userChats.size
        }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var chatImage: CircleImageView = itemView.findViewById(R.id.rowChatIv)
            var chatUnreadMessagesBackground: ImageView = itemView.findViewById(R.id.rowChatUnreadMessagesBackground)
            var chatUnreadMessagesCount: TextView = itemView.findViewById(R.id.chatRowUnreadMessagesCount)
            var isOnline: ImageView = itemView.findViewById(R.id.ivIsOnlineRowChat)
            var chatTitle: TextView = itemView.findViewById(R.id.rowChatTitle)
            var chatLastMessage: TextView = itemView.findViewById(R.id.rowChatLastMessage)
            var chatRowTime: TextView = itemView.findViewById(R.id.tvChatRowTime)
        }

    }

    private fun checkIfChatIsOnline(userChatId: String) {
        TODO("Not yet implemented")
    }

    private fun getDateTime(s: Long): String? {
        try {
            val sdf = SimpleDateFormat("hh:mm", Locale.ROOT)
            val netDate = Date(s)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    private fun noChatsShowNotice() {
        binding.rvPrivateChats.visibility = View.INVISIBLE
        binding.tvNoChats.visibility = View.VISIBLE
    }

    @SuppressLint("SimpleDateFormat")
    fun getDate(milliSeconds: Long, dateFormat: String?): String {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }
}