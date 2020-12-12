package com.cube.sequence;

import io.md.dao.Event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class SeqMgr {

    // assuming the incoming events are in right order sorted order
    public static Stream<Event> createSeqId(Stream<Event> goldenEvents , long size){

        SequenceGenerator gen = new SequenceGenerator(size);

        Iterator<String> seqItr = gen.iterator();
        Map<String, String> seqIdMap = new HashMap<>();
        return goldenEvents.map(e->{
            String seqId = seqIdMap.get(e.getReqId());
            if(seqId ==null){
                seqId = seqItr.next();
                seqIdMap.put(e.getReqId() , seqId);
            }
            e.setSeqId(seqId);
            return e;
        });

    }

    // assuming the movedEventsBatch are in right sorted order
    public static Stream<Event> insertBetween(Event insertAfter , Event insertBefore , Stream<Event> movedEventsBatch , int size) {

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
        Map<String, String> seqIdMap = new HashMap<>();

        return movedEventsBatch.map(e->{
            String seqId = seqIdMap.get(e.getReqId());
            if(seqId ==null){
                seqId = padding ? finalBasePadding + seqItr.next() : seqItr.next();
                seqIdMap.put(e.getReqId() , seqId);
            }
            e.setSeqId(seqId);
            return e;
        });

    }
}
