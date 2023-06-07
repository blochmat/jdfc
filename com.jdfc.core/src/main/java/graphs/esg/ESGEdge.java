package graphs.esg;

public class ESGEdge {

    int fstIdx;
    ESGNode fst;

    int sndIdx;
    ESGNode snd;

    public ESGEdge(int fstIdx, ESGNode fst, int sndIdx, ESGNode snd) {
       this.fstIdx = fstIdx;
       this.fst = fst;
       this.sndIdx = sndIdx;
       this.snd = snd;
    }

}
