package cc.lik.themeToolbox.servier;

import cc.lik.themeToolbox.entity.Link;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;

public interface RandomService {
    Mono<Post> randomPost(String filterPostName);
    Flux<Link> randomLink(int count);
}
