import io
import scipy.io.wavfile as wavfile
from kokoro import KPipeline

pipeline = KPipeline(lang_code='p')

def generate_speech_stream(text: str, voice_model: str) -> io.BytesIO:
    generator = pipeline(
        text, 
        voice=voice_model, 
        speed=1.0, 
        split_pattern=r'\n'
    )
    
    for _, _, audio in generator:
        # CONVERSÃO: Transformamos o tensor do PyTorch em um array do NumPy
        audio_numpy = audio.numpy()
        
        audio_buffer = io.BytesIO()
        # Passamos o áudio convertido (audio_numpy) para o wavfile
        wavfile.write(audio_buffer, 24000, audio_numpy)
        audio_buffer.seek(0)
        return audio_buffer
        
    return io.BytesIO()