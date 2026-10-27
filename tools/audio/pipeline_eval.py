import json, glob, os, subprocess, time, sys
from vosk import Model, KaldiRecognizer, SetLogLevel
import numpy as np, soundfile as sf, scipy.signal as ss
SetLogLevel(-1)
M=Model("vad/models/src/main/assets/model-en-us")
G={}
for line in open("grammars.tsv",encoding="utf8"):
    w,t,c=line.rstrip("\n").split("\t"); G[w]=(t,c)
V={'A':["a","ay","ey"],'B':["b","bee","be"],'C':["c","see","sea","cee"],'D':["d","dee"],'E':["e","ee"],'F':["f","ef","eff"],'G':["g","gee"],'H':["h","aitch"],'I':["i","eye","aye"],'J':["j","jay"],'K':["k","kay"],'L':["l","el","ell"],'M':["m","em"],'N':["n","en"],'O':["o","oh","owe"],'P':["p","pee","pea"],'Q':["q","queue","cue"],'R':["r","are","ar"],'S':["s","es","ess"],'T':["t","tea","tee"],'U':["u","you","yew"],'V':["v","vee"],'W':["w"],'X':["x","ex"],'Y':["y","why","wye"],'Z':["z","zee","zed"]}
WORDS=[w.lower() for w in G]
FREE=json.dumps(sorted({t for ts in V.values() for t in ts}|{"double"}|set(WORDS))+["[unk]"])
def load16(path,pad_end=1.0):
    a,sr=sf.read(path); a=a.astype(np.float32)
    if sr!=16000: a=ss.resample_poly(a,16000,sr)
    a=np.concatenate([np.zeros(8000),a,np.zeros(int(16000*pad_end))])
    return (np.clip(a,-1,1)*32767).astype('<i2').tobytes()
def decode(pcm,grammar):
    r=KaldiRecognizer(M,16000,grammar); r.SetWords(True); toks=[]
    for i in range(0,len(pcm),3200):
        if r.AcceptWaveform(pcm[i:i+3200]): toks+=json.loads(r.Result()).get("result",[])
    toks+=json.loads(r.FinalResult()).get("result",[]); return toks
def fmt(ts): return ";".join(f'{t["word"]}:{t.get("conf",1):.2f}:{t["start"]:.2f}:{t["end"]:.2f}' for t in ts)
def build(files,which,frame):
    lines=[]; t0=time.time()
    for f,w in files:
        pcm=load16(f); forced=decode(pcm,G[w][1 if frame else 0]); free=decode(pcm,FREE)
        lines.append(f"{w}|{1 if frame else 0}|{fmt(forced)}|{fmt(free)}")
    return lines,(time.time()-t0)/max(1,len(files))
def judge(lines,tag):
    open(f"j_{tag}.in","w",encoding="utf8").write("\n".join(lines)+"\n")
    subprocess.run(["java","-cp","tool.jar","ToolKt","judge",f"j_{tag}.in",f"j_{tag}.out"],check=True)
    return [l.split("|") for l in open(f"j_{tag}.out",encoding="utf8").read().splitlines()]
if __name__=="__main__":
    ok=[(f,os.path.basename(f)[:-4].split("_")[2]) for f in sorted(glob.glob("testset/*.wav"))]
    er=[(f,os.path.basename(f)[:-4].split("_")[2]) for f in sorted(glob.glob("errset/*.wav"))]
    lo,t1=build(ok,"ok",False); le,t2=build(er,"er",False)
    ro=judge(lo,"ok"); re_=judge(le,"er")
    from collections import Counter
    byv={}
    for (f,w),r in zip(ok,ro): byv.setdefault(os.path.basename(f).split("_")[0]+"_"+os.path.basename(f).split("_")[1],Counter())[r[0]]+=1
    print("tiempo medio por intento (este PC):",round(t1,2),"s")
    print("CORRECTOS (deben salir CORRECT):",dict(Counter(r[0] for r in ro)))
    for v,c in byv.items(): print("   ",v,dict(c))
    print("ERRORES (no deben salir CORRECT):",dict(Counter(r[0] for r in re_)))
    bad=[(os.path.basename(f)[:-4],r) for (f,w),r in zip(er,re_) if r[0]=="CORRECT"]
    print("   falsos aceptados:",[b[0] for b in bad])
    json.dump({"ok":ro,"er":re_},open("pipeline_result.json","w"))
