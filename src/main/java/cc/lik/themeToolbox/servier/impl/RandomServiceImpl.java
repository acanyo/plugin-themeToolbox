package cc.lik.themeToolbox.servier.impl;

import cc.lik.themeToolbox.entity.Link;
import cc.lik.themeToolbox.servier.RandomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
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
import java.util.ArrayList;

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
        return extensionClient.listAll(Category.class, buildOptions(all()), defaultSort())
            .collectList()
            .filter(list -> !list.isEmpty())
            .flatMap(categories -> {
                List<Category> filteredCategories = categories.stream()
                    .filter(category -> category.getStatus() != null 
                        && category.getStatus().getPostCount() != null 
                        && category.getStatus().getPostCount() > 0)
                    .toList();
                
                if (filteredCategories.isEmpty()) {
                    log.warn("没有找到包含文章的分类");
                    return Mono.empty();
                }

                // 随机选择一个分类
                int randomIndex = random.nextInt(filteredCategories.size());
                Category category = filteredCategories.get(randomIndex);
                String categoryName = category.getMetadata().getName();
                log.debug("随机选择的分类: {}", categoryName);
                // 构建文章查询条件
                var postListOptions = new ListOptions();
                var postQuery = contains("spec.categories", categoryName);
                if (filterPostName != null && !filterPostName.trim().isEmpty()) {
                    postQuery = and(postQuery, QueryFactory.notEqual("metadata.name", filterPostName));
                }
                postListOptions.setFieldSelector(FieldSelector.of(postQuery));
                return extensionClient.listAll(Post.class, postListOptions, defaultSort())
                    .collectList()
                    .flatMap(posts -> {
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
    public Flux<Link> randomLink(int count) {
        return extensionClient.listAll(Link.class, buildOptions(all()), defaultSort())
            .collectList()
            .flatMapMany(links -> {
                // 检查是否有可用的链接
                if (links.isEmpty()) {
                    log.warn("没有找到任何链接");
                    return Flux.empty();
                }
                // 计算实际返回的链接数量
                int actualCount = Math.min(count, links.size());
                log.debug("请求数量: {}, 可用链接数量: {}, 实际返回数量: {}", count, links.size(), actualCount);
                // 使用 Fisher-Yates 洗牌算法进行随机选择
                List<Link> shuffledLinks = new ArrayList<>(links);
                for (int i = shuffledLinks.size() - 1; i > 0; i--) {
                    int j = random.nextInt(i + 1);
                    // 交换元素
                    Link temp = shuffledLinks.get(i);
                    shuffledLinks.set(i, shuffledLinks.get(j));
                    shuffledLinks.set(j, temp);
                }
                return Flux.fromIterable(shuffledLinks.subList(0, actualCount));
            })
            .doOnError(error -> log.error("获取随机链接时发生错误", error));
    }

    private ListOptions buildOptions(Query que) {
        var listOptions = new ListOptions();
        return listOptions.setFieldSelector(FieldSelector.of(que));
    }

    static Sort defaultSort() {
        return Sort.by("metadata.creationTimestamp").descending();
    }

    private PageRequest defaultPageRequest() {
        // 使用一个较大的页面大小，确保能获取到所有文章
        return PageRequest.of(0, Integer.MAX_VALUE, defaultSort());
    }
}
