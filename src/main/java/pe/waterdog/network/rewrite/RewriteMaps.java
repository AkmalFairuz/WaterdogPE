/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.rewrite;

import pe.waterdog.player.ProxiedPlayer;

public class RewriteMaps {

    private final EntityTracker entityTracker;
    private final EntityMap entityMap;
    private BlockMap blockMap;

    private ItemMap itemMap;
    private ItemMap itemMapReversed;

    public RewriteMaps(ProxiedPlayer player) {
        this.entityTracker = new EntityTracker(player);
        this.entityMap = new EntityMap(player);
    }

    public EntityTracker getEntityTracker() {
        return this.entityTracker;
    }

    public EntityMap getEntityMap() {
        return this.entityMap;
    }

    public void setBlockMap(BlockMap blockMap) {
        this.blockMap = blockMap;
    }

    public BlockMap getBlockMap() {
        return this.blockMap;
    }

    public void setItemMap(ItemMap itemMap) {
        this.itemMap = itemMap;
    }

    public ItemMap getItemMap() {
        return this.itemMap;
    }

    public void setItemMapReversed(ItemMap itemMapReversed) {
        this.itemMapReversed = itemMapReversed;
    }

    public ItemMap getItemMapReversed() {
        return this.itemMapReversed;
    }
}
