import json, sys, glob, os, re, collections, difflib
import numpy as np, soundfile as sf, scipy.signal as ss
from vosk import Model, KaldiRecognizer, SetLogLevel
SetLogLevel(-1)
M=Model("vad/models/src/main/assets/model-en-us")
V={'A':["a","ay","ey"],'B':["b","bee","be"],'C':["c","see","sea","cee"],'D':["d","dee"],'E':["e","ee"],'F':["f","ef","eff"],'G':["g","gee"],'H':["h","aitch"],'I':["i","eye","aye"],'J':["j","jay"],'K':["k","kay"],'L':["l","el","ell"],'M':["m","em"],'N':["n","en"],'O':["o","oh","owe"],'P':["p","pee","pea"],'Q':["q","queue","cue"],'R':["r","are","ar"],'S':["s","es","ess"],'T':["t","tea","tee"],'U':["u","you","yew"],'V':["v","vee"],'W':["w"],'X':["x","ex"],'Y':["y","why","wye"],'Z':["z","zee","zed"]}
TOK={t:L for L,ts in V.items() for t in ts}
def load16(path):
    a,sr=sf.read(path); a=a.astype(np.float32)
    if sr!=16000: a=ss.resample_poly(a,16000,sr)
    a=np.concatenate([np.zeros(8000),a,np.zeros(16000)])
    return (np.clip(a,-1,1)*32767).astype('<i2').tobytes()
def recog(pcm,grammar,nbest=0):
    r=KaldiRecognizer(M,16000,json.dumps(grammar) if grammar else None) if grammar else KaldiRecognizer(M,16000)
    r.SetWords(True)
    if nbest: r.SetMaxAlternatives(nbest)
    out=[]
    for i in range(0,len(pcm),3200):
        if r.AcceptWaveform(pcm[i:i+3200]): out.append(json.loads(r.Result()))
    out.append(json.loads(r.FinalResult()))
    return out
def tokens(res):
    t=[]
    for o in res:
        if "result" in o: t+=[(w["word"],w.get("conf",1)) for w in o["result"]]
        elif "alternatives" in o: pass
    return t
def spelled(toks):
    s=""
    for w,c in toks:
        s+=TOK.get(w,"?") if w!="double" else "?"
    return s
def dist(a,b):
    sm=difflib.SequenceMatcher(None,a,b,autojunk=False); return sm
if __name__=="__main__":
    grammars={"actual":list(TOK)+["double","[unk]"],"abierta":None}
    files=sorted(glob.glob("testset/*.wav"))
    for gname,g in grammars.items():
        res=collections.defaultdict(lambda:[0,0]); conf=collections.Counter()
        for f in files:
            v,w=os.path.basename(f)[:-4].split("_",1) if os.path.basename(f).count("_")==1 else (None,None)
            name=os.path.basename(f)[:-4]; v=name.rsplit("_",1)[0]; w=name.rsplit("_",1)[1]
            toks=tokens(recog(load16(f),g)); s=spelled(toks)
            ok=(s==w); res[v][0]+=ok; res[v][1]+=1
            sm=difflib.SequenceMatcher(None,w,s,autojunk=False)
            for tag,i1,i2,j1,j2 in sm.get_opcodes():
                if tag=="replace": conf[(w[i1:i2],s[j1:j2])]+=1
                elif tag=="delete": conf[(w[i1:i2],"-")]+=1
                elif tag=="insert": conf[("+",s[j1:j2])]+=1
        print("==",gname,{v:f"{a}/{n}" for v,(a,n) in res.items()})
        print("  confusiones top:",conf.most_common(18),flush=True)
