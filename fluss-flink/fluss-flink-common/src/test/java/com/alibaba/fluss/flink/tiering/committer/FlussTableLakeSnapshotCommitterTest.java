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

package com.alibaba.fluss.flink.tiering.committer;

import com.alibaba.fluss.client.metadata.LakeSnapshot;
import com.alibaba.fluss.flink.utils.FlinkTestBase;
import com.alibaba.fluss.lake.committer.CommittedLakeSnapshot;
import com.alibaba.fluss.metadata.PartitionInfo;
import com.alibaba.fluss.metadata.TableBucket;
import com.alibaba.fluss.metadata.TablePath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fluss.record.TestData.DATA1_PARTITIONED_TABLE_DESCRIPTOR;
import static com.alibaba.fluss.record.TestData.DATA1_TABLE_DESCRIPTOR;
import static org.assertj.core.api.Assertions.assertThat;

/** Test for {@link FlussTableLakeSnapshotCommitter}. */
class FlussTableLakeSnapshotCommitterTest extends FlinkTestBase {

    private FlussTableLakeSnapshotCommitter flussTableLakeSnapshotCommitter;

    @BeforeEach
    void beforeEach() {
        flussTableLakeSnapshotCommitter =
                new FlussTableLakeSnapshotCommitter(FLUSS_CLUSTER_EXTENSION.getClientConfig());
        flussTableLakeSnapshotCommitter.open();
    }

    @AfterEach
    void afterEach() throws Exception {
        if (flussTableLakeSnapshotCommitter != null) {
            flussTableLakeSnapshotCommitter.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCommit(boolean isPartitioned) throws Exception {
        TablePath tablePath =
                TablePath.of("fluss", "test_commit" + (isPartitioned ? "_partitioned" : ""));
        long tableId =
                createTable(
                        tablePath,
                        isPartitioned
                                ? DATA1_PARTITIONED_TABLE_DESCRIPTOR
                                : DATA1_TABLE_DESCRIPTOR);

        List<String> partitions;
        Map<String, Long> partitionNameAndIds = Collections.emptyMap();
        if (!isPartitioned) {
            FLUSS_CLUSTER_EXTENSION.waitUntilTableReady(tableId);
            partitions = Collections.singletonList(null);
        } else {
            partitionNameAndIds = FLUSS_CLUSTER_EXTENSION.waitUntilPartitionAllReady(tablePath);
            partitions = new ArrayList<>(partitionNameAndIds.keySet());
        }

        CommittedLakeSnapshot committedLakeSnapshot = new CommittedLakeSnapshot(3);

        Map<TableBucket, Long> expectedOffsets = new HashMap<>();
        for (int bucket = 0; bucket < 3; bucket++) {
            long bucketOffset = bucket * bucket;
            for (String partition : partitions) {
                if (partition == null) {
                    committedLakeSnapshot.addBucket(bucket, bucketOffset);
                    expectedOffsets.put(new TableBucket(tableId, bucket), bucketOffset);
                } else {
                    committedLakeSnapshot.addPartitionBucket(partition, bucket, bucketOffset);
                    expectedOffsets.put(
                            new TableBucket(tableId, partitionNameAndIds.get(partition), bucket),
                            bucketOffset);
                }
            }
        }

        Map<String, Long> partitionIdByName = null;
        if (isPartitioned) {
            partitionIdByName =
                    admin.listPartitionInfos(tablePath).get().stream()
                            .collect(
                                    Collectors.toMap(
                                            PartitionInfo::getPartitionName,
                                            PartitionInfo::getPartitionId));
        }

        // commit offsets
        flussTableLakeSnapshotCommitter.commit(tableId, partitionIdByName, committedLakeSnapshot);
        LakeSnapshot lakeSnapshot = admin.getLatestLakeSnapshot(tablePath).get();
        assertThat(lakeSnapshot.getSnapshotId()).isEqualTo(3);

        // get and check the offsets
        Map<TableBucket, Long> bucketLogOffsets = lakeSnapshot.getTableBucketsOffset();
        assertThat(bucketLogOffsets).isEqualTo(expectedOffsets);
    }
}
