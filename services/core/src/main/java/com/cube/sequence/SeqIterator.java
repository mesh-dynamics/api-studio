/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        BigInteger gapToAdd = totalGap.getNumerator().divide(totalGap.getDenominator());
        next = next.add(gapToAdd);

        totalGap = totalGap.subtract(gapToAdd);
        count++;
        if(count > times){
            throw new UnsupportedOperationException("iterator next max limit reached "+ String.format("next:%s gap:%s times:%s", next , gap , times));
        }
        return convertToSeqId(next , stringLength);
    }


}
