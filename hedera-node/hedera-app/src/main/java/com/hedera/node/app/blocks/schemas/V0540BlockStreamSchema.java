/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.node.app.blocks.schemas;

import static com.hedera.node.app.blocks.impl.BlockImplUtils.appendHash;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.SemanticVersion;
import com.hedera.hapi.node.state.blockrecords.BlockInfo;
import com.hedera.hapi.node.state.blockrecords.RunningHashes;
import com.hedera.hapi.node.state.blockstream.BlockStreamInfo;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.config.api.Configuration;
import com.swirlds.state.spi.MigrationContext;
import com.swirlds.state.spi.Schema;
import com.swirlds.state.spi.StateDefinition;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;

/**
 * Defines the schema for two forms of state,
 * <ol>
 *     <li>State needed for a new or reconnected node to construct the next block exactly as will
 *     nodes already in the network.</li>
 *     <li>State derived from the block stream, and hence the natural provenance of the same service
 *     that is managing and producing blocks.</li>
 * </ol>
 * <p>
 * The two pieces of state in the first category are,
 * <ol>
 *     <li>The <b>number of the last completed block</b>, which each node must increment in the next block.</li>
 *     <li>The <b>hash of the last completed block</b>, which each node must include in the header and proof
 *     of the next block.</li>
 * </ol>
 * <p>
 * State in the second category has three parts,
 * <ol>
 *     <li>The <b>first consensus time of the last finished block</b>, for comparison with the consensus
 *     time at the start of the current block. Depending on the elapsed period between these times,
 *     the network may deterministically choose to purge expired entities, adjust node stakes and
 *     reward rates, or take other actions.</li>
 *     <li>The <b>last four values of the input block item running hash</b>, used to generate pseudorandom
 *     values for the {@link com.hedera.hapi.node.base.HederaFunctionality#UTIL_PRNG} operation.</li>
 *     <li>The <b>trailing 256 block hashes</b>, used to implement the EVM {@code BLOCKHASH} opcode.</li>
 * </ol>
 */
public class V0540BlockStreamSchema extends Schema {
    public static final String BLOCK_STREAM_INFO_KEY = "BLOCK_STREAM_INFO";
    private static final String SHARED_BLOCK_RECORD_INFO = "SHARED_BLOCK_RECORD_INFO";
    private static final String SHARED_RUNNING_HASHES = "SHARED_RUNNING_HASHES";

    /**
     * The version of the schema.
     */
    private static final SemanticVersion VERSION =
            SemanticVersion.newBuilder().major(0).minor(54).patch(0).build();

    /**
     * Schema constructor.
     */
    public V0540BlockStreamSchema() {
        super(VERSION);
    }

    @Override
    public @NonNull Set<StateDefinition> statesToCreate(@NonNull final Configuration config) {
        return Set.of(StateDefinition.singleton(BLOCK_STREAM_INFO_KEY, BlockStreamInfo.PROTOBUF));
    }

    @Override
    public void migrate(@NonNull final MigrationContext ctx) {
        final var state = ctx.newStates().getSingleton(BLOCK_STREAM_INFO_KEY);
        if (ctx.previousVersion() == null) {
            state.put(BlockStreamInfo.DEFAULT);
        } else {
            final var blockStreamInfo = state.get();
            // This will be null if the previous version is before 0.54.0
            if (blockStreamInfo == null) {
                final BlockInfo blockInfo =
                        (BlockInfo) requireNonNull(ctx.sharedValues().get(SHARED_BLOCK_RECORD_INFO));
                final RunningHashes runningHashes =
                        (RunningHashes) requireNonNull(ctx.sharedValues().get(SHARED_RUNNING_HASHES));
                state.put(BlockStreamInfo.newBuilder()
                        .blockTime(blockInfo.firstConsTimeOfLastBlock())
                        .blockNumber(blockInfo.lastBlockNumber())
                        .trailingBlockHashes(blockInfo.blockHashes())
                        .trailingOutputHashes(appendedHashes(runningHashes))
                        .build());
            }
        }
    }

    private Bytes appendedHashes(final RunningHashes runningHashes) {
        Bytes appendedHashes = Bytes.EMPTY;
        appendedHashes = appendHash(runningHashes.nMinus3RunningHash(), appendedHashes, 4);
        appendedHashes = appendHash(runningHashes.nMinus2RunningHash(), appendedHashes, 4);
        appendedHashes = appendHash(runningHashes.nMinus1RunningHash(), appendedHashes, 4);
        return appendHash(runningHashes.runningHash(), appendedHashes, 4);
    }
}
