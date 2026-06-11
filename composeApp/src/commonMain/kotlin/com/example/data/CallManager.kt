package com.example.data

import com.shepeliev.webrtckmp.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.decodeRecord
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class CallState {
    IDLE, RINGING, CONNECTING, CONNECTED
}

class CallManager(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
    
    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream? = null
    private var remoteStream: MediaStream? = null
    
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState
    
    private val _localStreamFlow = MutableStateFlow<MediaStream?>(null)
    val localStreamFlow: StateFlow<MediaStream?> = _localStreamFlow

    private val _remoteStreamFlow = MutableStateFlow<MediaStream?>(null)
    val remoteStreamFlow: StateFlow<MediaStream?> = _remoteStreamFlow

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    var activeCallId: String? = null
        private set
    
    private var signalJob: Job? = null
    private var myUserId: String? = null

    fun initializePeerConnection(turnUsername: String, turnCredential: String, turnUrls: List<String>, currentUserId: String) {
        myUserId = currentUserId
        val iceServers = if (turnUrls.isNotEmpty()) {
            listOf(
                IceServer(
                    urls = turnUrls,
                    username = turnUsername,
                    password = turnCredential
                ),
                IceServer(urls = listOf("stun:stun.l.google.com:19302"))
            )
        } else {
            listOf(
                IceServer(urls = listOf("stun:stun.l.google.com:19302"))
            )
        }
        
        val rtcConfig = RtcConfiguration(iceServers = iceServers)
        peerConnection = PeerConnection(rtcConfig)
        
        peerConnection?.onIceCandidate?.onEach { candidate ->
            coroutineScope.launch {
                myUserId?.let { uid ->
                    val candidateJson = buildJsonObject {
                        put("candidate", JsonPrimitive(candidate.candidate))
                        put("sdpMid", JsonPrimitive(candidate.sdpMid))
                        put("sdpMLineIndex", JsonPrimitive(candidate.sdpMLineIndex))
                    }.toString()
                    sendSignal("ice-candidate", candidateJson, senderId = uid)
                }
            }
        }?.launchIn(coroutineScope)
        
        peerConnection?.onTrack?.onEach { trackEvent ->
            val rStream = trackEvent.streams.firstOrNull()
            remoteStream = rStream
            _remoteStreamFlow.value = rStream
            _callState.value = CallState.CONNECTED
        }?.launchIn(coroutineScope)
    }

    private fun ensurePeerConnectionInitialized(currentUserId: String) {
        if (peerConnection == null) {
            initializePeerConnection("", "", emptyList(), currentUserId)
        }
    }

    fun startCall(conversationId: String, currentUserId: String, callType: String = "voice") {
        myUserId = currentUserId
        ensurePeerConnectionInitialized(currentUserId)
        coroutineScope.launch {
            try {
                // Initialize local stream
                val stream = MediaDevices.getUserMedia(
                    audio = true,
                    video = callType == "video"
                )
                localStream = stream
                _localStreamFlow.value = stream
                
                stream.tracks.forEach { track ->
                    peerConnection?.addTrack(track, stream)
                }

                activeCallId = "call_${System.currentTimeMillis()}"
                _callState.value = CallState.RINGING
                
                // 1. Insert Call into Supabase
                val call = SupabaseCall(
                    id = activeCallId!!,
                    conversationId = conversationId,
                    startedBy = currentUserId,
                    callType = callType,
                    isActive = true
                )
                SupabaseApi.client.postgrest["calls"].insert(call)
                
                // 2. Create Offer
                val offer = peerConnection?.createOffer(OfferAnswerOptions()) ?: return@launch
                peerConnection?.setLocalDescription(offer)
                
                // 3. Send Offer Signal
                sendSignal("offer", offer.sdp, senderId = currentUserId)
                
                // 4. Listen for Answer / Signals
                listenForSignals(activeCallId!!, currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
                _callState.value = CallState.IDLE
            }
        }
    }

    fun answerCall(callId: String, currentUserId: String) {
        myUserId = currentUserId
        ensurePeerConnectionInitialized(currentUserId)
        coroutineScope.launch {
            try {
                activeCallId = callId
                _callState.value = CallState.CONNECTING
                
                // Initialize local stream
                val stream = MediaDevices.getUserMedia(
                    audio = true,
                    video = false
                )
                localStream = stream
                _localStreamFlow.value = stream
                
                stream.tracks.forEach { track ->
                    peerConnection?.addTrack(track, stream)
                }

                // Listen for signals first (to catch ICE candidates)
                listenForSignals(callId, currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
                _callState.value = CallState.IDLE
            }
        }
    }

    private suspend fun sendSignal(type: String, payload: String, senderId: String) {
        val callId = activeCallId ?: return
        
        val signalData = if (type == "ice-candidate") {
            try {
                Json.parseToJsonElement(payload)
            } catch (e: Exception) {
                buildJsonObject {
                    put("candidate", JsonPrimitive(payload))
                    put("sdpMid", JsonPrimitive("0"))
                    put("sdpMLineIndex", JsonPrimitive(0))
                }
            }
        } else {
            buildJsonObject {
                put("sdp", JsonPrimitive(payload))
                put("type", JsonPrimitive(if (type == "offer") "offer" else "answer"))
            }
        }

        val signal = SupabaseCallSignal(
            callId = callId,
            fromUser = senderId,
            toUser = null,
            signalType = type,
            signalData = signalData
        )
        SupabaseApi.client.postgrest["call_signals"].insert(signal)
    }

    private fun listenForSignals(callId: String, currentUserId: String) {
        signalJob?.cancel()
        signalJob = SupabaseApi.client.channel("public:call_signals:$callId")
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "call_signals"
                filter("call_id", FilterOperator.EQ, callId)
            }
            .onEach { action ->
                val signal = action.decodeRecord<SupabaseCallSignal>()
                if (signal.fromUser != currentUserId) {
                    handleSignal(signal)
                }
            }
            .launchIn(coroutineScope)
    }

    private suspend fun handleSignal(signal: SupabaseCallSignal) {
        val data = signal.signalData
        val currentUid = myUserId ?: return
        when (signal.signalType) {
            "offer" -> {
                val sdp = data.jsonObject["sdp"]?.jsonPrimitive?.content ?: ""
                val sessionDescription = SessionDescription(SessionDescriptionType.Offer, sdp)
                peerConnection?.setRemoteDescription(sessionDescription)
                
                val answer = peerConnection?.createAnswer(OfferAnswerOptions()) ?: return
                peerConnection?.setLocalDescription(answer)
                
                sendSignal(type = "answer", payload = answer.sdp, senderId = currentUid)
            }
            "answer" -> {
                val sdp = data.jsonObject["sdp"]?.jsonPrimitive?.content ?: ""
                val sessionDescription = SessionDescription(SessionDescriptionType.Answer, sdp)
                peerConnection?.setRemoteDescription(sessionDescription)
                _callState.value = CallState.CONNECTED
            }
            "ice-candidate" -> {
                val candidateObj = data.jsonObject
                val candidateStr = candidateObj["candidate"]?.jsonPrimitive?.content ?: ""
                val sdpMid = candidateObj["sdpMid"]?.jsonPrimitive?.content ?: "0"
                val sdpMLineIndex = candidateObj["sdpMLineIndex"]?.jsonPrimitive?.intOrNull ?: 0
                val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                peerConnection?.addIceCandidate(candidate)
            }
        }
    }

    fun toggleMute() {
        val nextMuted = !_isMuted.value
        _isMuted.value = nextMuted
        localStream?.audioTracks?.forEach { it.enabled = !nextMuted }
    }

    fun toggleCamera() {
        val nextCamOff = !_isCameraOff.value
        _isCameraOff.value = nextCamOff
        localStream?.videoTracks?.forEach { it.enabled = !nextCamOff }
    }

    fun toggleRecording(title: String) {
        val nextRecording = !_isRecording.value
        _isRecording.value = nextRecording
        coroutineScope.launch {
            try {
                activeCallId?.let { id ->
                    SupabaseApi.client.postgrest["calls"]
                        .update({
                            set("is_recording", nextRecording)
                            set("recording_title", title)
                        }) {
                            filter { eq("id", id) }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun endCall() {
        coroutineScope.launch {
            _callState.value = CallState.IDLE
            activeCallId?.let { id ->
                try {
                    SupabaseApi.client.postgrest["calls"]
                        .update({ 
                            set("is_active", false)
                            set("ended_at", System.currentTimeMillis().toString()) 
                        }) { 
                            filter { eq("id", id) } 
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            peerConnection?.close()
            peerConnection = null
            localStream?.tracks?.forEach { it.stop() }
            localStream = null
            _localStreamFlow.value = null
            remoteStream = null
            _remoteStreamFlow.value = null
            signalJob?.cancel()
            _isMuted.value = false
            _isCameraOff.value = false
            _isRecording.value = false
        }
    }

    fun raiseHand(raised: Boolean) {
        coroutineScope.launch {
            try {
                val callId = activeCallId ?: return@launch
                val uid = myUserId ?: return@launch
                SupabaseApi.client.postgrest["call_participants"]
                    .update({
                        set("hand_raised", raised)
                    }) {
                        filter {
                            eq("call_id", callId)
                            eq("user_id", uid)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun muteParticipant(userId: String, isMuted: Boolean) {
        coroutineScope.launch {
            try {
                val callId = activeCallId ?: return@launch
                SupabaseApi.client.postgrest["call_participants"]
                    .update({
                        set("is_muted", isMuted)
                    }) {
                        filter {
                            eq("call_id", callId)
                            eq("user_id", userId)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
