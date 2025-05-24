package cc.lik.themeToolbox.servier.impl;

import cc.lik.themeToolbox.entity.Link;
import cc.lik.themeToolbox.servier.RandomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.index.query.Query;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.extension.router.selector.FieldSelector;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.contains;

@Component
@RequiredArgsConstructor
@Slf4j
public class RandomServiceImpl implements RandomService {
    private final ReactiveExtensionClient extensionClient;
    private final Random random = new Random();

    @Override
    public Mono<Post> randomPost(String filterPostName) {
        var listOptions = buildOptions(QueryFactory.greaterThan("status.postCount", "0"));
        return extensionClient.listAll(Category.class, listOptions, defaultSort())
            .collectList()
            .flatMap(categories -> {
                // 随机选择一个分类
                int randomIndex = random.nextInt(categories.size());
                Category category = categories.get(randomIndex);
                String categoryName = category.getMetadata().getName();
                log.debug("随机选择的分类: {}", categoryName);
                // 构建文章查询条件
                var postListOptions = new ListOptions();
                var postQuery = contains("spec.categories", categoryName);
                if (filterPostName != null && !filterPostName.trim().isEmpty()) {
                    postQuery = and(postQuery, QueryFactory.notEqual("metadata.name", filterPostName));
                }
                postListOptions.setFieldSelector(FieldSelector.of(postQuery));
                return extensionClient.listBy(Post.class, postListOptions, null)
                    .flatMap(result -> {
                        List<Post> posts = result.getItems();
                        if (posts.isEmpty()) {
                            log.warn("分类 {} 下没有符合条件的文章", categoryName);
                            return Mono.empty();
                        }
                        // 随机选择一篇文章
                        int randomPostIndex = random.nextInt(posts.size());
                        Post randomPost = posts.get(randomPostIndex);
                        log.debug("在分类 {} 中找到随机文章: {}", categoryName, randomPost.getMetadata().getName());
                        return Mono.just(randomPost);
                    });
            });
    }

    @Override
    public Mono<Link> randomLink() {
        return extensionClient.listAll(Link.class,buildOptions(all()),defaultSort())
            .collectList()
            .defaultIfEmpty(Collections.emptyList())
            .flatMap(links -> {
                Random randomLink = new Random();
                return Mono.just(links.get(randomLink.nextInt(links.size())));
            });
    }

    private ListOptions buildOptions(Query que) {
        var listOptions = new ListOptions();
        return listOptions.setFieldSelector(FieldSelector.of(que));
    }
    static Sort defaultSort() {
        return Sort.by("metadata.creationTimestamp").descending();
    }
}
