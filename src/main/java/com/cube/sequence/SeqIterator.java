package com.cube.sequence;

import java.math.BigInteger;
import java.util.Iterator;

import static com.cube.sequence.BaseCharUtils.convertToSeqId;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.fraction.Fraction;


public class SeqIterator implements Iterator<String> {

    private BigInteger next;
    private final long times;
    private final BigFraction gap;
    private BigFraction totalGap;

    private final int stringLength;
    private int count =0;

    public SeqIterator(BigInteger start , BigFraction gap , int stringLength , long times){
        this.gap = gap;
        this.stringLength = stringLength;
        this.times = times;
        this.next = start;
        this.totalGap = new BigFraction(0);
        //System.out.println(this);
    }

    @Override
    public String toString(){
        //debugging
        return String.format("SeqIterator start:%s,gap:%s,stringLength:%s,times:%s",next , gap , stringLength , times);
    }

    @Override
    public boolean hasNext() {
        return count < times;
    }

    @Override
    public String next() {
        totalGap = totalGap.add(gap);

        long gapToAdd = totalGap.longValue();
        next = next.add(BigInteger.valueOf(gapToAdd));

        totalGap = totalGap.subtract(gapToAdd);
        count++;
        if(count > times){
            throw new UnsupportedOperationException("iterator next max limit reached "+ String.format("next:%s gap:%s times:%s", next , gap , times));
        }
        return convertToSeqId(next , stringLength);
    }


}
