package com.cube.sequence;

import io.md.dao.Event;

import java.util.Iterator;
import java.util.stream.Stream;

public class SeqMgr {

    // assuming the incoming events are in right order sorted order
    public static void createSeqId(Stream<Event> goldenEvents , long size){

        SequenceGenerator gen = new SequenceGenerator(size);

        Iterator<String> seqItr = gen.iterator();
        goldenEvents.forEach(e->{
            String seqId = seqItr.next();
            e.setSeqId(seqId);
        });

    }

    // assuming the movedEventsBatch are in right sorted order
    public static void insertBetween(Event insertAfter , Event insertBefore , Stream<Event> movedEventsBatch , int size) {

        long prevSeq =  BaseCharUtils.convertToNumber(insertAfter.getSeqId());
        long nextSeq =  BaseCharUtils.convertToNumber(insertBefore.getSeqId());

        SequenceGenerator gen = null;
        String basePadding = null;
        if((nextSeq - prevSeq) > size){
            // can be fitted in between
            gen = new SequenceGenerator(prevSeq+1 , nextSeq , insertAfter.getSeqId().length() , size);
            basePadding = "";
        }else{
            // it has to be padded to  insertAfter seqId
            gen = new SequenceGenerator(size);
            basePadding = insertAfter.getSeqId();
        }

        Iterator<String> seqItr = gen.iterator();
        final String finalBasePadding = basePadding;
        final boolean padding = !finalBasePadding.isEmpty();
        movedEventsBatch.forEach(e->{
            String seqId = padding ? finalBasePadding + seqItr.next() : seqItr.next();
            e.setSeqId(seqId);
        });

    }
}
