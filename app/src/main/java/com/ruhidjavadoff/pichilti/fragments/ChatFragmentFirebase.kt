package com.ruhidjavadoff.pichilti.fragments

import androidx.core.net.toUri
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.ruhidjavadoff.pichilti.Message
import com.ruhidjavadoff.pichilti.MessageType
import com.ruhidjavadoff.pichilti.TrackStatus
import java.io.File
import kotlin.random.Random

fun ChatFragment.setupFirebaseListeners() {
    listenForMessages()
    listenForParticipantChanges()
    managePresence()
}

fun ChatFragment.removeFirebaseListeners() {
    messagesRef.removeEventListener(chatListener)
    participantsRef.removeEventListener(participantListener)
    participantsRef.child(currentUsername).removeValue()
}

private fun ChatFragment.listenForMessages() {
    chatListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val message = snapshot.getValue(Message::class.java)
            if (message != null) {
                if (messageList.any { it.messageId == message.messageId }) return

                checkAndUpdateLocalFileStatus(message)

                if(chatAdapter != null) {
                    messageList.add(message)
                    chatAdapter?.notifyItemInserted(messageList.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        // DÜZƏLİŞ BURADADIR: Silinən mesajları izləyən məntiq
        override fun onChildRemoved(snapshot: DataSnapshot) {
            val removedMessage = snapshot.getValue(Message::class.java)
            if (removedMessage != null) {
                val index = messageList.indexOfFirst { it.messageId == removedMessage.messageId }
                if (index != -1) {
                    messageList.removeAt(index)
                    chatAdapter?.notifyItemRemoved(index)
                }
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }
    messagesRef.addChildEventListener(chatListener)
}

private fun ChatFragment.checkAndUpdateLocalFileStatus(message: Message) {
    if (message.senderId == this.currentUsername) {
        when (MessageType.valueOf(message.type)) {
            MessageType.MUSIC -> message.musicTrack?.status = TrackStatus.READY
            MessageType.FILE -> message.fileMessage?.status = TrackStatus.READY
            MessageType.IMAGE -> message.imageMessage?.status = TrackStatus.READY
            MessageType.VIDEO -> message.videoMessage?.status = TrackStatus.READY
            else -> {}
        }
        return
    }

    val context = this.context ?: return
    var localFile: File? = null

    when (MessageType.valueOf(message.type)) {
        MessageType.MUSIC -> {
            localFile = File(context.filesDir, "${message.messageId}.mp3")
            if (localFile.exists()) {
                message.musicTrack?.localUri = localFile.toUri().toString()
                message.musicTrack?.status = TrackStatus.READY
            }
        }
        MessageType.FILE -> {
            message.fileMessage?.let {
                localFile = File(context.filesDir, it.fileName)
                if (localFile.exists()) {
                    it.localUri = localFile.toUri().toString()
                    it.status = TrackStatus.READY
                }
            }
        }
        MessageType.IMAGE -> {
            localFile = File(context.filesDir, "${message.messageId}.jpg")
            if (localFile.exists()) {
                message.imageMessage?.localUri = localFile.toUri().toString()
                message.imageMessage?.status = TrackStatus.READY
            }
        }
        MessageType.VIDEO -> {
            localFile = File(context.filesDir, "${message.messageId}.mp4")
            if (localFile.exists()) {
                message.videoMessage?.localUri = localFile.toUri().toString()
                message.videoMessage?.status = TrackStatus.READY
            }
        }
        else -> {}
    }
}


private fun ChatFragment.listenForParticipantChanges() {
    participantListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val realParticipantCount = snapshot.childrenCount.toInt()
            updateParticipantCount(realParticipantCount)
        }
        override fun onCancelled(error: DatabaseError) {}
    }
    participantsRef.addValueEventListener(participantListener)
}

private fun ChatFragment.managePresence() {
    val userPresenceRef = participantsRef.child(currentUsername)
    userPresenceRef.setValue(true)
    userPresenceRef.onDisconnect().removeValue()
}

fun ChatFragment.sendMessageToFirebase(text: String) {
    val messageId = messagesRef.push().key!!
    val finalMessage = Message(
        messageId = messageId,
        text = text,
        senderId = currentUsername,
        senderName = currentUsername,
        timestamp = System.currentTimeMillis(),
        type = MessageType.TEXT.name
    )
    messagesRef.child(messageId).setValue(finalMessage)
}

private fun ChatFragment.updateParticipantCount(realCount: Int) {
    val countText = if (realCount < 3) {
        val thirtyMinutesInMillis = 30 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        if (currentTime - room.fakeParticipantTimestamp > thirtyMinutesInMillis) {
            val newBase = Random.nextInt(5, 16)
            val updates = mapOf("fakeParticipantBase" to newBase, "fakeParticipantTimestamp" to currentTime)
            roomRef.updateChildren(updates)
            room = room.copy(fakeParticipantBase = newBase, fakeParticipantTimestamp = currentTime)
            "${newBase + realCount} members"
        } else {
            "${room.fakeParticipantBase + realCount} members"
        }
    } else {
        "$realCount members"
    }
    binding.chatHeader.headerParticipantCount.text = countText
}
