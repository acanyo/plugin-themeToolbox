package cc.lik.themeToolbox.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import cc.lik.themeToolbox.entity.Link;
import cc.lik.themeToolbox.servier.RandomService;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomEndpoint implements CustomEndpoint {
    private final RandomService randomService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "api.themeToolbox.lik.cc/v1alpha1/random";
        return SpringdocRouteBuilder.route()
            .GET("/toolbox/post", this::randomPost, builder -> builder.operationId("Get random post")
                .tag(tag)
                .description("获取随机文章")
                .parameter(
                    parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("filterPostName")
                        .description("过滤文章名称")
                        .required(false)
                )
                .response(
                    responseBuilder()
                        .implementation(Post.class)
                )
            )
            .GET("/toolbox/link", this::randomLink, builder -> builder.operationId("Get random links")
                .tag(tag)
                .description("获取随机链接")
                .parameter(
                    parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("count")
                        .description("需要返回的随机链接数量，默认为1")
                        .required(false)
                )
                .response(
                    responseBuilder()
                        .implementationArray(Link.class)
                )
            )
            .build();
    }

    Mono<ServerResponse> randomPost(ServerRequest request) {
        String filterPostName = request.queryParam("filterPostName").orElse("");
        return randomService.randomPost(filterPostName)
            .flatMap(post -> ServerResponse.ok().bodyValue(post))
            .doOnError(error -> log.error("获取随机文章失败", error))
            .onErrorResume(error -> ServerResponse.badRequest()
                .bodyValue("获取随机文章失败: " + error.getMessage()));
    }

    Mono<ServerResponse> randomLink(ServerRequest request) {
        int count = request.queryParam("count")
            .filter(s -> !s.trim().isEmpty())
            .map(Integer::parseInt)
            .orElse(1);
        
        return randomService.randomLink(count)
            .collectList()
            .flatMap(links -> ServerResponse.ok().bodyValue(links))
            .doOnError(error -> log.error("获取随机链接失败", error))
            .onErrorResume(error -> ServerResponse.badRequest()
                .bodyValue("获取随机链接失败: " + error.getMessage()));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.themeToolbox.lik.cc/v1alpha1");
    }
}
