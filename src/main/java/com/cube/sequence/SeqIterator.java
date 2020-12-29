package com.cube.sequence;

import java.math.BigInteger;
import java.util.Iterator;

import static com.cube.sequence.BaseCharUtils.convertToSeqId;

import org.apache.commons.math3.fraction.Fraction;


public class SeqIterator implements Iterator<String> {

    private BigInteger next;
    private final long times;
    private final BigInteger gap;

    private final int stringLength;
    private int count =0;
    private final Fraction extraPerItem;
    private Fraction totalExtra;

    public SeqIterator(BigInteger start , long gap , Fraction extraPerItem ,  int stringLength , long times){
        this.gap = BigInteger.valueOf(gap) ;
        this.stringLength = stringLength;
        this.times = times;
        this.next = start;
        this.extraPerItem = extraPerItem;
        this.totalExtra = extraPerItem;
        //System.out.println(this);
    }

    @Override
    public String toString(){
        //debugging
        return String.format("SeqIterator start:%s,gap:%s,fraction:%s,stringLength:%s,times:%s",next , gap ,  extraPerItem.toString() , stringLength , times);
    }

    @Override
    public boolean hasNext() {
        return count < times;
    }

    @Override
    public String next() {
        BigInteger oldNext = next ;
        next = next.add(gap);

        totalExtra = totalExtra.add(extraPerItem);
        int extraPad = 0;
        if(totalExtra.compareTo(Fraction.ONE) >= 0){
            extraPad = totalExtra.intValue();
            totalExtra = totalExtra.subtract(extraPad) ;
            next = next.add(BigInteger.valueOf(extraPad));
        }
        count++;
        if(count > times){
            throw new UnsupportedOperationException("iterator next max limit reached "+ String.format("next:%s gap:%s times:%s", next , gap , times));
        }
        return convertToSeqId(oldNext , stringLength);
    }


}
