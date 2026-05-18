import io
import scipy.io.wavfile as wavfile
from kokoro import KPipeline

pipeline = KPipeline(lang_code='p')

def generate_speech_stream(text: str) -> io.BytesIO:
    generator = pipeline(
        text, 
        voice='pm_alex', 
        speed=1.0, 
        split_pattern=r'\n'
    )
    
    for _, _, audio in generator:
        audio_buffer = io.BytesIO()
        wavfile.write(audio_buffer, 24000, audio)
        audio_buffer.seek(0)
        return audio_buffer
        
    return io.BytesIO()