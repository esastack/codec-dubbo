/*
 * Copyright 2021 OPPO ESA Stack Project
 *
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
package io.esastack.codec.serialization.protobuf;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static io.esastack.codec.serialization.api.SerializeConstants.PROTOBUF_JSON_SERIALIZATION_ID;

public class ProtobufJsonSerialization implements Serialization {

    static {
        ProtobufUtil.init();
    }

    @Override
    public byte getSeriTypeId() {
        return PROTOBUF_JSON_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public String getSeriName() {
        return "protobuf-json";
    }

    @Override
    public DataOutputStream serialize(OutputStream output) throws IOException {
        return new ProtobufJsonObjectOutput(output);
    }

    @Override
    public DataInputStream deserialize(InputStream input) throws IOException {
        return new ProtobufJsonObjectInput(input);
    }
}
