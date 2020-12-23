package com.cube.sequence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.cube.sequence.BaseCharUtils.*;

public class SequenceGenerator implements Iterable<String> {

    private final long size;
    private final BigInteger start;
    private final BigInteger end ;
    private final int stringLength;
    private boolean test = false;

    public SequenceGenerator(SequenceGenerator gen){
        this.size = gen.size;
        this.start = gen.start;
        this.end = gen.end;
        this.stringLength = gen.stringLength;
        this.test = true;
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

    /*
    private Iterator<String> getSeqIterator(){

        int strLen = stringLength ==-1 ? (size==1 ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN))) : stringLength;
        BigInteger max =  end.compareTo(BigInteger.ZERO)<0 ?  BASE_LEN_BI.pow(strLen) : end;

        // calculation what is the best starting point and gap to fit all members in the range
        long gap1 = max.subtract(start).divide(BigInteger.valueOf(size+1)).longValueExact();
        long gap2 = max.subtract(start).divide(BigInteger.valueOf(size)).longValueExact();
        BigInteger start1 = start , start2 = start;

        if(start1.add(BigInteger.valueOf(size*gap1)).compareTo(max) < 0) start1 = start1.add(BigInteger.valueOf(gap1));
        if(start2.add(BigInteger.valueOf(size*gap2)).compareTo(max) < 0) start2 = start2.add(BigInteger.valueOf(gap2));

        BigInteger start = start1; long gap = gap1;

        // diff is difference between boundrygap and normal members gap
        double diff1 = Math.abs(gap1 - (max.subtract(start1).longValueExact() - (size-1)*gap1)) + Math.abs(gap1 - start1.subtract(start).longValueExact()) ;
        double diff2 = Math.abs(gap2 - (max.subtract(start2).longValueExact() - (size-1)*gap2)) + Math.abs(gap2 - start2.subtract(start).longValueExact()) ;

        //System.out.println(String.format("diff1 %s gap1 %s" , diff1 , gap1));
        //System.out.println(String.format("diff2 %s gap2 %s" , diff2 , gap2));
        if(diff2 < diff1){
            start = start2;
            gap = gap2;
            System.out.println("This is GAP2");
        }

        return new SeqIterator(start , gap , strLen , size);
    }*/

    /*
    public SequenceGenerator getTestGen(){
        return new SequenceGenerator(this);
    }
    */

    private Iterator<String> getSeqIterator2() {

        int strLen = stringLength == -1 ? (size == 1 ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN))) : stringLength;
        BigInteger max = end.equals(BigInteger.valueOf(-1)) ? BASE_LEN_BI.pow(strLen) : end;

        // calculation what is the best starting point and gap to fit all members in the range
        BigInteger[] gapRes = max.subtract(start).add(BigInteger.ONE).divideAndRemainder(BigInteger.valueOf(size + 1));
        long gap = gapRes[0].longValueExact() + (long)Math.floor(gapRes[1].doubleValue()/size);
        BigInteger startPos = start;

        if (startPos.add(BigInteger.valueOf(size * gap)).compareTo(max) < 0)
            startPos = startPos.add(BigInteger.valueOf(gap));


        return new SeqIterator(startPos, gap, Math.random() /* todo */ , strLen,  size);
    }

    /*
    private Iterator<String> getSeqIterator3() {

        int strLen = stringLength == -1 ? (size == 1 ? 1 : (int) Math.ceil(Math.log(size) / Math.log(BASE_LEN))) : stringLength;
        BigInteger max = end.equals(BigInteger.valueOf(-1)) ? BASE_LEN_BI.pow(strLen) : end;

        // calculation what is the best starting point and gap to fit all members in the range
        BigInteger[] gapRes = max.subtract(start).add(BigInteger.ONE).divideAndRemainder(BigInteger.valueOf(size + 1));
        long gap = gapRes[0].longValueExact() + (long)Math.floor(gapRes[1].doubleValue()/size);
        BigInteger startPos = start.subtract(BigInteger.ONE).add(BigInteger.valueOf(gap));

        return new SeqIterator(startPos, gap, strLen, size);
    }*/


    @Override
    public Iterator<String> iterator() {
        //return test ? getSeqIterator2() : getSeqIterator();
        return getSeqIterator2();
    }

    /*
    private static void test(SequenceGenerator gen1){
        SequenceGenerator gen2 = gen1.getTestGen();
        List<String> list1 = getList(gen1.iterator());
        List<String> list2 = getList(gen2.iterator());

        for(int i=0 ; i<gen1.size ; i++){
            if(!list1.get(i).equals(list2.get(i))){
                System.out.println("Not equals");
                System.out.println(list1.stream().map(BaseCharUtils::convertToNumber).map(Object::toString).collect(Collectors.joining(",")));
                System.out.println(list2.stream().map(BaseCharUtils::convertToNumber).map(Object::toString).collect(Collectors.joining(",")));
                return;
            }
        }

        System.out.println("Equals");
    }

    private static List<String> getList(Iterator<String> itr){
        List<String> list = new ArrayList<>();
        itr.forEachRemaining(list::add);
        return list;
    }

    public static void main(String[] args){

        SequenceGenerator gen1 = new SequenceGenerator(5);
        //SequenceGenerator gen2 = new SequenceGenerator(50 , 2000 , 2 , 15 );

        SequenceGenerator gen2 = new SequenceGenerator(BigInteger.valueOf(50) , BigInteger.valueOf(58) , 1 , 4 );

        for(String val : gen1){

            System.out.println(val + " : " +convertToNumber(val) );
        }


        test(gen1);

    }*/
}


