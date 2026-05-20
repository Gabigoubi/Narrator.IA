import io
import scipy.io.wavfile as wavfile
import numpy as np
from kokoro import KPipeline

pipeline = KPipeline(lang_code='p')

VOICE_MAPPING = {
    "mulher.agressiva": "af_bella",   
    "mulher.amigavel": "af_heart",    
    "homem.agressivo": "am_adam",     
    "homem.amigavel": "pm_alex"       
}

def generate_speech_stream(text: str, persona_id: str) -> io.BytesIO:
    voice_model = VOICE_MAPPING.get(persona_id, "pm_alex")

    generator = pipeline(
        text, 
        voice=voice_model, 
        speed=0.85,  
        split_pattern=r'\n'
    )

    audio_chunks = []
    
    for _, _, audio in generator:
        audio_chunks.append(audio.numpy())

    if not audio_chunks:
        return io.BytesIO()

    final_audio = np.concatenate(audio_chunks)
    
    buffer = io.BytesIO()
    wavfile.write(buffer, 24000, final_audio)
    buffer.seek(0)
    
    return buffer
