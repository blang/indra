package org.lambda3.indra.core.impl;

/*-
 * ==========================License-Start=============================
 * Indra Core Module
 * --------------------------------------------------------------------
 * Copyright (C) 2016 - 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import org.apache.commons.math3.linear.RealVector;
import org.lambda3.indra.core.Params;
import org.lambda3.indra.core.RelatednessBaseClient;
import org.lambda3.indra.core.VectorSpace;

public class Jaccard2Client extends RelatednessBaseClient {

    public Jaccard2Client(Params params, VectorSpace vectorSpace) {
        super(params, vectorSpace);
    }

    @Override
    protected double sim(RealVector r1, RealVector r2, boolean sparse) {
        if (r1.getDimension() != r2.getDimension()) {
            return 0;
        }

        double min = 0.0;
        double max = 0.0;

        for (int i = 0; i <r1.getDimension(); ++i) {
            if (r1.getEntry(i) > r2.getEntry(i)) {
                min +=r2.getEntry(i);
                max += r1.getEntry(i);
            } else {
                min += r1.getEntry(i);
                max += r2.getEntry(i);
            }
        }

        if (max == 0) {
            return 0;
        }

        return Math.abs(min / max);
    }
}
