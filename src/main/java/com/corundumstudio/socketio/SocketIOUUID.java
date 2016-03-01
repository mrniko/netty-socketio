/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import com.relops.snowflake.Snowflake;

/**
 * Created by jiayin on 16/1/4.
 */
public class SocketIOUUID {

    public static int node = 1;

    public static Snowflake s;

    public static void init () {
        s = new Snowflake(node);
    }

    public static Long getId(){
        return s.next();
    }
}
