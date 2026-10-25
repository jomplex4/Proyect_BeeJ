import sherpa_onnx, soundfile as sf, numpy as np
_rec=None
def rec():
    global _rec
    if _rec is None:
        d="sherpa-onnx-whisper-small.en/"
        _rec=sherpa_onnx.OfflineRecognizer.from_whisper(encoder=d+"small.en-encoder.int8.onnx",decoder=d+"small.en-decoder.int8.onnx",tokens=d+"small.en-tokens.txt",num_threads=4,language="en",task="transcribe")
    return _rec
def transcribe(samples, sr):
    if sr!=16000:
        import scipy.signal as ss
        samples=ss.resample_poly(samples,16000,sr)
    pad=np.zeros(int(16000*0.3),dtype=np.float32)
    samples=np.concatenate([pad,samples.astype(np.float32),pad])
    r=rec(); s=r.create_stream(); s.accept_waveform(16000,samples); r.decode_stream(s)
    return s.result.text.strip()
