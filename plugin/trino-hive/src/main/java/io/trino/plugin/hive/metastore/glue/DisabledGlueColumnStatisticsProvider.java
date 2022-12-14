/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.hive.metastore.glue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.trino.plugin.hive.HiveColumnStatisticType;
import io.trino.plugin.hive.metastore.HiveColumnStatistics;
import io.trino.plugin.hive.metastore.Partition;
import io.trino.plugin.hive.metastore.Table;
import io.trino.spi.TrinoException;
import io.trino.spi.type.Type;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static java.util.function.UnaryOperator.identity;

public class DisabledGlueColumnStatisticsProvider
        implements GlueColumnStatisticsProvider
{
    @Override
    public Set<HiveColumnStatisticType> getSupportedColumnStatistics(Type type)
    {
        return ImmutableSet.of();
    }

    @Override
    public Map<String, HiveColumnStatistics> getTableColumnStatistics(Table table)
    {
        return ImmutableMap.of();
    }

    @Override
    public Map<Partition, Map<String, HiveColumnStatistics>> getPartitionColumnStatistics(Collection<Partition> partitions)
    {
        return partitions.stream().collect(toImmutableMap(identity(), partition -> ImmutableMap.of()));
    }

    @Override
    public void updateTableColumnStatistics(Table table, Map<String, HiveColumnStatistics> columnStatistics)
    {
        if (!columnStatistics.isEmpty()) {
            throw new TrinoException(NOT_SUPPORTED, "Glue metastore column level statistics are disabled");
        }
    }

    @Override
    public void updatePartitionStatistics(Set<PartitionStatisticsUpdate> partitionStatisticsUpdates)
    {
        if (partitionStatisticsUpdates.stream().anyMatch(update -> !update.getColumnStatistics().isEmpty())) {
            throw new TrinoException(NOT_SUPPORTED, "Glue metastore column level statistics are disabled");
        }
    }
}
