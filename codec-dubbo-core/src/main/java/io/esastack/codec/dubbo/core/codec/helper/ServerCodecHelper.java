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
package io.esastack.codec.dubbo.core.codec.helper;

import esa.commons.io.IOUtils;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.esastack.codec.dubbo.core.utils.ReflectUtils;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ServerCodecHelper {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    public static DubboMessage toDubboMessage(RpcResult rpcResult) throws SerializationException {
        DubboMessage response = new DubboMessage();

        DubboHeader header = new DubboHeader()
                .setHeartbeat(false)
                .setRequestId(rpcResult.getRequestId())
                .setStatus(DubboConstants.RESPONSE_STATUS.OK)
                .setSeriType(rpcResult.getSeriType())
                //the response to dubbo must to setTwoWay(false)
                .setTwoWay(false);

        response.setHeader(header);

        Serialization serialization = SerializeFactory.getSerialization(rpcResult.getSeriType());
        if (serialization == null) {
            throw new SerializationException("unsupported serialization type:" + header.getSeriType());
        }

        ByteArrayOutputStream byteArrayOutputStream = null;
        DataOutputStream out = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            out = serialization.serialize(byteArrayOutputStream);
            if (rpcResult.getException() == null) {
                final Map<String, String> attachments = rpcResult.getAttachments();
                final boolean emptyAttachments = attachments == null || attachments.isEmpty();
                if (rpcResult.getValue() == null) {
                    if (emptyAttachments) {
                        out.writeByte(RpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE);
                    } else {
                        out.writeByte(RpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE_WITH_ATTACHMENTS);
                        out.writeMap(attachments);
                    }
                } else {
                    if (emptyAttachments) {
                        out.writeByte(RpcResult.RESPONSE_FLAG.RESPONSE_VALUE);
                        out.writeObject(rpcResult.getValue());
                    } else {
                        out.writeByte(RpcResult.RESPONSE_FLAG.RESPONSE_VALUE_WITH_ATTACHMENTS);
                        out.writeObject(rpcResult.getValue());
                        out.writeMap(attachments);
                    }
                }
            } else {
                out.writeByte(RpcResult.RESPONSE_FLAG.RESPONSE_WITH_EXCEPTION);
                out.writeThrowable(rpcResult.getException());
            }
            out.flush();
            response.setBody(byteArrayOutputStream.toByteArray());
        } catch (Throwable t) {
            rpcResult.setException(t);
            return toDubboMessage(rpcResult);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(byteArrayOutputStream);
        }
        return response;
    }

    public static RpcInvocation toRpcInvocation(DubboMessage request) throws Exception {
        return toRpcInvocation(request, null);
    }

    public static RpcInvocation toRpcInvocation(final DubboMessage request,
                                                final Map<String, String> extraAttachments) throws Exception {
        if (request == null || request.getHeader() == null) {
            return null;
        }

        if (request.getHeader().isHeartbeat()) {
            return null;
        }

        RpcInvocation invocation = new RpcInvocation();
        invocation.setSeriType(request.getHeader().getSeriType());
        invocation.setRequestId(request.getHeader().getRequestId());

        ByteArrayInputStream byteArrayInputStream = null;
        DataInputStream in = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(request.getHeader().getSeriType());
            if (serialization == null) {
                throw new SerializationException("unsupported serialization type:" +
                        request.getHeader().getSeriType());
            }

            if (request.getBody() == null) {
                throw new IllegalStateException();
            }

            byteArrayInputStream = new ByteArrayInputStream(request.getBody());
            in = serialization.deserialize(byteArrayInputStream);

            String dubboVersion = in.readUTF();

            String interfaceName = in.readUTF();

            Map<String, String> attachments = new HashMap<>(16);
            if (extraAttachments != null) {
                attachments.putAll(extraAttachments);
            }
            attachments.put(DubboConstants.PARAMETER_KEY.DUBBO_VERSION_KEY, dubboVersion);
            attachments.put(DubboConstants.PARAMETER_KEY.PATH_KEY, interfaceName);
            attachments.put(DubboConstants.PARAMETER_KEY.VERSION_KEY, in.readUTF());

            String method = in.readUTF();

            //方法的参数值
            Object[] args;

            //方法参数类型
            Class<?>[] parameterTypes;

            //方法参数类型描述
            String parameterTypeDesc = in.readUTF();

            if (parameterTypeDesc.length() == 0) {
                parameterTypes = EMPTY_CLASS_ARRAY;
                args = EMPTY_OBJECT_ARRAY;
            } else {
                if (method.equals(DubboConstants.GENERIC_INVOKE.$INVOKE) &&
                        parameterTypeDesc.equals(DubboConstants.GENERIC_INVOKE.PARAM_TYPES_DESE)) {
                    parameterTypes = DubboConstants.GENERIC_INVOKE.PARAM_TYPES;
                } else {
                    parameterTypes = ReflectUtils.desc2classArray(parameterTypeDesc);
                }
                args = new Object[parameterTypes.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = in.readObject(parameterTypes[i]);
                }
            }

            //传递的附件信息
            @SuppressWarnings("unchecked")
            Map<String, String> attachmentTmp = in.readMap();
            if (attachmentTmp != null && attachmentTmp.size() > 0) {
                attachments.putAll(attachmentTmp);
            }

            invocation.setMethodName(method);
            invocation.setParameterTypes(parameterTypes);
            invocation.setArguments(args);
            invocation.setInterfaceName(interfaceName);
            invocation.setAttachments(attachments);
        } finally {
            // request.release();  no need to release here
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(byteArrayInputStream);
        }

        return invocation;
    }
}
