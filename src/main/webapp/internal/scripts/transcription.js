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
            mediaRecorder = new MediaRecorder(stream);
            hasPermissions = true;

            mediaRecorder.ondataavailable = function (event) {
                audioChunks.push(event.data);
            };

            mediaRecorder.onstart = () => {
                isRecording = true;
            }

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunks, {type: 'audio/webm'});

                await handleTranscribe(audioBlob);

                // Clear for next recording
                audioChunks = [];
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
    if (mediaRecorder && hasPermissions) {
        mediaRecorder.start();
    }
}

function stopRecording() {
    if (mediaRecorder && hasPermissions) {
        mediaRecorder.stop();
    }
}

function handleRecorderBtn() {
    const voiceBtn = document.getElementById("voiceButton");

    if (!isRecording) {
        startRecording();
        showPointingToast(voiceBtn, "Listening for your answer... Tap again to save it!");
    } else {
        stopRecording();
        showPointingToast(voiceBtn, "Recording saved!");
    }
}

async function handleTranscribe(audio) {
    // get questionId from DOM
    const questionId = document.getElementById("questionId").value;
    const playerAnswerKey = await transcribeAnswer(questionId, audio);

    handleAnswerSelect(playerAnswerKey);
}

// need function for sending recorded audio
async function transcribeAnswer(questionId, audio) {
    const contextPath = getContextPath();
    const formData = new FormData();
    formData.append("question_id", questionId);
    formData.append("file", audio, "recording.webm")

    try {
        const res = await sendMultipartFormData(`${contextPath}/whisper/transcribe-answer`, formData);

        return res.playerAnswerKey;
    } catch (errorMsg) {
        // user feedback with errorMsg in DOM
    }
}

// need function for selecting appropriate answer based on whisper response
function handleAnswerSelect(playerAnswerKey) {
    const btn = document.querySelector(`button[data-key="${playerAnswerKey}"]`);
    if (btn) btn.click();
}

init();