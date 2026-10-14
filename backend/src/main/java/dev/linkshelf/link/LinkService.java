package dev.linkshelf.link;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

	private final LinkRepository linkRepository;

	public LinkService(LinkRepository linkRepository) {
		this.linkRepository = linkRepository;
	}

	@Transactional
	public LinkResponse save(SaveLinkRequest request) {
		Link link = new Link(UrlNormalizer.normalize(request.url()), Texts.stripToNull(request.memo()),
				TagNormalizer.normalize(request.tags()), Instant.now());
		return LinkResponse.from(linkRepository.saveAndFlush(link));
	}

}
