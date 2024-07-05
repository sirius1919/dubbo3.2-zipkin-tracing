package com.mcsirius.cloud.dubbo.filter;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.alibaba.fastjson2.JSON;
import com.mcsirius.cloud.dubbo.SingletonTracingFactory;
import com.mcsirius.cloud.dubbo.dto.DubboRequestDTO;
import com.mcsirius.cloud.dubbo.dto.DubboResponseDTO;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;


@Activate(group = {Constants.PROVIDER_PROTOCOL, Constants.CONSUMER_PROTOCOL}, order = 100, value = "dubboTraceFilter")
public class DubboTraceFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(DubboTraceFilter.class);

    private static Tracing tracing;
    private static Tracer tracer;
    private static TraceContext.Extractor<Map<String, String>> extractor;
    private static TraceContext.Injector<Map<String, String>> injector;

    static final Propagation.Getter<Map<String, String>, String> GETTER =
            new Propagation.Getter<Map<String, String>, String>() {
                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }

                @Override
                public String toString() {
                    return "Map::get";
                }
            };
    static final Propagation.Setter<Map<String, String>, String> SETTER =
            new Propagation.Setter<Map<String, String>, String>() {
                @Override
                public void put(Map<String, String> carrier, String key, String value) {
                    carrier.put(key, value);
                }

                @Override
                public String toString() {
                    return "Map::set";
                }
            };

    static {
        tracing = SingletonTracingFactory.getTracing();
        tracer = tracing.tracer();
        extractor = tracing.propagation().extractor(GETTER);
        injector = tracing.propagation().injector(SETTER);
    }

    public DubboTraceFilter() {
        super();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 处理 Trace 信息
        printRequest(invocation);
        // 执行前
        long start = System.currentTimeMillis();
        Result result = null;

        RpcContext rpcContext = RpcContext.getContext();
        Span.Kind kind;
        try {
            kind = rpcContext.isProviderSide() ? Span.Kind.SERVER : Span.Kind.CLIENT;
        } catch (Exception e) {
            kind = Span.Kind.CLIENT;
        }
        final Span span;
        if (kind.equals(Span.Kind.CLIENT)) {
            span = tracer.nextSpan();
            injector.inject(span.context(), invocation.getAttachments());
        } else {
            TraceContextOrSamplingFlags extracted = extractor.extract(invocation.getAttachments());
            span = extracted.context() != null ? tracer.joinSpan(extracted.context()) : tracer.nextSpan(extracted);
        }

        if (!span.isNoop()) {
            span.kind(kind).start();
            String service = invoker.getInterface().getSimpleName();
            String method = RpcUtils.getMethodName(invocation);
            span.kind(kind);
            span.name(service + "/" + method);
            InetSocketAddress remoteAddress = rpcContext.getRemoteAddress();
            span.remoteIpAndPort(
                    remoteAddress.getAddress() != null ? remoteAddress.getAddress().getHostAddress() : remoteAddress.getHostName(),remoteAddress.getPort());
        }

        boolean isOneway = false, deferFinish = false;
        try (Tracer.SpanInScope scope = tracer.withSpanInScope(span)){
            collectArguments(invocation, span, kind);
            result = invoker.invoke(invocation);

            if (result.hasException()) {
                onError(result.getException(), span);
            }
            isOneway = RpcUtils.isOneway(invoker.getUrl(), invocation);

            Future<Object> future = rpcContext.getFuture();
            if (future instanceof FutureAdapter) {
                deferFinish = true;
                ((FutureAdapter) future).getFuture().setCallback(new FinishSpanCallback(span));
            }
        } catch (Error | RuntimeException e) {
            onError(e, span);
            throw e;
        } finally {
            if (isOneway) {
                span.flush();
            } else if (!deferFinish) {
                span.finish();
            }
        }

        long end = System.currentTimeMillis();
        // 执行后
        final long executionTime = end - start;
        printResponse(invocation, result, executionTime);
        return result;
    }


    private void printRequest(Invocation invocation) {
        DubboRequestDTO requestDTO = new DubboRequestDTO();
        requestDTO.setInterfaceClass(invocation.getInvoker().getInterface().getName());
        requestDTO.setMethodName(invocation.getMethodName());
        requestDTO.setArgs(getArgs(invocation));
        logger.info("RPC请求开始 , {}", requestDTO);
    }


    private void printResponse(Invocation invocation, Result result, long spendTime) {
        DubboResponseDTO responseDTO = new DubboResponseDTO();
        responseDTO.setInterfaceClassName(invocation.getInvoker().getInterface().getName());
        responseDTO.setMethodName(invocation.getMethodName());
        responseDTO.setResult(JSON.toJSONString(result.getValue()));
        responseDTO.setSpendTime(spendTime);
        logger.info("RPC请求结束 , {}", responseDTO);
    }

    private Object[] getArgs(Invocation invocation) {
        Object[] args = invocation.getArguments();
        args = Arrays.stream(args).filter(arg -> {
            // 过滤大参
            if (arg instanceof byte[] || arg instanceof Byte[] || arg instanceof InputStream || arg instanceof File) {
                return false;
            }
            return true;
        }).toArray();
        return args;
    }

    static void collectArguments(Invocation invocation, Span span, Span.Kind kind) {
        if (kind == Span.Kind.CLIENT) {
            StringBuilder fqcn = new StringBuilder();
            Object[] args = invocation.getArguments();
            if (args != null && args.length > 0) {
                try {
                    fqcn.append(JsonUtils.toJson(args));
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            span.tag("args", fqcn.toString());
        }
    }

    static void onError(Throwable error, Span span) {
        span.error(error);
        if (error instanceof RpcException) {
            span.tag("dubbo.error_msg", RpcExceptionEnum.getMsgByCode(((RpcException) error).getCode()));
        }
    }

    private enum RpcExceptionEnum {
        UNKNOWN_EXCEPTION(0, "unknown exception"),
        NETWORK_EXCEPTION(1, "network exception"),
        TIMEOUT_EXCEPTION(2, "timeout exception"),
        BIZ_EXCEPTION(3, "biz exception"),
        FORBIDDEN_EXCEPTION(4, "forbidden exception"),
        SERIALIZATION_EXCEPTION(5, "serialization exception"),;

        private int code;

        private String msg;

        RpcExceptionEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static String getMsgByCode(int code) {
            for (RpcExceptionEnum error : RpcExceptionEnum.values()) {
                if (code == error.code) {
                    return error.msg;
                }
            }
            return null;
        }
    }

    static final class FinishSpanCallback implements ResponseCallback {
        final Span span;

        FinishSpanCallback(Span span) {
            this.span = span;
        }

        @Override
        public void done(Object response) {
            span.finish();
        }

        @Override
        public void caught(Throwable exception) {
            onError(exception, span);
            span.finish();
        }
    }
}
