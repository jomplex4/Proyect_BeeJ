import numpy as np, soundfile as sf, re, json, time, sys
from kokoro_onnx import Kokoro
from asr import transcribe
PH={"A":"ˈeɪ","B":"bˈi","C":"sˈi","D":"dˈi","E":"ˈi","F":"ˈɛf","G":"ʤˈi","H":"ˈAʧ","I":"ˈI","J":"ʤˈA","K":"kˈA","L":"ˈɛl","M":"ˈɛm","N":"ˈɛn","O":"ˈO","P":"pˈi","Q":"kjˈu","R":"ˈɑɹ","S":"ˈɛs","T":"tˈi","U":"jˈu","V":"vˈi","W":"dˈʌbəlju","X":"ˈɛks","Y":"wˈI","Z":"zˈi"}
WORDS="SINGER LAWYER ALWAYS WAITER REALLY PEOPLE DOCTOR PILOT SUGAR COFFEE MUSICIAN BEAUTIFUL EXCITED EXPENSIVE NORMALLY POLICEMAN RECORDING CAREFUL PINEAPPLE DANGEROUS WATERMELON INTERESTING UNCOUNTABLE PARTICIPATION PAPARAZZO DICTIONARY PRONUNCIATION COMPREHENSION COMMUNITY INTELLIGENT".split()
V="am_adam"; k=Kokoro("kokoro-v1.0.onnx","voices-v1.0.bin"); t0=time.time(); rep={}
def norm(a):
    rms=np.sqrt(np.mean(a**2)); a=a*(0.1/max(rms,1e-6)); pk=np.abs(a).max()
    if pk>0.89: a=a*(0.89/pk)
    return a.astype(np.float32)
import os
for L,p in PH.items():
    if os.path.exists(f"raw/l_{L.lower()}.wav"): continue
    a,sr=k.create(p,voice=V,speed=1.0,is_phonemes=True); sf.write(f"raw/l_{L.lower()}.wav",norm(a),sr); print("letter",L,f"{len(a)/sr:.2f}s t={time.time()-t0:.0f}",flush=True)
a,sr=k.create("double",voice=V,speed=1.0,lang="en-us"); sf.write("raw/double.wav",norm(a),sr); print("double",transcribe(a,sr),flush=True)
for w in WORDS:
    if os.path.exists(f"raw/w_{w.lower()}.wav") and w not in ("NORMALLY","POLICEMAN"): continue
    a,sr=k.create(w.lower(),voice=V,speed=1.0,lang="en-us"); sf.write(f"raw/w_{w.lower()}.wav",norm(a),sr)
    t=transcribe(a,sr); rep[w]=t; print("word",w,repr(t),f"{len(a)/sr:.2f}s t={time.time()-t0:.0f}",flush=True)
json.dump(rep,open("raw/word_asr.json","w"),indent=1); print("DONE")
