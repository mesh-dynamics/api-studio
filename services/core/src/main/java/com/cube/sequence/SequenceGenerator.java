package com.cube.sequence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cube.sequence.BaseCharUtils.*;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.fraction.Fraction;

public class SequenceGenerator implements Iterable<String> {

    private final long size;
    //start is non-inclusive
    private final BigInteger start;
    //end is non-inclusive
    private final BigInteger end ;
    private final int stringLength;

    public SequenceGenerator(long size){
        this.size = size;
        this.start = BigInteger.ZERO.subtract(BigInteger.ONE);
        this.stringLength = (size == 1) ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN));
        this.end = BASE_LEN_BI.pow(stringLength);

    }
    public SequenceGenerator(BigInteger start , BigInteger end , int stringLength , long size ){
        this.size = size;
        this.start = start;
        this.end = end;
        this.stringLength = stringLength;
    }

    private Iterator<String> getSeqIterator() {

        BigFraction gap = new BigFraction(end.subtract(start) ,  BigInteger.valueOf(size+1));

        return new SeqIterator(start, gap , stringLength,  size);
    }

    @Override
    public Iterator<String> iterator() {
        return getSeqIterator();
    }



    /*
    public static void main(String[] args){

        SequenceGenerator gen1 = new SequenceGenerator(61);
        //SequenceGenerator gen2 = new SequenceGenerator(50 , 2000 , 2 , 15 );

        SequenceGenerator gen2 = new SequenceGenerator(BigInteger.valueOf(50) , BigInteger.valueOf(58) , 1 , 8 );

        BigInteger last = BigInteger.ZERO.subtract(BigInteger.ONE);
        for(String val : gen1){
            BigInteger next = convertToNumber(val);
            BigInteger diff = next.subtract(last);

            last = next;
            System.out.println(val + " : " + next + " diff:"+diff);
        }
    }

     */
}


