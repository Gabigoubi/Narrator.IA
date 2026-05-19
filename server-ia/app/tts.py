import io
import scipy.io.wavfile as wavfile
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
    
    for _, _, audio in generator:
        audio_numpy = audio.numpy()
        audio_buffer = io.BytesIO()
        wavfile.write(audio_buffer, 24000, audio_numpy)
        audio_buffer.seek(0)
        return audio_buffer
        
    return io.BytesIO()
