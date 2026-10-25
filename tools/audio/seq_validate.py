import numpy as np, soundfile as sf, random, re, sys, json, difflib, time
from asr import transcribe
def load(prefix,L):
    a,sr=sf.read(f"{prefix}_{L}.wav"); return a.astype(np.float32),sr
def clean(t):
    t=t.upper().replace("DOUBLE YOU","W").replace("DOUBLE U","W").replace("DOUBLE-U","W")
    return re.sub(r"[^A-Z]","",t)
def run(prefix,ntrials=2,gap=0.45,seed=7):
    letters=[chr(65+i) for i in range(26)]
    stats={L:[0,0] for L in letters}; t0=time.time(); fails=[]
    for tr in range(ntrials):
        rnd=random.Random(seed+tr); order=letters[:]; rnd.shuffle(order)
        groups=[order[i:i+5] for i in range(0,25,5)]+[[order[25],order[0],order[7],order[13]]]
        for g in groups:
            clips=[]; sr=None
            for L in g:
                a,sr=load(prefix,L); clips+= [a,np.zeros(int(sr*gap),np.float32)]
            txt=transcribe(np.concatenate(clips),sr); got=clean(txt); exp="".join(g)
            sm=difflib.SequenceMatcher(None,exp,got,autojunk=False)
            okidx=set()
            for b in sm.get_matching_blocks():
                for i in range(b.size): okidx.add(b.a+i)
            for i,L in enumerate(g):
                stats[L][1]+=1; stats[L][0]+= i in okidx
            if exp!=got: fails.append((exp,got))
    return stats,fails
if __name__=="__main__":
    prefix=sys.argv[1]; st,fails=run(prefix, int(sys.argv[2]) if len(sys.argv)>2 else 2)
    tot=sum(v[0] for v in st.values()); n=sum(v[1] for v in st.values())
    print(prefix,"per-letter-position ok:",tot,"/",n)
    print("weak:",{L:f"{v[0]}/{v[1]}" for L,v in st.items() if v[0]<v[1]})
    print("fails:",fails[:12])
