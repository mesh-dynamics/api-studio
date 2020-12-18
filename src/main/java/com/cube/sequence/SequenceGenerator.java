package com.cube.sequence;

import java.util.Iterator;

import static com.cube.sequence.BaseCharUtils.*;

public class SequenceGenerator implements Iterable<String> {

    private final long size;
    private final long start;
    private final long end ;
    private final int stringLenth;

    public SequenceGenerator(long size){
        this.size = size;
        this.start = 0;
        this.end = -1;
        this.stringLenth = -1;
    }
    public SequenceGenerator(long start , long end , int stringLength , long size ){
        this.size = size;
        this.start = start;
        this.end = end;
        this.stringLenth = stringLength;
    }


    @Override
    public Iterator<String> iterator() {
        int strLen = stringLenth==-1 ? (size==1 ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN))) : stringLenth;
        long max =  end==-1 ?  (long) Math.pow(BASE_LEN , strLen) : end;

        // calculation what is the best starting point and gap to fit all members in the range
        long gap1 = (int) Math.floorDiv(max - start, size+1);
        long gap2 = (int) Math.floorDiv(max - start , size);
        long start1 = start , start2 = start;

        if((start1 + size*gap1) < max) start1 += gap1;
        if((start2 + size*gap2) < max) start2 += gap2;

        long start = start1 , gap = gap1;

        // diff is difference between boundrygap and normal members gap
        double diff1 = Math.abs(gap1 - (max - (start1 + (size-1)*gap1))) ;
        double diff2 = Math.abs(gap2 - (max - (start2 + (size-1)*gap2))) ;

        //System.out.println(String.format("diff1 %s gap1 %s" , diff1 , gap1));
        //System.out.println(String.format("diff2 %s gap2 %s" , diff2 , gap2));

        if(diff2 < diff1){
            start = start2;
            gap = gap2;
        }

        return new SeqIterator(start , gap , strLen , size);
    }

    public static void main(String[] args){

        SequenceGenerator gen1 = new SequenceGenerator(100);
        //SequenceGenerator gen2 = new SequenceGenerator(50 , 2000 , 2 , 15 );
        SequenceGenerator gen2 = new SequenceGenerator(50 , 2400 , 2 , 15 );
        for(String val : gen2){

            System.out.println(val + " : " +convertToNumber(val) );
        }

    }
}


