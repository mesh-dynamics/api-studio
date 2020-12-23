package com.cube.sequence;

import java.math.BigInteger;
import java.util.Iterator;

import static com.cube.sequence.BaseCharUtils.convertToSeqId;

public class SeqIterator implements Iterator<String> {

    private BigInteger next;
    private final long times;
    private final long gap;

    private final int stringLength;
    private int count =0;

    public SeqIterator(BigInteger start , long gap , int stringLength , long times){
        this.gap = gap;
        this.stringLength = stringLength;
        this.times = times;
        this.next = start;
    }
    @Override
    public boolean hasNext() {
        return count < times;
    }

    @Override
    public String next() {
        BigInteger oldNext = next ;
        next = next.add(BigInteger.valueOf(gap));
        count++;
        if(count > times){
            throw new UnsupportedOperationException("iterator next max limit reached "+ String.format("next:%s gap:%s times:%s", next , gap , times));
        }
        return convertToSeqId(oldNext , stringLength);
    }


}
