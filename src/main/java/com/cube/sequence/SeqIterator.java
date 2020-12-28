package com.cube.sequence;

import java.math.BigInteger;
import java.util.Iterator;

import static com.cube.sequence.BaseCharUtils.convertToSeqId;

import org.apache.commons.math3.fraction.Fraction;


public class SeqIterator implements Iterator<String> {

    private BigInteger next;
    private final long times;
    private final long gap;

    private final int stringLength;
    private int count =0;
    private final Fraction extraPerItem;
    private Fraction totalExtra = new Fraction(0,1);

    public SeqIterator(BigInteger start , long gap , Fraction extraPerItem ,  int stringLength , long times){
        this.gap = gap;
        this.stringLength = stringLength;
        this.times = times;
        this.next = start;
        this.extraPerItem = extraPerItem;
        next();
        //System.out.println(this);
    }

    @Override
    public String toString(){
        return String.format("start:%s,gap:%s,fraction:%s",this.next , this.gap , this.extraPerItem.toString());
    }
    @Override
    public boolean hasNext() {
        return count <= times;
    }

    @Override
    public String next() {
        BigInteger oldNext = next ;
        totalExtra = totalExtra.add(extraPerItem);
        int extraPad = 0;
        if(totalExtra.compareTo(Fraction.ONE) >= 0){
            extraPad = (int)totalExtra.longValue();
            totalExtra = totalExtra.subtract(extraPad) ;
        }
        long mgap = count==0 ? gap : gap+1;
        next = next.add(BigInteger.valueOf(mgap+extraPad));
        count++;
        if(count > times+1){
            throw new UnsupportedOperationException("iterator next max limit reached "+ String.format("next:%s gap:%s times:%s", next , gap , times));
        }
        return convertToSeqId(oldNext , stringLength);
    }


}
