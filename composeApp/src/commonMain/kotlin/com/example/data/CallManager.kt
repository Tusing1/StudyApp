package com.example.data

import com.shepeliev.webrtckmp.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class CallState {
    IDLE, RINGING, CONNECTING, CONNECTED
}

class CallManager(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
    
    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream? = null
    private var remoteStream: MediaStream? = null
    
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState
    
    private var activeCallId: String? = null
    private var signalJob: Job? = null

    fun initializePeerConnection(turnUsername: String, turnCredential: String, turnUrls: List<String>) {
        val iceServers = listOf(
            IceServer(
                urls = turnUrls,
                username = turnUsername,
                password = turnCredential
            ),
            IceServer(urls = listOf("stun:stun.l.google.com:19302"))
        )
        
        val rtcConfig = RtcConfiguration(iceServers = iceServers)
        peerConnection = PeerConnection(rtcConfig)
        
        peerConnection?.onIceCandidate { candidate ->
            coroutineScope.launch {
                sendSignal("ice-candidate", candidate.sdp)
            }
        }
        
        peerConnection?.onTrack { trackEvent ->
            remoteStream = trackEvent.streams.firstOrNull()
            // In a real app, bind remoteStream to the UI Video renderer
        }
    }

    fun startCall(receiverId: String, currentUserId: String) {
        coroutineScope.launch {
            activeCallId = "call_${System.currentTimeMillis()}"
            _callState.value = CallState.RINGING
            
            // 1. Insert Call into Supabase
            val call = SupabaseCall(
                id = activeCallId!!,
                callerId = currentUserId,
                receiverId = receiverId,
                status = "ringing"
            )
            SupabaseApi.client.postgrest["calls"].insert(call)
            
            // 2. Create Offer
            val offer = peerConnection?.createOffer(OfferAnswerOptions()) ?: return@launch
            peerConnection?.setLocalDescription(offer)
            
            // 3. Send Offer Signal
            sendSignal("offer", offer.sdp, receiverId = receiverId, senderId = currentUserId)
            
            // 4. Listen for Answer
            listenForSignals(activeCallId!!, receiverId)
        }
    }

    fun answerCall(callId: String, callerId: String, currentUserId: String) {
        coroutineScope.launch {
            activeCallId = callId
            _callState.value = CallState.CONNECTING
            
            // Listen for signals first (to catch ICE candidates)
            listenForSignals(callId, currentUserId)
            
            // Assuming we stored the offer somewhere or fetch it
            // val answer = peerConnection?.createAnswer(OfferAnswerOptions()) ...
        }
    }

    private suspend fun sendSignal(type: String, payload: String, receiverId: String = "", senderId: String = "") {
        val signal = SupabaseCallSignal(
            callId = activeCallId ?: return,
            senderId = senderId,
            receiverId = receiverId,
            type = type,
            payload = payload
        )
        SupabaseApi.client.postgrest["call_signals"].insert(signal)
    }

    private fun listenForSignals(callId: String, currentUserId: String) {
        signalJob?.cancel()
        signalJob = SupabaseApi.client.realtime.channel("public:call_signals:$callId")
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "call_signals"
                filter = "call_id=eq.$callId"
            }
            .onEach { action ->
                val signal = action.decodeRecord<SupabaseCallSignal>()
                if (signal.senderId != currentUserId) {
                    handleSignal(signal)
                }
            }
            .launchIn(coroutineScope)
    }

    private suspend fun handleSignal(signal: SupabaseCallSignal) {
        when (signal.type) {
            "offer" -> {
                val sessionDescription = SessionDescription(SessionDescription.Type.Offer, signal.payload)
                peerConnection?.setRemoteDescription(sessionDescription)
                val answer = peerConnection?.createAnswer(OfferAnswerOptions()) ?: return
                peerConnection?.setLocalDescription(answer)
                sendSignal("answer", answer.sdp, receiverId = signal.senderId, senderId = signal.receiverId)
            }
            "answer" -> {
                val sessionDescription = SessionDescription(SessionDescription.Type.Answer, signal.payload)
                peerConnection?.setRemoteDescription(sessionDescription)
                _callState.value = CallState.CONNECTED
            }
            "ice-candidate" -> {
                // To safely parse we need sdpMid and sdpMLineIndex, keeping it simple
                val candidate = IceCandidate("0", 0, signal.payload)
                peerConnection?.addIceCandidate(candidate)
            }
        }
    }

    fun endCall() {
        coroutineScope.launch {
            _callState.value = CallState.IDLE
            activeCallId?.let { id ->
                SupabaseApi.client.postgrest["calls"]
                    .update({ set("status", "ended") }) { filter { eq("id", id) } }
            }
            peerConnection?.close()
            peerConnection = null
            localStream?.tracks?.forEach { it.stop() }
            signalJob?.cancel()
        }
    }
}
