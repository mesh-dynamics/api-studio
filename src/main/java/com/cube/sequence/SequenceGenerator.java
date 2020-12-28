package com.cube.sequence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cube.sequence.BaseCharUtils.*;

import org.apache.commons.math3.fraction.Fraction;

public class SequenceGenerator implements Iterable<String> {

    private final long size;
    private final BigInteger start;
    private final BigInteger end ;
    private final int stringLength;

    public SequenceGenerator(SequenceGenerator gen){
        this.size = gen.size;
        this.start = gen.start;
        this.end = gen.end;
        this.stringLength = gen.stringLength;
    }
    public SequenceGenerator(long size){
        this.size = size;
        this.start = BigInteger.ZERO;
        this.end = BigInteger.valueOf(-1);
        this.stringLength = -1;
    }
    public SequenceGenerator(BigInteger start , BigInteger end , int stringLength , long size ){
        this.size = size;
        this.start = start;
        this.end = end;
        this.stringLength = stringLength;
    }

    private Iterator<String> getSeqIterator() {

        int strLen = stringLength == -1 ? (size == 1 ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN))) : stringLength;
        BigInteger max = end.equals(BigInteger.valueOf(-1)) ? BASE_LEN_BI.pow(strLen) : end;

        BigInteger[] gapRes = max.subtract(start).subtract(BigInteger.valueOf(size)).divideAndRemainder(BigInteger.valueOf(size + 1));
        long gap = gapRes[0].longValueExact() ;


        Fraction extraPerItem = new Fraction(gapRes[1].intValueExact() , (int)size+1);

        return new SeqIterator(start, gap, extraPerItem , strLen,  size);
    }

    @Override
    public Iterator<String> iterator() {
        return getSeqIterator();
    }

    /*
    public static void main(String[] args){

        SequenceGenerator gen1 = new SequenceGenerator(2);
        //SequenceGenerator gen2 = new SequenceGenerator(50 , 2000 , 2 , 15 );

        SequenceGenerator gen2 = new SequenceGenerator(BigInteger.valueOf(50) , BigInteger.valueOf(58) , 1 , 4 );

        BigInteger last = BigInteger.ZERO.subtract(BigInteger.ONE);
        for(String val : gen1){
            BigInteger next = convertToNumber(val);
            BigInteger diff = next.subtract(last);

            last = next;
            System.out.println(val + " : " + next + " diff:"+diff);
        }
    }*/
}


