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

package com.hedera.node.app.workflows.handle.flow.dispatch.user;

import com.hedera.node.app.workflows.handle.flow.dispatch.Dispatch;
import com.hedera.node.app.workflows.handle.flow.dispatch.user.modules.UserDispatchModule;
import dagger.Subcomponent;

/**
 * The Dagger subcomponent to provide the bindings for the user transaction dispatch scope.
 */
@Subcomponent(modules = {UserDispatchModule.class})
@UserDispatchScope
public interface UserDispatchComponent extends Dispatch {
    @Subcomponent.Factory
    interface Factory {
        UserDispatchComponent create();
    }
}
