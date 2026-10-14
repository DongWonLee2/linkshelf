package dev.linkshelf.link;

import java.util.List;

public record SaveLinkRequest(String url, String memo, List<String> tags) {

}
