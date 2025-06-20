package com.example.thinkaboutit

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class SessionFullLoadingActivity : AppCompatActivity() {
    private lateinit var stateText: TextView
    private lateinit var queueStatusText: TextView
    private lateinit var gameStateListener: ValueEventListener
    private lateinit var queueListener: ValueEventListener
    private lateinit var episodeListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_full_loading)

        stateText = findViewById(R.id.session_full_state_text)

        // Listen for currentEpisode changes (for non-hosts)
        episodeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // When currentEpisode changes, re-initialize and go to NameCreationActivity
                ServiceManager.Instance.checkIsHost { isHost ->
                    if (!isHost) {
                        ServiceManager.Instance.initializeComponents {
                            val intent = Intent(this@SessionFullLoadingActivity, NameCreationActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ServiceManager.Instance.database.reference.child("currentEpisode").addValueEventListener(episodeListener)

        // Use a ValueEventListener to update the state text live
        gameStateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stateName = snapshot.value?.toString() ?: "Unknown"
                runOnUiThread {
                    stateText.text = "Game in progress: $stateName"
                }
                if(stateName == "Unknown")
                {
                    val intent = Intent(this@SessionFullLoadingActivity, NameCreationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ServiceManager.Instance.episodeRef.child("gameState").addValueEventListener(gameStateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.Instance.episodeRef.child("gameState").removeEventListener(gameStateListener)
        ServiceManager.Instance.database.reference.child("currentEpisode").removeEventListener(episodeListener)
    }
} 