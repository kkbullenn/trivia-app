import {getContextPath, sendMultipartFormData} from "./helpers.js"
import {showPointingToast} from "../components/toast/toast.js";

let mediaRecorder;
let audioChunks = [];

let hasPermissions = false;
let isRecording = false;

function init() {
    setUpRecorder();
    setUpVoiceBtn();
}

// need function for getting permissions and setting mediaDevices listeners
function setUpRecorder() {
    navigator.mediaDevices.getUserMedia({audio: true, video: false})
        .then(function (stream) {
            hasPermissions = true;
            mediaRecorder = new MediaRecorder(stream);

            mediaRecorder.ondataavailable = function (event) {
                audioChunks.push(event.data);
            };

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunks, {type: 'audio/webm'});
                const audioUrl = URL.createObjectURL(audioBlob);
                const audio = new Audio(audioUrl);

                console.log("Got audio!")

                // // --- DOWNLOAD LOCALLY ---
                // const a = document.createElement("a");
                // a.href = audioUrl;
                // a.download = "recording.webm";   // File name
                // document.body.appendChild(a);
                // a.click();
                // document.body.removeChild(a);
                // // -------------------------
                //
                // console.log("Saved locally!");

                await handleTranscribe(audio);

                // Clear for next recording
                audioChunks = [];
                mediaRecorder = undefined;
                hasPermissions = false;
                isRecording = false;
            };
        })
        .catch(function (err) {
            console.error('Error accessing microphone:', err);
        });
}

function setUpVoiceBtn() {
    const voiceBtn = document.getElementById("voiceButton");

    voiceBtn.addEventListener("click", (event) => handleRecorderBtn())
}

function startRecording() {
    if (mediaRecorder && !isRecording) {
        mediaRecorder.start();
        isRecording = true;
    }
}

function stopRecording() {
    if (mediaRecorder && isRecording) {
        mediaRecorder.stop();
        isRecording = false;
    }
}

function handleRecorderBtn() {
    const voiceBtn = document.getElementById("voiceButton");

    showPointingToast(voiceBtn, "Listening for your answer... Tap again to save it!");

    if (hasPermissions && !isRecording) {
        startRecording();
    } else {
        stopRecording();
    }
}

async function handleTranscribe(audio) {
    // get questionId from DOM
    const questionId = 1;
    const playerAnswerKey = await transcribeAnswer(questionId, audio);

    handleAnswerSelect(playerAnswerKey);
}

// need function for sending recorded audio
async function transcribeAnswer(questionId, audio) {
    const contextPath = getContextPath();
    const formData = new FormData();
    formData.append("questionId", questionId);
    formData.append("file", audio)

    try {
        const res = await sendMultipartFormData(`${contextPath}/whisper/transcribe-answer`, formData);

        return res.playerAnswerKey;
    } catch (errorMsg) {
        // user feedback with errorMsg in DOM
    }
}

// need function for selecting appropriate answer based on whisper response
function handleAnswerSelect(playerAnswerKey) {
    // select answer in the DOM
}

init();