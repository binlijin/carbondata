/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.presto.readers;

import java.io.IOException;

import org.apache.carbondata.core.cache.dictionary.Dictionary;
import org.apache.carbondata.core.metadata.datatype.DataTypes;
import org.apache.carbondata.core.util.DataTypeUtil;

import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.block.BlockBuilderStatus;
import com.facebook.presto.spi.type.Type;

public class IntegerStreamReader extends AbstractStreamReader {

  private Dictionary dictionaryValues;
  private boolean isDictionary;

  public IntegerStreamReader() {

  }

  public IntegerStreamReader(boolean isDictionary, Dictionary dictionary) {
    this.dictionaryValues = dictionary;
    this.isDictionary = isDictionary;
  }

  public Block readBlock(Type type) throws IOException {
    int numberOfRows;
    BlockBuilder builder;
    if (isVectorReader) {
      numberOfRows = batchSize;
      builder = type.createBlockBuilder(new BlockBuilderStatus(), numberOfRows);
      if (columnVector != null) {
        if (columnVector.anyNullsSet()) {
          handleNullInVector(type, numberOfRows, builder);
        } else {
          populateVector(type, numberOfRows, builder);
        }
      }
    } else {
      numberOfRows = streamData.length;
      builder = type.createBlockBuilder(new BlockBuilderStatus(), numberOfRows);
      if (streamData != null) {
        for (int i = 0; i < numberOfRows; i++) {
          type.writeLong(builder, ((Integer) streamData[i]).longValue());
        }
      }
    }
    return builder.build();
  }

  private void handleNullInVector(Type type, int numberOfRows, BlockBuilder builder) {
    for (int i = 0; i < numberOfRows; i++) {
      if (columnVector.isNullAt(i)) {
        builder.appendNull();
      } else {
        type.writeLong(builder, ((Integer) columnVector.getData(i)).longValue());
      }
    }
  }

  private void populateVector(Type type, int numberOfRows, BlockBuilder builder) {
    if (isDictionary) {
      for (int i = 0; i < numberOfRows; i++) {
        int value = (int) columnVector.getData(i);
        Object data = DataTypeUtil
            .getDataBasedOnDataType(dictionaryValues.getDictionaryValueForKey(value),
                DataTypes.INT);
        if (data != null) {
          type.writeLong(builder, ((Integer) data).longValue());
        } else {
          builder.appendNull();
        }
      }
    } else {
      for (int i = 0; i < numberOfRows; i++) {
        Integer value = (Integer) columnVector.getData(i);
        type.writeLong(builder, value.longValue());
      }
    }
  }
}
