import io
import scipy.io.wavfile as wavfile
import numpy as np
from kokoro import KPipeline

# --- CONSTANTS ---
SAMPLE_RATE = 24000
EDSON_VOICE_MODEL = "pm_alex" # The one and only definitive voice
SPEECH_SPEED = 0.85

print("[BOOT] ⏳ Loading Kokoro TTS Engine into memory...")
try:
    pipeline = KPipeline(lang_code='p')
    print("[BOOT] ✔️ TTS Engine ready.")
except Exception as e:
    print(f"\n[BOOT FATAL] ❌ Failed to load Kokoro Pipeline: {str(e)}")
    import sys
    sys.exit(1)

def generate_speech_stream(text: str) -> io.BytesIO:
    """
    Synthesizes text into a 16-bit PCM WAV using the definitive Edson voice.
    """
    audio_chunks = []

    try:
        generator = pipeline(
            text, 
            voice=EDSON_VOICE_MODEL, # Strictly locked
            speed=SPEECH_SPEED,  
            split_pattern=r'\n'
        )

        for _, _, audio_tensor in generator:
            if audio_tensor is not None:
                audio_chunks.append(audio_tensor.numpy())

        if not audio_chunks:
            raise ValueError("TTS engine produced empty audio chunks.")

        final_audio = np.concatenate(audio_chunks)
        
        buffer = io.BytesIO()
        wavfile.write(buffer, SAMPLE_RATE, final_audio)
        buffer.seek(0)
        
        return buffer

    except Exception as e:
        print(f"\n[TTS ERROR] ❌ Failed to synthesize speech: {str(e)}")
        return _create_silent_wav()

def _create_silent_wav() -> io.BytesIO:
    buffer = io.BytesIO()
    silent_audio = np.zeros(SAMPLE_RATE, dtype=np.float32)
    wavfile.write(buffer, SAMPLE_RATE, silent_audio)
    buffer.seek(0)
    return buffer
